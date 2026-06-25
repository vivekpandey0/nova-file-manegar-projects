package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FileUtil
import com.example.model.FileModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FileListItem(
    file: FileModel,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onCompress: () -> Unit = {},
    onExtract: () -> Unit = {},
    onRename: () -> Unit = {},
    onCopy: () -> Unit = {},
    isSelected: Boolean = false,
    showCheckbox: Boolean = false,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onAiSummarize: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    var menuExpanded by remember { mutableStateOf(false) }

    val iconInfo = if (file.isDirectory) {
        Pair(Icons.Rounded.Folder, Primary)
    } else {
        when (file.extension.lowercase()) {
            "jpg", "png", "webp" -> Pair(Icons.Rounded.Image, Color(0xFF6366F1))
            "mp4", "mkv" -> Pair(Icons.Rounded.Videocam, Secondary)
            "pdf", "doc", "docx" -> Pair(Icons.Rounded.Description, Color(0xFF10B981))
            "apk" -> Pair(Icons.Rounded.Android, Color(0xFFF59E0B))
            "zip", "rar" -> Pair(Icons.Rounded.Archive, Color(0xFF3B82F6))
            "wav", "mp3" -> Pair(Icons.Rounded.Audiotrack, Accent)
            else -> Pair(Icons.Rounded.InsertDriveFile, Color(0xFF94A3B8))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x0D000000))
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconInfo.second.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconInfo.first,
                contentDescription = null,
                tint = iconInfo.second,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    text = if (file.isDirectory) dateFormat.format(Date(file.lastModified)) else "${FileUtil.formatSize(file.size)} • ${dateFormat.format(Date(file.lastModified))}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (showCheckbox) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, if (isSelected) Primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .background(if (isSelected) Primary else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        } else {
             Box {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     IconButton(onClick = onToggleFavorite) {
                         Icon(
                             if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                             contentDescription = "Favorite",
                             tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                         )
                     }
                     IconButton(onClick = { menuExpanded = true }) {
                         Icon(
                             Icons.Rounded.MoreVert,
                             contentDescription = "More",
                             tint = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                     }
                 }
                 DropdownMenu(
                     expanded = menuExpanded,
                     onDismissRequest = { menuExpanded = false }
                 ) {
                     DropdownMenuItem(
                         text = { Text("AI Summarize / Analyze") },
                         onClick = { onAiSummarize(); menuExpanded = false },
                         leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color(0xFFA855F7)) }
                     )
                     if (file.extension.lowercase() == "zip") {
                         DropdownMenuItem(
                             text = { Text("Extract") },
                             onClick = { onExtract(); menuExpanded = false },
                             leadingIcon = { Icon(Icons.Rounded.Unarchive, contentDescription = null) }
                         )
                     } else {
                         DropdownMenuItem(
                             text = { Text("Compress (ZIP)") },
                             onClick = { onCompress(); menuExpanded = false },
                             leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) }
                         )
                     }
                     DropdownMenuItem(
                         text = { Text("Copy") },
                         onClick = { onCopy(); menuExpanded = false },
                         leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) }
                     )
                     DropdownMenuItem(
                         text = { Text("Rename") },
                         onClick = { onRename(); menuExpanded = false },
                         leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) }
                     )
                     DropdownMenuItem(
                         text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                         onClick = { onDelete(); menuExpanded = false },
                         leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                     )
                 }
             }
        }
    }
}
