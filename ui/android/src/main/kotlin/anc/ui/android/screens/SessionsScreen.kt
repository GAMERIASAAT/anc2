package anc.ui.android.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import anc.core.Framework
import anc.core.session.Session
import anc.ui.android.viewmodel.FrameworkViewModel

@Composable
fun SessionsScreen(viewModel: FrameworkViewModel) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(16.dp)
    ) {
        Text(
            text = "Active Sessions",
            color = Color(0xFFE6EDF3),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (state.sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No active sessions",
                        color = Color(0xFF6E7681),
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Run an exploit or use multi/handler\nto catch a callback",
                        color = Color(0xFF484F58),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.sessions, key = { it.id }) { session ->
                    SessionCard(session = session, onClose = {
                        Framework.getInstance().sessionManager.close(session.id)
                    })
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: Session, onClose: () -> Unit) {
    val typeColor = when (session.type) {
        "shell"   -> Color(0xFF3FB950)
        "phantom" -> Color(0xFF58A6FF)
        else      -> Color(0xFF8B949E)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = typeColor.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = session.type.uppercase(),
                            color = typeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "#${session.id}",
                        color = Color(0xFF8B949E),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = session.remoteAddress,
                    color = Color(0xFFE6EDF3),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (session.isAlive) "alive" else "closed",
                    color = if (session.isAlive) Color(0xFF3FB950) else Color(0xFFF85149),
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close session",
                    tint = Color(0xFF6E7681)
                )
            }
        }
    }
}
