package com.algorand.android.service.credential

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.provider.CallingAppInfo
import com.algorand.android.service.credential.db.Credential
import com.algorand.android.service.credential.db.CredentialDatabase
import com.algorand.android.service.key.KeyRepository
import foundation.algorand.deterministicP256.DeterministicP256
import java.security.KeyFactory
import java.security.KeyPair
import java.security.MessageDigest
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

interface CredentialRepository {
    var db: CredentialDatabase
    suspend fun saveCredential(context: Context, credential: Credential)
    fun getDatabase(context: Context): CredentialDatabase
    fun generateCredentialId(keyPair: KeyPair): ByteArray
    fun getKeyPair(context: Context, credentialId: ByteArray): KeyPair?
    fun createDeterministicKeyPair(context: Context, origin: String, userHandle: String): KeyPair
    fun appInfoToOrigin(info: CallingAppInfo): String
    fun getCredential(context: Context, credentialId: ByteArray): Credential?
    fun getCredentialByOrigin(context: Context, origin: String): Credential?
    fun sign(keyPair: KeyPair, payload: ByteArray): ByteArray
}

fun CredentialRepository(): CredentialRepository = Repository()

class Repository : CredentialRepository {
    private val keys = KeyRepository()
    private var dP256: DeterministicP256 = DeterministicP256()
    override lateinit var db: CredentialDatabase

    companion object {
        const val TAG = "CredentialRepository"
    }

    override suspend fun saveCredential(context: Context, credential: Credential) {
        Log.d(TAG, "saveCredential($credential)")
        getDatabase(context)
        db.credentialDao().insertAll(credential)
    }

    override fun getDatabase(context: Context): CredentialDatabase {
        Log.d(TAG, "getDatabase($context)")
        if (!::db.isInitialized) {
            db = CredentialDatabase.getInstance(context)
        }
        return db
    }

    // CredentialId is deterministically generated from the public key
    // Taking SHA-256 hash of the public key, giving us a 32-byte credentialId
    override fun generateCredentialId(keyPair: KeyPair): ByteArray {
        Log.d(TAG, "generateCredentialId()")

        // Get the public key bytes
        val publicKeyBytes = keyPair.public.encoded

        // Compute SHA-256 hash of the public key
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val credentialId = messageDigest.digest(publicKeyBytes)

        return credentialId
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun getCredential(context: Context, credentialId: ByteArray): Credential? {
        getDatabase(context)
        return db.credentialDao().findById(Base64.encode(credentialId))
    }

    /**
     * Retrieves a credential from the database based on the specified origin.
     *
     * @param context The context used to access the database.
     * @param origin The origin string used to query the credential.
     * @return The matching Credential object, or null if no credential is found for the given origin.
     */
    override fun getCredentialByOrigin(context: Context, origin: String): Credential? {
        getDatabase(context)
        return db.credentialDao().findByOrigin(origin)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun getKeyPairFromDatabase(context: Context, credentialId: ByteArray): KeyPair? {
        Log.d(TAG, "getKeyPairFromDatabase()")
        getDatabase(context)
        val credential = db.credentialDao().findById(Base64.encode(credentialId))
        if (credential != null) {
            val publicKeyBytes = Base64.decode(credential.publicKey)
            val privateKeyBytes = Base64.decode(credential.privateKey)
            val factory = KeyFactory.getInstance("EC")
            val publicKey = factory.generatePublic(X509EncodedKeySpec(publicKeyBytes))
            val privateKey = factory.generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
            return KeyPair(publicKey, privateKey)
        }
        return null
    }

    override fun getKeyPair(context: Context, credentialId: ByteArray): KeyPair? {
        Log.d(TAG, "getKeyPair($context, $credentialId)")
        return getKeyPairFromDatabase(context, credentialId)
    }

    override fun createDeterministicKeyPair(
        context: Context,
        origin: String,
        userHandle: String
    ): KeyPair {
        // Note that we take the LOWERCASE of the userHandle, to prevent confusion
        Log.d(TAG, "createDeterministicKeyPair($context, , $origin, ${userHandle.lowercase()})")

        val derivedParentSecret =
            keys.getDerivedParentSecret(context)?.derivedSecret!!.toByteArray()
        return dP256.genDomainSpecificKeypair(derivedParentSecret, origin, userHandle.lowercase())
    }

    override fun sign(keyPair: KeyPair, payload: ByteArray): ByteArray {
        return dP256.signWithDomainSpecificKeyPair(keyPair, payload)
    }

    @OptIn(ExperimentalEncodingApi::class)
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun appInfoToOrigin(info: CallingAppInfo): String {
        val cert = info.signingInfo.apkContentsSigners[0].toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val certHash = md.digest(cert)
        // This is the format for origin
        return "android:apk-key-hash:${Base64.encode(certHash)}"
    }
}
