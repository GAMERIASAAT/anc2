package anc.ui.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import anc.ui.android.viewmodel.ConsoleLine
import anc.ui.android.viewmodel.FrameworkViewModel
import anc.ui.android.viewmodel.LineType
import kotlinx.coroutines.launch

private val TerminalBackground = Color(0xFF0D1117)
private val TerminalText = Color(0xFFE6EDF3)
private val GoodColor = Color(0xFF3FB950)
private val BadColor = Color(0xFFF85149)
private val StatusColor = Color(0xFF58A6FF)
private val WarningColor = Color(0xFFD29922)
private val InputColor = Color(0xFFCFF3AC)

@Composable
fun ConsoleScreen(viewModel: FrameworkViewModel) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    // Auto-scroll to bottom on new output
    LaunchedEffect(state.consoleLines.size) {
        if (state.consoleLines.isNotEmpty()) {
            listState.animateScrollToItem(state.consoleLines.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
        // Active module indicator
        state.activeModule?.let { mod ->
            Surface(
                color = Color(0xFF161B22),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "  ${mod.fullName}  ",
                    color = StatusColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Output area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(state.consoleLines) { line ->
                Text(
                    text = line.text,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = lineColor(line)
                    ),
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        // Running indicator
        if (state.isRunning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = StatusColor,
                trackColor = Color(0xFF21262D)
            )
        }

        Divider(color = Color(0xFF30363D))

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val prompt = if (state.activeModule != null)
                "anc(${state.activeModule!!.modulePath.substringAfterLast('/')}) > "
            else
                "anc > "

            Text(
                text = prompt,
                color = InputColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )

            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = TerminalText
                ),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    cursorColor = GoodColor
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.executeCommand(inputText)
                        inputText = ""
                    }
                }),
                singleLine = true,
                placeholder = {
                    Text("type a command...", color = Color(0xFF6E7681), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            )
        }
    }
}

@Composable
private fun lineColor(line: ConsoleLine): Color = when (line.type) {
    LineType.GOOD    -> GoodColor
    LineType.BAD     -> BadColor
    LineType.STATUS  -> StatusColor
    LineType.WARNING -> WarningColor
    LineType.ERROR   -> BadColor
    LineType.INPUT   -> InputColor
    LineType.OUTPUT  -> TerminalText
}
