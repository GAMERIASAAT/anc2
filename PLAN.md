# AncKit — Kotlin Security Framework (Android-First)

## Overview

AncKit is a full Kotlin security framework inspired by the architecture of open-source penetration
testing frameworks, designed to run natively on Android and the JVM. It leverages Kotlin idioms:
coroutines for concurrency, sealed classes for module hierarchies, interfaces with default methods
as mixin replacements, and the Kotlin multiplatform toolchain for cross-platform support.

---

## Goals

1. Structural parity with open-source security framework conventions (inspired by `rapid7/metasploit-framework`)
2. Android-first: every component must work on Android API 26+ (no `java.*` APIs unavailable on Android)
3. Statically-typed equivalent of Ruby's mixin/dynamic module system
4. Full module lifecycle: discover → load → configure → run → session
5. Custom Phantom agent session layer with TLV binary protocol
6. REST/RPC API with a clean, framework-agnostic spec

---

## Technology Stack

| Concern                | Choice                                  |
|------------------------|-----------------------------------------|
| Language               | Kotlin 2.x (JVM + Android targets)     |
| Build system           | Gradle (Kotlin DSL), multi-module       |
| Android min SDK        | 26 (Android 8.0)                        |
| Concurrency            | Kotlin Coroutines + Flow                |
| Networking             | OkHttp (Android-safe) + Ktor Client     |
| Serialization          | kotlinx.serialization                   |
| Database (JVM)         | Exposed ORM + PostgreSQL/SQLite         |
| Database (Android)     | Room (SQLite)                           |
| CLI (JVM only)         | Clikt + Mordant (terminal UI)           |
| HTTP server (JVM only) | Ktor Server                             |
| Crypto                 | Bouncy Castle (both platforms)          |
| DI                     | Koin                                    |
| Testing                | JUnit5 + Kotest + MockK                 |

---

## Project Directory Structure

Follows proven security framework conventions, adapted for a Gradle multi-module Kotlin project.

