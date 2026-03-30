package anc.ui.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import anc.ui.android.viewmodel.FrameworkViewModel

private val V_BG      = Color(0xFF0D1117)
private val V_SURFACE = Color(0xFF161B22)
private val V_BORDER  = Color(0xFF30363D)
private val V_TEXT    = Color(0xFFE6EDF3)
private val V_MUTED   = Color(0xFF8B949E)
private val V_GREEN   = Color(0xFF3FB950)
private val V_BLUE    = Color(0xFF58A6FF)
private val V_RED     = Color(0xFFF85149)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VenomScreen(viewModel: FrameworkViewModel) {
    val venom by viewModel.venomState.collectAsState()
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(V_BG)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "PAYLOAD GENERATOR",
            color = V_BLUE,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace
        )

        // ── Payload selection ─────────────────────────────────────────────────
        VenomCard("PAYLOAD") {
            VenomDropdown(
                items = venom.payloads.map { it.fullName },
                selected = venom.selectedPayload,
                placeholder = "Select payload...",
                onSelect = { viewModel.selectVenomPayload(it) }
            )
        }

        // ── Dynamic options ───────────────────────────────────────────────────
        if (venom.payloadOptions.isNotEmpty()) {
            VenomCard("OPTIONS") {
                venom.payloadOptions.forEach { opt ->
                    val value = venom.optionValues[opt.name] ?: ""
                    OutlinedTextField(
                        value = value,
                        onValueChange = { viewModel.setVenomOption(opt.name, it) },
                        label = {
                            Text(
                                buildString {
                                    append(opt.name)
                                    if (opt.required) append(" *")
                                },
                                color = if (opt.required) V_BLUE else V_MUTED,
                                fontSize = 12.sp
                            )
                        },
                        placeholder = {
                            Text(opt.description.take(40), color = V_MUTED, fontSize = 12.sp)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = venomTextFieldColors(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = V_TEXT
                        )
                    )
                }
            }
        }

        // ── Encoder ───────────────────────────────────────────────────────────
        VenomCard("ENCODER  (optional)") {
            VenomDropdown(
                items = venom.encoders.map { it.fullName },
                selected = venom.selectedEncoder,
                placeholder = "None",
                allowNone = true,
                onSelect = { viewModel.setVenomEncoder(it) }
            )
        }

        // ── Output format ─────────────────────────────────────────────────────
        VenomCard("FORMAT") {
            val formats = listOf("hex", "c", "python", "base64", "kotlin", "raw")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                formats.forEach { fmt ->
                    FilterChip(
                        selected = venom.format == fmt,
                        onClick = { viewModel.setVenomFormat(fmt) },
                        label = {
                            Text(fmt, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = V_BLUE.copy(alpha = 0.15f),
                            selectedLabelColor = V_BLUE,
                            labelColor = V_MUTED
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = venom.format == fmt,
                            selectedBorderColor = V_BLUE.copy(alpha = 0.5f),
                            borderColor = V_BORDER
                        )
                    )
                }
            }
        }

        // ── Generate button ───────────────────────────────────────────────────
        Button(
            onClick = { viewModel.generateVenomPayload() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = venom.selectedPayload.isNotEmpty() && !venom.isGenerating,
            colors = ButtonDefaults.buttonColors(
                containerColor = V_GREEN.copy(alpha = 0.85f),
                disabledContainerColor = V_BORDER
            ),
            shape = MaterialTheme.shapes.small
        ) {
            if (venom.isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = V_BG,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("GENERATING...", fontFamily = FontFamily.Monospace,
                     letterSpacing = 1.sp, color = V_BG, fontSize = 13.sp)
            } else {
                Text("GENERATE", fontFamily = FontFamily.Monospace,
                     letterSpacing = 2.sp, color = V_BG, fontSize = 13.sp)
            }
        }

        // ── Error ─────────────────────────────────────────────────────────────
        venom.error?.let { err ->
            Text(
                "[-] $err",
                color = V_RED,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // ── Output ────────────────────────────────────────────────────────────
        if (venom.output.isNotEmpty()) {
            VenomCard(
                title = "OUTPUT  (${venom.outputSize} bytes)",
                action = {
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(venom.output)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy to clipboard",
                            tint = V_MUTED,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            ) {
                SelectionContainer {
                    Text(
                        text = venom.output,
                        color = V_GREEN,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(V_BG.copy(alpha = 0.6f), shape = MaterialTheme.shapes.small)
                            .padding(10.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VenomDropdown(
    items: List<String>,
    selected: String,
    placeholder: String,
    allowNone: Boolean = false,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.ifEmpty { placeholder },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors = venomTextFieldColors(),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = if (selected.isEmpty()) V_MUTED else V_TEXT
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(V_SURFACE)
        ) {
            if (allowNone) {
                DropdownMenuItem(
                    text = {
                        Text("None", color = V_MUTED,
                             fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    },
                    onClick = { onSelect(""); expanded = false }
                )
                HorizontalDivider(color = V_BORDER, thickness = 0.5.dp)
            }
            items.forEach { path ->
                DropdownMenuItem(
                    text = {
                        Text(path, color = V_TEXT,
                             fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    },
                    onClick = { onSelect(path); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun VenomCard(
    title: String,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = V_SURFACE,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Text(
                    title,
                    color = V_MUTED,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                action?.invoke()
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun venomTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = V_BLUE,
    unfocusedBorderColor = V_BORDER,
    focusedTextColor = V_TEXT,
    unfocusedTextColor = V_TEXT,
    cursorColor = V_GREEN,
    focusedLabelColor = V_BLUE,
    unfocusedLabelColor = V_MUTED,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)
