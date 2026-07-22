package com.omnilabs.omfiles.ui.screens.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omnilabs.omfiles.preview.PreviewType
import com.omnilabs.omfiles.preview.PreviewUiState
import com.omnilabs.omfiles.preview.PreviewViewModel
import com.omnilabs.omfiles.utils.FileUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    path: String,
    onBack: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(path) {
        viewModel.load(path)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (val state = uiState) {
                        is PreviewUiState.Ready -> Text(
                            text = state.fileName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        else -> Text("Preview")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        try {
                            val intent = FileUtils.openFile(context, File(path))
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            }
                        } catch (_: Exception) { }
                    }) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = "Open externally")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is PreviewUiState.Loading -> LoadingView()
                is PreviewUiState.Error -> ErrorView((state as PreviewUiState.Error).message)
                is PreviewUiState.Ready -> {
                    val readyState = state as PreviewUiState.Ready
                    PreviewContent(
                        path = readyState.path,
                        previewType = readyState.previewType,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewContent(
    path: String,
    previewType: PreviewType,
    viewModel: PreviewViewModel
) {
    when (previewType) {
        PreviewType.IMAGE -> ImagePreviewContent(path = path)
        PreviewType.TEXT, PreviewType.CODE -> TextPreviewContent(viewModel = viewModel)
        PreviewType.ARCHIVE -> ArchivePreviewContent(viewModel = viewModel)
        PreviewType.APK -> ApkPreviewContent(viewModel = viewModel)
        PreviewType.VIDEO -> VideoPreviewContent(path = path)
        PreviewType.AUDIO -> AudioPreviewContent(path = path)
        PreviewType.PDF -> PdfPreviewContent(path = path)
        PreviewType.UNKNOWN -> UnknownPreviewContent(path = path, viewModel = viewModel)
        else -> UnknownPreviewContent(path = path, viewModel = viewModel)
    }
}
