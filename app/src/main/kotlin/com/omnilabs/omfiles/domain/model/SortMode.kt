package com.omnilabs.omfiles.domain.model

enum class SortMode(val displayName: String) {
    NAME("Name"),
    DATE("Date"),
    SIZE("Size"),
    EXTENSION("Extension")
}

enum class SortOrder(val displayName: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending")
}

data class FileSortOptions(
    val mode: SortMode = SortMode.NAME,
    val order: SortOrder = SortOrder.ASCENDING,
    val foldersFirst: Boolean = true
)