```
anckit/
├── build.gradle.kts                  # Root build script
├── settings.gradle.kts               # Module declarations
├── gradle/
│   └── libs.versions.toml            # Version catalog
│
├── core/                             # ≈ lib/rex/ + framework core
│   └── src/
│       ├── commonMain/kotlin/anc/core/
│       │   ├── Framework.kt          # Framework singleton / DI root
│       │   ├── Module.kt             # Abstract base for all modules
│       │   ├── ModuleManager.kt      # Discovery, loading, caching
│       │   ├── SessionManager.kt     # Active session tracking
│       │   ├── PayloadManager.kt     # Payload generation pipeline
│       │   ├── EncoderManager.kt     # Encoder chain
│       │   ├── DbManager.kt          # Persistence abstraction
│       │   ├── EventBus.kt           # Observer / UI events
│       │   ├── Options.kt            # Module option DSL
│       │   ├── Datastore.kt          # Key-value option store
│       │   ├── exploit/
│       │   │   ├── Exploit.kt        # Abstract Exploit base
│       │   │   ├── Remote.kt         # Remote exploit mixin interface
│       │   │   ├── Local.kt          # Local privilege-escalation interface
│       │   │   ├── CmdStager.kt      # Command staging mixin
│       │   │   ├── EggHunter.kt      # Egg hunting mixin
│       │   │   └── FileDropper.kt    # File drop mixin
│       │   ├── auxiliary/
│       │   │   └── Auxiliary.kt      # Abstract Auxiliary base
│       │   ├── post/
│       │   │   └── Post.kt           # Abstract Post base
│       │   ├── payload/
│       │   │   ├── Payload.kt        # Payload sealed hierarchy
│       │   │   ├── Single.kt         # Single-stage payload
│       │   │   ├── Stager.kt         # Stager payload
│       │   │   ├── Stage.kt          # Stage payload
│       │   │   └── Adapter.kt        # Payload adapter
│       │   ├── encoder/
│       │   │   └── Encoder.kt        # Abstract Encoder base
│       │   ├── nop/
│       │   │   └── Nop.kt            # Abstract NOP generator
│       │   └── session/
│       │       ├── Session.kt        # Abstract Session
│       │       ├── ShellSession.kt   # Shell session
│       │       ├── PhantomSession.kt # Phantom agent session (TLV protocol)
│       │       └── handler/
│       │           ├── Handler.kt    # Payload handler interface
│       │           ├── ReverseHandler.kt
│       │           └── BindHandler.kt
│
├── rex/                              # Low-level toolkit (sockets, crypto, protocols)
│   └── src/commonMain/kotlin/rex/
│       ├── arch/                     # Architecture constants (x86, x64, ARM, ARM64, MIPS)
│       ├── crypto/
│       │   ├── Aes.kt
│       │   ├── Rc4.kt
│       │   ├── Sha256.kt
│       │   └── TlsClient.kt
│       ├── proto/
│       │   ├── Http.kt               # HTTP helper (OkHttp wrapper)
│       │   ├── Smb.kt                # SMB protocol primitives
│       │   ├── Ftp.kt
│       │   ├── Ssh.kt                # SSH via JSch / sshj
│       │   ├── Telnet.kt
│       │   ├── Snmp.kt
│       │   └── Dns.kt
│       ├── socket/
│       │   ├── TcpSocket.kt
│       │   ├── UdpSocket.kt
│       │   └── SslSocket.kt
│       ├── text/
│       │   ├── Table.kt              # ASCII table renderer
│       │   ├── Hex.kt                # Hex encoding/decoding
│       │   └── Patterns.kt           # Common regex patterns
│       ├── java/                     # JVM-only overrides (java.net)
│       └── android/                  # Android-only overrides
│
├── base/                             # Base wrappers and simplified interfaces
│   └── src/commonMain/kotlin/anc/base/
│       ├── Config.kt                 # Framework-wide config
│       ├── Logging.kt                # Logging facade (Timber on Android)
│       ├── PersistentStorage.kt      # Storage interface
│       ├── serializer/
│       │   └── JsonSerializer.kt
│       └── sessions/
│           ├── DefaultShell.kt
│           └── DefaultPhantom.kt     # Default Phantom agent session
│
├── modules/                          # ≈ modules/  (actual exploit/aux/etc content)
│   ├── exploits/
│   │   ├── android/
│   │   │   └── local/
│   │   │       └── ExampleAndroidLocalExploit.kt
│   │   ├── windows/
│   │   ├── linux/
│   │   ├── unix/
│   │   ├── osx/
│   │   └── multi/
│   ├── auxiliary/
│   │   ├── scanner/
│   │   │   ├── PortScanner.kt
│   │   │   └── ServiceVersionScanner.kt
│   │   ├── gather/
│   │   ├── fuzzers/
│   │   ├── dos/
│   │   ├── sniffer/
│   │   └── admin/
│   ├── payloads/
│   │   ├── singles/
│   │   │   ├── android/
│   │   │   ├── linux/
│   │   │   └── windows/
│   │   ├── stagers/
│   │   │   ├── android/
│   │   │   ├── linux/
│   │   │   └── windows/
│   │   └── stages/
│   │       └── phantom/              # Phantom agent stage (custom TLV agent)
│   ├── encoders/
│   │   ├── x86/
│   │   ├── x64/
│   │   └── arm/
│   ├── nops/
│   │   ├── x86/
│   │   └── arm/
│   ├── post/
│   │   ├── android/
│   │   ├── linux/
│   │   └── windows/
│   └── evasion/
│
├── plugins/                          # Runtime plugin extensions
│   └── src/main/kotlin/anc/plugins/
│       └── PluginBase.kt
│
├── ui/                               # User interface layer
│   ├── console/                      # JVM CLI console (ancconsole)
│   │   └── src/main/kotlin/anc/ui/console/
│   │       ├── Console.kt
│   │       ├── CommandDispatcher.kt
│   │       ├── commands/
│   │       │   ├── Use.kt
│   │       │   ├── Set.kt
│   │       │   ├── Run.kt
│   │       │   ├── Sessions.kt
│   │       │   ├── Search.kt
│   │       │   └── Info.kt
│   │       └── Shell.kt              # Interactive REPL
│   └── android/                      # Android UI (Jetpack Compose)
│       └── src/main/kotlin/anc/ui/android/
│           ├── MainActivity.kt
│           ├── screens/
│           │   ├── ConsoleScreen.kt
│           │   ├── ModulesScreen.kt
│           │   └── SessionsScreen.kt
│           └── viewmodel/
│               └── FrameworkViewModel.kt
│
├── app/                              # Data models and validators
│   └── src/main/kotlin/anc/app/
│       ├── models/
│       │   ├── Host.kt
│       │   ├── Service.kt
│       │   ├── Vuln.kt
│       │   ├── Credential.kt
│       │   └── Note.kt
│       └── validators/
│           └── OptionValidator.kt
│
├── db/                               # ≈ db/  (migrations, schema)
│   ├── migrations/
│   └── schema.sql
│
├── webservices/                      # RPC / REST API server
│   └── src/main/kotlin/anc/web/
│       ├── RpcServer.kt              # JSON-RPC compatible server
│       ├── RestApi.kt                # REST API (Ktor routes)
│       └── auth/
│           └── TokenAuth.kt
│
├── data/                             # ≈ data/  (wordlists, signatures, templates)
│   ├── wordlists/
│   ├── exploit_info/
│   └── templates/
│       ├── payloads/
│       └── evasion/
│
├── tools/                            # ≈ tools/  (CLI utilities)
│   ├── ancvenom/                     # Payload generator CLI
│   └── anc-pattern/                  # Cyclic pattern tool
│
├── scripts/                          # Helper scripts
│   ├── setup.sh
│   └── generate-module.sh
│
├── docs/                             # Architecture + developer docs
│   └── architecture.md
│
├── docker/
│   └── Dockerfile
│
└── android-app/                      # Standalone Android APK entry point
    ├── src/main/
    │   ├── kotlin/com/anckit/
    │   │   └── App.kt
    │   └── AndroidManifest.xml
    └── build.gradle.kts
```

