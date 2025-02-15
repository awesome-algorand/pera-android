package com.algorand.android.service.key

import android.content.Context
import android.security.keystore.KeyProperties
import android.util.Log
import com.algorand.android.service.key.db.KeyEntity
import com.algorand.android.service.key.db.KeyDatabase
import com.algorand.android.service.key.db.SecretType
import foundation.algorand.deterministicP256.DeterministicP256
import java.security.KeyPairGenerator
import java.security.KeyStore

// import java.security.interfaces.ECPrivateKey

interface KeyRepository {
    val keyStore: KeyStore
    var db: KeyDatabase
    fun saveDerivedParentSecret(context: Context, mnemonic: CharArray)
    fun getDatabase(context: Context): KeyDatabase
    fun getDerivedParentSecret(context: Context): KeyEntity?
}

fun KeyRepository(): KeyRepository = Repository()

class Repository : KeyRepository {
    override var keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
    private var generator: KeyPairGenerator =
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)
    private var dP256: DeterministicP256 = DeterministicP256()
    override lateinit var db: KeyDatabase

    init {
        keyStore.load(null)
    }

    companion object {
        const val TAG = "DerivedSecretRepository"
    }

    override fun saveDerivedParentSecret(context: Context, mnemonic: CharArray) {
        Log.d(TAG, "saveDerivedParentSecret([mnemonic kept hidden])")
        getDatabase(context)

        getDerivedParentSecret(context)?.let { db.derivedSecretDao().delete(it) }

        db.derivedSecretDao()
            .insertAllNoSuspend(
                KeyEntity(
                    id = "derivedParentSecret",
                    derivedSecret =
                    dP256.genDerivedMainKeyWithBIP39(mnemonic.concatToString())
                        .contentToString(),
                    type = SecretType.PASSKEY,
                    mnemonic = mnemonic.concatToString() // We could choose to not store the mnemonic
                )
            )
    }

    override fun getDatabase(context: Context): KeyDatabase {
        Log.d(TAG, "getDatabase($context)")
        if (!::db.isInitialized) {
            db = KeyDatabase.getInstance(context)
        }
        return db
    }

    override fun getDerivedParentSecret(context: Context): KeyEntity? {
        getDatabase(context)
        return db.derivedSecretDao().findById("derivedParentSecret")
    }
}
