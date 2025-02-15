package com.algorand.android.service.key.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyDao {
    @Query("SELECT * FROM keyentity")
    fun getAll(): Flow<List<KeyEntity>>

    @Query("SELECT * FROM keyentity")
    fun getAllRegular(): List<KeyEntity>

    @Query("SELECT * FROM keyentity WHERE id IN (:ids)")
    fun loadAllByIds(ids: List<String>): List<KeyEntity>

    @Query("SELECT * FROM keyentity WHERE id LIKE :id LIMIT 1")
    fun findById(id: String): KeyEntity?

    @Query("SELECT * FROM keyentity WHERE type LIKE :secretType LIMIT 1")
    fun findBySecretType(secretType: SecretType): KeyEntity?

    @Insert
    suspend fun insertAll(vararg key: KeyEntity)

    @Delete
    fun delete(key: KeyEntity)

    @Insert
    fun insertAllNoSuspend(vararg key: KeyEntity)
}
