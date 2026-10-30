package com.mediax.shared.model

data class DashboardSnapshot(
    val summary: DashboardSummary,
    val recentMedia: List<MediaItem>,
)

data class DashboardSummary(
    val totalItems: Int,
    val publishedItems: Int,
    val totalViews: Long,
)

data class MediaItem(
    val id: String,
    val title: String,
    val kind: MediaKind,
    val status: MediaStatus,
    val viewCount: Long,
    val publishedAtEpochSeconds: Long,
)

enum class MediaKind {
    VIDEO,
    AUDIO,
    IMAGE,
}

enum class MediaStatus {
    PUBLISHED,
    PROCESSING,
    DRAFT,
}
