package com.algorand.android.service.key.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class SecretType {
    PASSKEY,
    SPENDKEY
}

class Converters {
    @TypeConverter
    fun fromSecretType(value: SecretType): String {
        return value.name
    }

    @TypeConverter
    fun toSecretType(value: String): SecretType {
        return SecretType.valueOf(value)
    }
}

@Entity
data class KeyEntity(
    @PrimaryKey val id: String,
    // FIXME: Not secure storage of keys, this is just for demonstration
    @ColumnInfo(name = "derivedSecret") val derivedSecret: String,
    @ColumnInfo(name = "mnemonic") val mnemonic: String,
    @ColumnInfo(name = "type") val type: SecretType
)
