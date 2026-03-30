package com.jacobarau.minstrel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jacobarau.minstrel.data.SortDimension

@Composable
fun SortingMenu(
    currentSortDimension: SortDimension?,
    onSortDimensionChanged: (SortDimension?) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { isMenuExpanded = true }
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Sort options"
            )
        }

        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false }
        ) {
            Text(
                text = "Sort by",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider()

            DropdownMenuItem(
                text = { Text("None (Default)") },
                onClick = {
                    onSortDimensionChanged(null)
                    isMenuExpanded = false
                }
            )

            DropdownMenuItem(
                text = { Text(SortDimension.Folder.getDisplayName()) },
                onClick = {
                    onSortDimensionChanged(SortDimension.Folder)
                    isMenuExpanded = false
                }
            )

            DropdownMenuItem(
                text = { Text(SortDimension.Artist.getDisplayName()) },
                onClick = {
                    onSortDimensionChanged(SortDimension.Artist)
                    isMenuExpanded = false
                }
            )

            DropdownMenuItem(
                text = { Text(SortDimension.Album.getDisplayName()) },
                onClick = {
                    onSortDimensionChanged(SortDimension.Album)
                    isMenuExpanded = false
                }
            )

            DropdownMenuItem(
                text = { Text(SortDimension.Genre.getDisplayName()) },
                onClick = {
                    onSortDimensionChanged(SortDimension.Genre)
                    isMenuExpanded = false
                }
            )
        }
    }
}
