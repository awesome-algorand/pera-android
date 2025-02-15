package co.algorand.app.service.key.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyDao {
    @Query("SELECT * FROM secretkey")
    fun getAll(): Flow<List<SecretKey>>
    @Query("SELECT * FROM secretkey")
    fun getAllRegular(): List<SecretKey>

    @Query("SELECT * FROM secretkey WHERE id IN (:ids)")
    fun loadAllByIds(ids: List<String>): List<SecretKey>

    @Query("SELECT * FROM secretkey WHERE id LIKE :id LIMIT 1")
    fun findById(id: String): SecretKey?

    @Query("SELECT * FROM secretkey WHERE type LIKE :secretType LIMIT 1")
    fun findBySecretType(secretType: SecretType): SecretKey?

    @Insert
    suspend fun insertAll(vararg key: SecretKey)

    @Delete
    fun delete(key: SecretKey)

    @Insert
    fun insertAllNoSuspend(vararg key: SecretKey)
}
