package com.jacobarau.minstrel.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jacobarau.minstrel.ui.theme.MinstrelTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinstrelSearchBar(
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text("Minstrel") },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun MinstrelSearchBarPreview() {
    MinstrelTheme {
        MinstrelSearchBar()
    }
}