---

## Phase-by-Phase Implementation Plan

### Phase 1 — Core Infrastructure (Weeks 1–4)

**Goal:** Skeleton compiles on JVM and Android. No modules yet.

| Task | Files | Notes |
|------|-------|-------|
| Initialize Gradle multi-module project | `settings.gradle.kts`, root `build.gradle.kts` | Use version catalog |
| Define core module interfaces | `core/Module.kt`, `Options.kt`, `Datastore.kt` | Sealed class hierarchy |
| Implement Framework singleton | `core/Framework.kt` | Koin DI root |
| Implement EventBus | `core/EventBus.kt` | Kotlin Flow-based |
| Build ModuleManager skeleton | `core/ModuleManager.kt` | ServiceLoader on JVM, reflection on Android |
| Create base Exploit/Auxiliary/Post/Payload/Encoder/Nop abstract classes | `core/exploit/`, `core/auxiliary/` etc. | Mirrors open framework class tree |
| Implement Logging facade | `base/Logging.kt` | Timber on Android, SLF4J on JVM |
| Setup Room (Android) + Exposed (JVM) | `db/` | Shared `DbManager` interface |
| Write unit tests for Framework, Options, Datastore | `core/src/test/` | Kotest |

**Deliverable:** `./gradlew build` succeeds; JVM jar and Android AAR generated.

---

### Phase 2 — Rex Low-Level Toolkit (Weeks 5–7)

**Goal:** Network, crypto, and protocol primitives that work on Android.

| Task | Files | Notes |
|------|-------|-------|
| Architecture constants | `rex/arch/` | ARM, ARM64, x86, x64, MIPS |
| TCP/UDP/SSL sockets | `rex/socket/` | `java.net` wrapped; Android-safe |
| HTTP helper | `rex/proto/Http.kt` | OkHttp, coroutine-based |
| Crypto primitives | `rex/crypto/` | Bouncy Castle |
| SSH client | `rex/proto/Ssh.kt` | sshj library |
| Hex/pattern utilities | `rex/text/` | Pure Kotlin |
| SMB primitives | `rex/proto/Smb.kt` | Custom implementation |

