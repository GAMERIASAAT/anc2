package anc.ui.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import anc.core.AncModule
import anc.core.EventBus
import anc.core.Framework
import anc.core.ModuleManager
import anc.core.ModuleMetadata
import anc.core.ModuleType
import anc.core.Option
import anc.core.session.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Base64

data class UiState(
    val consoleLines: List<ConsoleLine> = emptyList(),
    val modules: List<ModuleMetadata> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val activeModule: AncModule? = null,
    val isRunning: Boolean = false,
    val searchQuery: String = ""
)

data class ConsoleLine(
    val text: String,
    val type: LineType = LineType.OUTPUT
)

enum class LineType { INPUT, OUTPUT, GOOD, BAD, STATUS, WARNING, ERROR }

data class VenomState(
    val payloads: List<ModuleMetadata> = emptyList(),
    val encoders: List<ModuleMetadata> = emptyList(),
    val selectedPayload: String = "",
    val payloadOptions: List<Option<*>> = emptyList(),
    val optionValues: Map<String, String> = emptyMap(),
    val format: String = "hex",
    val selectedEncoder: String = "",
    val output: String = "",
    val outputSize: Int = 0,
    val isGenerating: Boolean = false,
    val error: String? = null
)

class FrameworkViewModel : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _venomState = MutableStateFlow(VenomState())
    val venomState: StateFlow<VenomState> = _venomState.asStateFlow()

    private val framework: Framework get() = Framework.getInstance()

    init {
        observeEvents()
        refreshModules()
        refreshVenomLists()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            framework.eventBus.events.collect { event ->
                when (event) {
                    is EventBus.Event.ModuleOutput -> appendLine(event.line, classifyLine(event.line))
                    is EventBus.Event.ConsoleOutput -> appendLine(event.line, if (event.isError) LineType.ERROR else LineType.OUTPUT)
                    is EventBus.Event.SessionOpened -> {
                        appendLine("[*] Session ${event.sessionId} opened (${event.type}) — ${event.remote}", LineType.GOOD)
                        refreshSessions()
                    }
                    is EventBus.Event.SessionClosed -> {
                        appendLine("[-] Session ${event.sessionId} closed", LineType.STATUS)
                        refreshSessions()
                    }
                    is EventBus.Event.FrameworkStatus -> appendLine("[*] ${event.message}", LineType.STATUS)
                    is EventBus.Event.ModuleStarted -> _state.update { it.copy(isRunning = true) }
                    is EventBus.Event.ModuleFinished -> _state.update { it.copy(isRunning = false) }
                    else -> {}
                }
            }
        }
    }

    private fun classifyLine(line: String) = when {
        line.startsWith("[+]") -> LineType.GOOD
        line.startsWith("[-]") -> LineType.BAD
        line.startsWith("[*]") -> LineType.STATUS
        line.startsWith("[!]") -> LineType.WARNING
        else -> LineType.OUTPUT
    }

    fun executeCommand(raw: String) {
        val cmd = raw.trim()
        if (cmd.isBlank()) return
        appendLine(cmd, LineType.INPUT)
        viewModelScope.launch(Dispatchers.Default) {
            processCommand(cmd)
        }
    }

    private suspend fun processCommand(cmd: String) {
        val parts = cmd.split("\\s+".toRegex())
        val verb = parts[0].lowercase()
        val args = parts.drop(1)

        when (verb) {
            "help", "?" -> showHelp()
            "show" -> when (args.firstOrNull()?.lowercase()) {
                "modules", "all" -> showModules()
                "exploits"  -> showModulesByType(ModuleType.EXPLOIT)
                "auxiliary" -> showModulesByType(ModuleType.AUXILIARY)
                "sessions"  -> showSessions()
                "options"   -> showOptions()
                else -> output("Usage: show [modules|exploits|auxiliary|sessions|options]")
            }
            "search" -> searchModules(args.joinToString(" "))
            "use" -> useModule(args.joinToString(" "))
            "set" -> if (args.size >= 2) setOption(args[0], args.drop(1).joinToString(" ")) else output("Usage: set <option> <value>")
            "unset" -> if (args.isNotEmpty()) unsetOption(args[0]) else output("Usage: unset <option>")
            "run", "exploit" -> runModule()
            "back" -> back()
            "sessions" -> showSessions()
            "info" -> showInfo()
            "version" -> output("AncKit Framework v0.1.0 — Android Security Research Tool")
            "clear" -> _state.update { it.copy(consoleLines = emptyList()) }
            "exit", "quit" -> output("[*] Use the back button to exit.")
            else -> output("Unknown command: $verb  (type 'help' for commands)")
        }
    }

    private fun showHelp() {
        output("""
            Core Commands
            =============
            help            Show this help
            version         Show framework version
            clear           Clear the console

            Module Commands
            ===============
            show modules    List all modules
            show exploits   List exploit modules
            show auxiliary  List auxiliary modules
            search <term>   Search modules by name/description
            use <path>      Select a module (e.g. auxiliary/scanner/portscan/tcp)
            info            Show info about current module
            back            Deselect current module

            Module Options
            ==============
            show options    Show current module options
            set <opt> <val> Set an option value
            unset <opt>     Clear an option value
            run / exploit   Run the current module

            Sessions
            ========
            sessions        List active sessions
        """.trimIndent())
    }

    private fun showModules() {
        val mods = framework.moduleManager.allMeta()
        output("${mods.size} modules:\n")
        mods.forEach { m -> output("  ${m.fullName.padEnd(60)} ${m.rank}  ${m.description.take(50)}") }
    }

    private fun showModulesByType(type: ModuleType) {
        framework.moduleManager.byType(type).forEach { m ->
            output("  ${m.fullName.padEnd(60)} ${m.description.take(50)}")
        }
    }

    private fun searchModules(query: String) {
        if (query.isBlank()) { output("Usage: search <term>"); return }
        val results = framework.moduleManager.search(query)
        if (results.isEmpty()) { output("No results for '$query'"); return }
        output("${results.size} result(s):")
        results.forEach { m -> output("  ${m.fullName.padEnd(60)} ${m.description.take(50)}") }
    }

    private fun useModule(path: String) {
        val module = framework.moduleManager.create(path)
            ?: framework.moduleManager.search(path).firstOrNull()
                ?.let { framework.moduleManager.create(it.fullName) }
        if (module == null) { output("[-] Module not found: $path"); return }
        module.datastore.applyDefaults(module.options)
        _state.update { it.copy(activeModule = module) }
        output("[*] Using module: ${module.fullName}")
    }

    private fun setOption(key: String, value: String) {
        val mod = _state.value.activeModule ?: run { output("[-] No module selected"); return }
        mod.datastore[key] = value
        output("$key => $value")
    }

    private fun unsetOption(key: String) {
        // Datastore doesn't expose remove; set empty string as convention
        _state.value.activeModule?.datastore?.set(key, "") ?: output("[-] No module selected")
    }

    private fun showOptions() {
        val mod = _state.value.activeModule ?: run { output("No module selected."); return }
        output("Module options (${mod.fullName}):\n")
        mod.options.all().forEach { opt ->
            val current = mod.datastore[opt.name]?.toString() ?: opt.default?.toString() ?: "<unset>"
            val req = if (opt.required) "yes" else "no"
            output("  ${opt.name.padEnd(20)} ${current.padEnd(20)} $req  ${opt.description}")
        }
    }

    private fun showInfo() {
        val mod = _state.value.activeModule ?: run { output("No module selected."); return }
        output("""
            Name:        ${mod.name}
            Path:        ${mod.fullName}
            Rank:        ${mod.rank}
            Description: ${mod.description}
            Authors:     ${mod.authors.joinToString(", ")}
            References:  ${mod.references.joinToString(", ") { it.display }}
        """.trimIndent())
    }

    private fun showSessions() {
        val sessions = framework.sessionManager.all()
        if (sessions.isEmpty()) { output("No active sessions."); return }
        output("Active sessions (${sessions.size}):")
        sessions.forEach { s -> output("  #${s.id}  ${s.type.padEnd(8)} ${s.remoteAddress}") }
    }

    private suspend fun runModule() {
        val mod = _state.value.activeModule ?: run { output("[-] No module selected"); return }
        val errors = mod.options.validate(mod.datastore)
        if (errors.isNotEmpty()) {
            errors.forEach { output("[-] $it") }
            return
        }
        _state.update { it.copy(isRunning = true) }
        framework.eventBus.emit(EventBus.Event.ModuleStarted(mod.fullName))
        output("[*] Running ${mod.fullName}...")
        try {
            mod.run()
        } catch (e: Exception) {
            output("[!] Module error: ${e.message}")
        } finally {
            framework.eventBus.emit(EventBus.Event.ModuleFinished(mod.fullName, true))
            _state.update { it.copy(isRunning = false) }
        }
    }

    private fun back() {
        _state.update { it.copy(activeModule = null) }
        output("[*] Deselected module.")
    }

    private fun output(text: String) = appendLine(text, LineType.OUTPUT)

    private fun appendLine(text: String, type: LineType) {
        _state.update { state ->
            val lines = (state.consoleLines + ConsoleLine(text, type)).takeLast(2000)
            state.copy(consoleLines = lines)
        }
    }

    private fun refreshModules() {
        _state.update { it.copy(modules = framework.moduleManager.allMeta()) }
    }

    private fun refreshSessions() {
        _state.update { it.copy(sessions = framework.sessionManager.all()) }
    }

    fun updateSearch(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    // ── Venom (payload generator) ─────────────────────────────────────────────

    private fun refreshVenomLists() {
        _venomState.update {
            it.copy(
                payloads = framework.payloadManager.all(),
                encoders = framework.encoderManager.all()
            )
        }
    }

    fun selectVenomPayload(path: String) {
        val payload = framework.payloadManager.create(path) ?: return
        payload.datastore.applyDefaults(payload.options)
        val opts = payload.options.all()
        val defaults = opts.associate { opt ->
            opt.name to (payload.datastore[opt.name]?.toString() ?: opt.default?.toString() ?: "")
        }
        _venomState.update {
            it.copy(
                selectedPayload = path,
                payloadOptions = opts,
                optionValues = defaults,
                output = "",
                outputSize = 0,
                error = null
            )
        }
    }

    fun setVenomOption(key: String, value: String) {
        _venomState.update { it.copy(optionValues = it.optionValues + (key to value)) }
    }

    fun setVenomFormat(format: String) {
        _venomState.update { it.copy(format = format) }
    }

    fun setVenomEncoder(path: String) {
        _venomState.update { it.copy(selectedEncoder = path) }
    }

    fun generateVenomPayload() {
        val vs = _venomState.value
        if (vs.selectedPayload.isEmpty()) return
        _venomState.update { it.copy(isGenerating = true, error = null) }
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val payload = framework.payloadManager.create(vs.selectedPayload)
                    ?: throw IllegalArgumentException("Payload not found: ${vs.selectedPayload}")
                vs.optionValues.forEach { (k, v) -> if (v.isNotEmpty()) payload.datastore[k] = v }
                val errors = payload.options.validate(payload.datastore)
                if (errors.isNotEmpty()) {
                    _venomState.update { it.copy(isGenerating = false, error = errors.joinToString("; ")) }
                    return@launch
                }
                var raw = payload.generate()
                if (vs.selectedEncoder.isNotEmpty()) {
                    raw = framework.encoderManager.encode(raw, vs.selectedEncoder)
                }
                _venomState.update {
                    it.copy(
                        isGenerating = false,
                        output = formatVenomBytes(raw, vs.format),
                        outputSize = raw.size,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _venomState.update { it.copy(isGenerating = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    private fun formatVenomBytes(bytes: ByteArray, fmt: String): String {
        val list = bytes.toList()
        return when (fmt) {
            "hex"    -> bytes.joinToString("") { "%02x".format(it) }
            "c"      -> list.chunked(16).joinToString("\n") { line ->
                            line.joinToString(", ", "\"", "\"") { "\\x%02x".format(it.toInt() and 0xff) }
                        }
            "python" -> "shellcode = (\n" +
                        list.chunked(16).joinToString("\n") { line ->
                            "    b\"" + line.joinToString("") { "\\x%02x".format(it.toInt() and 0xff) } + "\""
                        } + "\n)"
            "base64" -> Base64.getEncoder().encodeToString(bytes)
            "kotlin" -> "val shellcode = byteArrayOf(\n" +
                        list.chunked(12).joinToString(",\n") { line ->
                            "    " + line.joinToString(", ") { "0x%02x".format(it.toInt() and 0xff) }
                        } + "\n)"
            else     -> bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
