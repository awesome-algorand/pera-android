package com.algorand.android.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.algorand.android.service.credential.CredentialRepository
import com.algorand.android.service.credential.db.Credential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class ExampleCredentialProviderService : CredentialProviderService() {
    private val credentialRepository = CredentialRepository()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        const val TAG = "ExampleCredentialProviderService"

        // TODO: App Lock Intents
        const val GET_PASSKEY_INTENT = 200
        const val CREATE_PASSKEY_INTENT = 300
        const val GET_PASSKEY_ACTION = "com.algorand.android.service.GET_PASSKEY"
        const val CREATE_PASSKEY_ACTION = "com.algorand.android.service.CREATE_PASSKEY"
    }

    /**
     * Handles the initialization of a create credential request operation.
     *
     * @param request The request object of type `BeginCreateCredentialRequest`
     * containing the details required for the Create Credential operation.
     * @param cancellationSignal A signal to allow canceling the operation if needed.
     * @param callback The callback to return either the result as a `BeginCreateCredentialResponse`
     * or an error as `CreateCredentialException`.
     */
    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>
    ) {
        val response: BeginCreateCredentialResponse? = processCreateCredentialRequest(request)
        if (response != null) {
            callback.onResult(response)
        } else {
            callback.onError(CreateCredentialUnknownException())
        }
    }

    /**
     * Process incoming Create Credential Requests
     */
    private fun processCreateCredentialRequest(request: BeginCreateCredentialRequest): BeginCreateCredentialResponse? {
        when (request) {
            is BeginCreatePublicKeyCredentialRequest -> {
                return handleCreatePasskeyQuery(request)
            }
        }
        return null
    }

    /**
     * Handles the creation of a passkey query based on the incoming `BeginCreatePublicKeyCredentialRequest`.
     *
     * @param request The request object of type `BeginCreatePublicKeyCredentialRequest` containing
     * the details required for creating the passkey.
     * @return An instance of `BeginCreateCredentialResponse` containing a list of created credential entries.
     */
    private fun handleCreatePasskeyQuery(
        request: BeginCreatePublicKeyCredentialRequest
    ): BeginCreateCredentialResponse {
        Log.d(TAG, request.requestJson)
        val createEntries: MutableList<CreateEntry> = mutableListOf()
        val name = JSONObject(request.requestJson).getJSONObject("user").get("name").toString()

        Log.d(TAG, "name: $name")
        createEntries.add(
            CreateEntry(
                name,
                createNewPendingIntent(CREATE_PASSKEY_ACTION, CREATE_PASSKEY_INTENT, null)
            )
        )
        return BeginCreateCredentialResponse(createEntries)
    }

    /**
     * Handles the initialization of a get credential request.
     *
     * @param request The request object of type `BeginGetCredentialRequest` containing
     * the details of the Get Credential operation.
     * @param cancellationSignal A signal to allow canceling the operation if needed.
     * @param callback The callback to return either the result as a `BeginGetCredentialResponse`
     * or an error as `GetCredentialException`.
     */
    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        try {
            callback.onResult(processGetCredentialRequest(request))
        } catch (e: GetCredentialException) {
            callback.onError(GetCredentialUnknownException())
        }
    }

    /**
     * Processes a Get Credential Request and generates a response containing credential entries.
     *
     * @param request The request object of type `BeginGetCredentialRequest` containing the details
     * of the Get Credential operation.
     * @return An instance of `BeginGetCredentialResponse` that includes a list of credential
     * entries based on the request.
     */
    private fun processGetCredentialRequest(request: BeginGetCredentialRequest): BeginGetCredentialResponse {
        Log.v(TAG, "processing GetCredentialRequest")

        // Lookup Credentials from the Database
        val deferredCredentials: Deferred<List<Credential>> = scope.async {
            credentialRepository.getDatabase(this@ExampleCredentialProviderService).credentialDao()
                .getAllRegular()
        }
        val credentials = runBlocking {
            deferredCredentials.await()
        }
        return BeginGetCredentialResponse(credentials.map {
            val data = Bundle()
            data.putString("credentialId", it.credentialId)
            data.putString("userHandle", it.userHandle)
            PublicKeyCredentialEntry.Builder(
                this@ExampleCredentialProviderService,
                it.userHandle,
                createNewPendingIntent(GET_PASSKEY_ACTION, GET_PASSKEY_INTENT, data),
                // TODO: filter the request for PublicKeyCredentialOptions
                request.beginGetCredentialOptions[0] as BeginGetPublicKeyCredentialOption
            ).build()
        })
    }

    /**
     * Handles the request to clear credential state in the credential provider service. This is used to
     * remove or reset any state associated with a given credential.
     *
     * @param request The request object of type `ProviderClearCredentialStateRequest` containing
     *                the details of the clear operation.
     * @param cancellationSignal A signal to allow canceling the operation if needed.
     * @param callback The callback to return the result of the operation, either as a success or
     *                 a `ClearCredentialException` in case of an error.
     */
    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>
    ) {
        Log.d(TAG, "onClearCredentialStateRequest")
        TODO("Not yet implemented")
    }

    /**
     * Creates a new PendingIntent configured with the provided action, requestCode, and optional extras.
     *
     * @param action The action string to be used for the Intent.
     * @param requestCode The request code for the PendingIntent, which allows distinguishing between multiple intents.
     * @param extra An optional Bundle containing additional data to be passed with the Intent.
     * @return A PendingIntent instance configured to start an activity with the specified details.
     */
    private fun createNewPendingIntent(
        action: String,
        requestCode: Int,
        extra: Bundle?
    ): PendingIntent {
        val intent = Intent(action).setPackage("com.algorand.android")
        if (extra != null) {
            intent.putExtra("CREDENTIAL_DATA", extra)
        }
        return PendingIntent.getActivity(
            applicationContext, requestCode,
            intent, (PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        )
    }

    /**
     * Called when the service is being destroyed. This lifecycle method is typically used to perform
     * cleanup operations such as releasing resources or canceling ongoing tasks.
     *
     * This implementation cancels any associated coroutine jobs by invoking `job.cancel()`.
     * Ensure that any resources tied to the lifecycle of this service are properly released here.
     */
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
