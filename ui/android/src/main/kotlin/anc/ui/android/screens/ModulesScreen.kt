package anc.ui.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import anc.core.ModuleMetadata
import anc.core.ModuleType
import anc.ui.android.viewmodel.FrameworkViewModel

@Composable
fun ModulesScreen(viewModel: FrameworkViewModel, onUseModule: (String) -> Unit = {}) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<ModuleType?>(null) }

    val filtered = remember(state.modules, searchQuery, selectedType) {
        state.modules
            .filter { selectedType == null || it.moduleType == selectedType }
            .filter { searchQuery.isBlank() || it.fullName.contains(searchQuery, true) || it.description.contains(searchQuery, true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            placeholder = { Text("Search modules...", color = Color(0xFF6E7681)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF6E7681)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF58A6FF),
                unfocusedBorderColor = Color(0xFF30363D),
                focusedTextColor = Color(0xFFE6EDF3),
                unfocusedTextColor = Color(0xFFE6EDF3),
                cursorColor = Color(0xFF58A6FF)
            ),
            singleLine = true
        )

        // Type filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == null,
                onClick = { selectedType = null },
                label = { Text("All", fontSize = 12.sp) }
            )
            ModuleType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = if (selectedType == type) null else type },
                    label = { Text(type.path.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) }
                )
            }
        }

        Text(
            text = "${filtered.size} module(s)",
            color = Color(0xFF6E7681),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(filtered, key = { it.fullName }) { meta ->
                ModuleItem(meta = meta, onClick = { onUseModule(meta.fullName) })
            }
        }
    }
}

@Composable
private fun ModuleItem(meta: ModuleMetadata, onClick: () -> Unit) {
    val typeColor = when (meta.moduleType) {
        ModuleType.EXPLOIT   -> Color(0xFFF85149)
        ModuleType.AUXILIARY -> Color(0xFF58A6FF)
        ModuleType.POST      -> Color(0xFFD29922)
        ModuleType.PAYLOAD   -> Color(0xFF3FB950)
        else                 -> Color(0xFF8B949E)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = typeColor.copy(alpha = 0.15f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.width(72.dp)
        ) {
            Text(
                text = meta.moduleType.path.take(3).uppercase(),
                color = typeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = meta.fullName,
                color = Color(0xFFE6EDF3),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = meta.description,
                color = Color(0xFF8B949E),
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        Text(
            text = meta.rank.toString(),
            color = rankColor(meta.rank.label),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
    Divider(color = Color(0xFF21262D), thickness = 0.5.dp)
}

private fun rankColor(rank: String): Color = when (rank) {
    "excellent", "great" -> Color(0xFF3FB950)
    "good", "normal"     -> Color(0xFF58A6FF)
    "average"            -> Color(0xFFD29922)
    else                 -> Color(0xFF8B949E)
}
