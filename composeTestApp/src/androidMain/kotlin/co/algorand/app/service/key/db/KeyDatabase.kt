package co.algorand.app.service.key.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [SecretKey::class], version = 1)
@TypeConverters(Converters::class)
abstract class KeyDatabase : RoomDatabase() {
    abstract fun derivedSecretDao(): KeyDao

    companion object {
        const val TAG = "KeyDatabase"
        private var INSTANCE: KeyDatabase? = null
        fun getInstance(context: Context): KeyDatabase {
            Log.d(TAG, "getInstance($context)")
            if (INSTANCE == null) {
                INSTANCE =
                    Room.databaseBuilder(
                        context,
                        KeyDatabase::class.java,
                        "key"
                    )
                        .allowMainThreadQueries()
                        .build()
            }

            return INSTANCE!!
        }
    }
}
