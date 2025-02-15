package com.algorand.android.service.credential.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Credential(
    @PrimaryKey val credentialId: String,
    @ColumnInfo(name = "origin") val origin: String,
    @ColumnInfo(name = "userHandle") val userHandle: String,
    @ColumnInfo(name = "userId") val userId: String,
    // FIXME: Not secure storage of keys, this is just for demonstration
    @ColumnInfo(name = "publicKey") val publicKey: String,
    @ColumnInfo(name = "privateKey") val privateKey: String,
    @ColumnInfo(name = "count") val count: Int
)