**Deliverable:** Rex module passes all protocol unit tests.

---

### Phase 3 — Payload Pipeline (Weeks 8–10)

**Goal:** Generate shellcode payloads, encode them, deliver via handlers.

| Task | Files | Notes |
|------|-------|-------|
| Payload sealed class hierarchy | `core/payload/Payload.kt` | Single / Stager / Stage / Adapter |
| PayloadManager | `core/PayloadManager.kt` | Builds payload by type+arch+platform |
| EncoderManager + Encoder base | `core/EncoderManager.kt` | Chain of responsibility pattern |
| NOP generators | `modules/nops/` | x86 / ARM |
| First Android payload: reverse shell | `modules/payloads/singles/android/ReverseShell.kt` | |
| First Linux payload: reverse shell | `modules/payloads/singles/linux/ReverseShellX86.kt` | |
| Payload handlers | `core/session/handler/` | Reverse TCP, Bind TCP |
| ancvenom CLI | `tools/ancvenom/` | Clikt-based |

**Deliverable:** `ancvenom -p android/shell/reverse_tcp LHOST=x LPORT=4444 -f raw` produces binary.

---

### Phase 4 — Session & Post-Exploitation (Weeks 11–13)

**Goal:** Working shell and Phantom agent sessions post-exploitation.

| Task | Files | Notes |
|------|-------|-------|
| Session base + lifecycle | `core/session/Session.kt` | Open/close/interact |
| ShellSession | `core/session/ShellSession.kt` | Bi-directional pipe |
| PhantomSession | `core/session/PhantomSession.kt` | Custom TLV protocol parser |
| SessionManager | `core/SessionManager.kt` | Background session list |
| Post module base | `core/post/Post.kt` | Runs in session context |
| First Android post module: device info | `modules/post/android/GatherDeviceInfo.kt` | |
| First Linux post module: local users | `modules/post/linux/GatherUsers.kt` | |

**Deliverable:** End-to-end: exploit → shell session → run post module.

---

### Phase 5 — Exploit Modules (Weeks 14–18)

**Goal:** Implement first real exploit modules across platforms.

| Task | Files | Notes |
|------|-------|-------|
| Exploit mixin interfaces | `core/exploit/Remote.kt`, `Local.kt`, `CmdStager.kt` | Kotlin interfaces with defaults |
| Android local exploit (intent hijack demo) | `modules/exploits/android/local/` | Proof of concept |
| Linux exploit (SUID shell demo) | `modules/exploits/linux/local/` | |
| Multi/handler | `modules/exploits/multi/handler/` | Generic payload catcher |
| Auxiliary port scanner | `modules/auxiliary/scanner/PortScanner.kt` | Coroutines, parallel |
| Auxiliary HTTP banner grabber | `modules/auxiliary/scanner/HttpVersion.kt` | |

**Deliverable:** `use exploits/multi/handler`, `set payload`, `run` catches a reverse shell.

---

### Phase 6 — Console UI (Weeks 19–21)

**Goal:** `ancconsole` interactive terminal for JVM; Compose UI for Android.

#### JVM Console
| Task | Files |
|------|-------|
| REPL shell loop | `ui/console/Shell.kt` |
| Command dispatcher | `ui/console/CommandDispatcher.kt` |
| `use`, `show`, `set`, `run`, `back`, `sessions`, `search`, `info` commands | `ui/console/commands/` |
| Tab completion | `ui/console/Completion.kt` |
| Colored output (Mordant) | Throughout console |

#### Android UI (Jetpack Compose)
| Task | Files |
|------|-------|
| Compose terminal emulator screen | `ui/android/screens/ConsoleScreen.kt` |
| Module browser screen | `ui/android/screens/ModulesScreen.kt` |
| Sessions screen | `ui/android/screens/SessionsScreen.kt` |
| FrameworkViewModel (coroutines bridge) | `ui/android/viewmodel/` |

