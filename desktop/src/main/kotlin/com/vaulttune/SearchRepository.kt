package com.vaulttune

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.SongItem

/**
 * Hides the YouTube/InnerTube backend behind a small, testable surface.
 */
class SearchRepository {
    suspend fun searchSongs(query: String): List<SongItem> =
        withContext(Dispatchers.IO) {
            YouTube
                .search(query, YouTube.SearchFilter.FILTER_SONG, useAccountContext = false)
                .getOrThrow()
                .items
                .filterIsInstance<SongItem>()
        }
}
