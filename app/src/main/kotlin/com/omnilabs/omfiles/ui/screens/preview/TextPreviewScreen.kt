package com.omnilabs.omfiles.ui.screens.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnilabs.omfiles.preview.PreviewViewModel

@Composable
fun TextPreviewContent(viewModel: PreviewViewModel) {
    val content by viewModel.textContent.collectAsState()
    val scrollState = rememberScrollState()

    Text(
        text = content ?: "Loading text…",
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp
        ),
        color = MaterialTheme.colorScheme.onSurface
    )
}