**Deliverable:** Full interactive console on JVM; functional Compose UI on Android.

---

### Phase 7 — Database & Models (Weeks 22–23)

**Goal:** Persist hosts, services, vulns, creds (mirrors MSF workspace model).

| Task | Files | Notes |
|------|-------|-------|
| Data models | `app/models/` | Host, Service, Vuln, Credential, Note, Workspace |
| Room entities (Android) | `db/android/` | |
| Exposed tables (JVM) | `db/jvm/` | |
| DbManager implementation | `core/DbManager.kt` | Platform-specific impls |
| ORM migrations | `db/migrations/` | Versioned SQL scripts |

---

### Phase 8 — Web Services & RPC (Weeks 24–25)

**Goal:** Ktor-based RPC/REST server with a clean, self-contained API spec.

| Task | Files | Notes |
|------|-------|-------|
| JSON-RPC server | `webservices/RpcServer.kt` | AncKit RPC API |
| REST API routes | `webservices/RestApi.kt` | Hosts, sessions, modules CRUD |
| Token authentication | `webservices/auth/TokenAuth.kt` | |
| WebSocket for live console | `webservices/ConsoleSocket.kt` | |

---

### Phase 9 — Plugin System (Week 26)

**Goal:** Runtime plugin loading for extending framework capabilities.

| Task | Files | Notes |
|------|-------|-------|
| PluginBase interface | `plugins/PluginBase.kt` | |
| Plugin loader | `core/PluginManager.kt` | ServiceLoader + Android asset loading |
| Example plugin: auto-exploit | `plugins/AutoExploit.kt` | |

---

### Phase 10 — Hardening, Docs & CI (Weeks 27–28)

| Task |
|------|
| Full test coverage for core + rex (target 80%) |
| ProGuard/R8 rules for Android release builds |
| GitHub Actions CI: build + test on JVM and Android emulator |
| Developer docs in `docs/` |
| Docker image for JVM deployment |
| README with quickstart |

---

## Key Kotlin Design Decisions (Mapping Ruby → Kotlin)

### 1. Mixins → Interfaces with Default Methods

Ruby uses `include Anc::Exploit::Remote`. Kotlin uses interfaces:

```kotlin
// core/exploit/Remote.kt
interface Remote {
    val rhost: String
    val rport: Int
    fun connect(): Socket = TcpSocket.connect(rhost, rport)
    fun sslConnect(): SSLSocket = SslSocket.connect(rhost, rport)
}

// modules/exploits/linux/ExampleExploit.kt
class ExampleExploit : Exploit(), Remote, HttpMixin {
    override val rhost = datastore["RHOST"] as String
    override val rport = datastore["RPORT"] as Int
    override fun exploit() { /* ... */ }
}
```

### 2. Dynamic Module Loading → ServiceLoader + Annotation Processor

On JVM, modules are loaded via `ServiceLoader`. On Android, modules bundled as
`assets/modules/**/*.class` are loaded via a custom `DexClassLoader` wrapper.

```kotlin
// core/ModuleManager.kt
class ModuleManager(private val framework: Framework) {
    private val cache = ConcurrentHashMap<String, ModuleMetadata>()

    fun loadModules(paths: List<Path>) {
        ServiceLoader.load(AncModule::class.java).forEach { module ->
            cache[module.fullName] = ModuleMetadata.from(module)
        }
    }
}
```

### 3. Options DSL

```kotlin
// core/Options.kt
class Module {
    val options = OptionsBuilder().apply {
        required<String>("RHOST") { description = "Target address" }
        required<Int>("RPORT") { description = "Target port"; default = 4444 }
        optional<Boolean>("SSL") { description = "Use SSL"; default = false }
    }.build()
}
```

### 4. Coroutines for Concurrency (replaces Ruby threads)

```kotlin
// Every exploit/auxiliary run() is a suspend function
class PortScanner : Auxiliary() {
    override suspend fun run() = coroutineScope {
        (1..65535).map { port ->
            async(Dispatchers.IO) { checkPort(rhost, port) }
        }.awaitAll()
    }
}
```

