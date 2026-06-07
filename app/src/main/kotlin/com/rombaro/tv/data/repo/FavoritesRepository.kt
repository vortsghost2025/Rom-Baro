package com.rombaro.tv.data.repo

import com.rombaro.tv.data.db.FavoriteDao
import com.rombaro.tv.data.db.FavoriteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val dao: FavoriteDao,
) {
    suspend fun toggle(playlistId: Long, streamId: String): Boolean {
        val isFav = dao.isFavorite(playlistId, streamId)
        if (isFav) dao.remove(playlistId, streamId)
        else dao.add(FavoriteEntity(playlistId, streamId))
        return !isFav
    }

    suspend fun isFavorite(p: Long, s: String) = dao.isFavorite(p, s)

    fun observe(playlistId: Long): Flow<List<FavoriteEntity>> = dao.observe(playlistId)
}
