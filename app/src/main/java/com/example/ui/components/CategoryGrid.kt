package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class Category(val title: String, val icon: ImageVector, val color: Color, val size: String)

@Composable
fun CategoryGrid(
    categoryCounts: Map<String, Int>,
    modifier: Modifier = Modifier,
    onCategoryClick: (String) -> Unit
) {
    val categories = listOf(
        Category("Documents", Icons.Rounded.Description, Color(0xFF6366F1), "${categoryCounts["Documents"] ?: 0} Items"),
        Category("Images", Icons.Rounded.Image, Color(0xFFA855F7), "${categoryCounts["Images"] ?: 0} Items"),
        Category("Videos", Icons.Rounded.Videocam, Color(0xFFFB7185), "${categoryCounts["Videos"] ?: 0} Items"),
        Category("Audio", Icons.Rounded.Audiotrack, Color(0xFF10B981), "${categoryCounts["Audio"] ?: 0} Items"),
        Category("APK", Icons.Rounded.Android, Color(0xFFF59E0B), "${categoryCounts["APK"] ?: 0} Items"),
        Category("Archives", Icons.Rounded.Archive, Color(0xFF3B82F6), "${categoryCounts["Archives"] ?: 0} Items"),
        Category("Downloads", Icons.Rounded.Download, Color(0xFFEC4899), "${categoryCounts["Downloads"] ?: 0} Items")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.heightIn(max = 400.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false
    ) {
        items(categories.size) { index ->
            CategoryItem(categories[index]) {
                onCategoryClick(categories[index].title)
            }
        }
    }
}

@Composable
fun CategoryItem(category: Category, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color(0x0D000000))
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(category.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.title,
                tint = category.color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = category.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = category.size,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