### 5. Sealed Classes for Payload Hierarchy

```kotlin
sealed class Payload {
    abstract val arch: Arch
    abstract val platform: Platform
    abstract suspend fun generate(options: Datastore): ByteArray

    class Single(override val arch: Arch, override val platform: Platform,
                 val shellcode: suspend (Datastore) -> ByteArray) : Payload() {
        override suspend fun generate(options: Datastore) = shellcode(options)
    }
    class Stager(...) : Payload()
    class Stage(...) : Payload()
}
```

### 6. Android-Specific Payload Notes

Android payloads must use:
- `android.net.LocalSocket` or `java.net.Socket` (available API 26+)
- No `Runtime.exec()` for shell — use `ProcessBuilder` with `/system/bin/sh`
- Phantom agent Android stage: compiled to `.dex` and loaded via `DexClassLoader`
- Permissions declared in `AndroidManifest.xml` (INTERNET minimum)

---

## Module Metadata Format

Every module defines metadata via a companion object (mirrors the Ruby framework's `def initialize` pattern):

```kotlin
class EternalBlue : Exploit(), Remote {
    companion object : ModuleInfo {
        override val name = "MS17-010 EternalBlue SMB RCE"
        override val description = "Exploits a buffer overflow in SMBv1"
        override val authors = listOf("Shadow Brokers", "hdm")
        override val references = listOf(
            Reference.CVE("2017-0144"),
            Reference.MSB("MS17-010"),
            Reference.URL("https://technet.microsoft.com/en-us/library/security/ms17-010.aspx")
        )
        override val platform = listOf(Platform.WINDOWS)
        override val arch = listOf(Arch.X86, Arch.X64)
        override val rank = Rank.GREAT
        override val targets = listOf(
            Target("Windows 7 SP1 x64", Arch.X64),
            Target("Windows Server 2008 R2 x64", Arch.X64)
        )
    }
}
```

---

## Android-Specific Components

### android-app/ Module

The standalone Android APK wires up the framework with an Android-aware DI config:

```kotlin
// android-app/src/main/kotlin/com/anckit/App.kt
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(androidFrameworkModule, rexAndroidModule, dbAndroidModule)
        }
        Framework.initialize()
    }
}
```

### Android Module Loader

Because Android cannot use `ServiceLoader` with split APKs reliably, modules are
registered via a generated `ModuleRegistry.kt` (annotation processor at compile time):

```kotlin
@Module(path = "exploits/android/local/example")
class ExampleAndroidExploit : Exploit(), Local { ... }
```

An annotation processor generates:
```kotlin
// generated/ModuleRegistry.kt
object ModuleRegistry {
    val all: List<KClass<out AncModule>> = listOf(
        ExampleAndroidExploit::class,
        PortScanner::class,
        // ...
    )
}
```

---

## Permissions Required (Android)

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<!-- For post modules that gather device info -->
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
```

---

## Build Outputs

| Artifact | Output |
|----------|--------|
| `ancconsole` | JVM fat JAR — interactive console |
| `ancvenom` | JVM fat JAR — payload generator CLI |
| `anckit.apk` | Android APK — full framework on Android |
| `core.aar` | Android library for embedding in other apps |
| Docker image | `ghcr.io/anckit:latest` |

---

## Milestones Summary

| Milestone | Target | Deliverable |
|-----------|--------|-------------|
| M1 | Week 4 | Project skeleton builds on JVM + Android |
| M2 | Week 7 | Rex toolkit functional with tests |
| M3 | Week 10 | Payload generation + encoding working |
| M4 | Week 13 | End-to-end shell session |
| M5 | Week 18 | First real exploit modules |
| M6 | Week 21 | Interactive console (JVM + Android UI) |
| M7 | Week 23 | Database persistence |
| M8 | Week 25 | RPC/REST API live |
| M9 | Week 26 | Plugin system |
| M10 | Week 28 | 80% test coverage, CI, Docker, docs |
