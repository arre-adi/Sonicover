package com.example.songper.ViewModel

import User
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.auth0.android.Auth0
import com.auth0.android.callback.Callback
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import java.util.Properties

class MainViewModel : ViewModel() {
    var appJustLaunched by mutableStateOf(true)
    var userIsAuthenticated by mutableStateOf(false)
    private val TAG = "MainViewModel"
    private var _account: Auth0? = null
    private var _context: Context? = null
    var user by mutableStateOf(User())

    // Add error handling state
    var errorMessage by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)

    fun setContext(activityContext: Context) {
        _context = activityContext
        try {
            val properties = loadPropertiesFromRawResource(activityContext)
            val clientId = properties.getProperty("com.auth0.client_id")
                ?: throw IllegalStateException("Missing client_id in config")
            val domain = properties.getProperty("com.auth0.domain")
                ?: throw IllegalStateException("Missing domain in config")
            _account = Auth0.getInstance(clientId, domain)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Auth0: $e")
            errorMessage = "Failed to initialize login. Please try again."
        }
    }

    private fun loadPropertiesFromRawResource(context: Context): Properties {
        val properties = Properties()
        context.resources.openRawResource(
            context.resources.getIdentifier("config", "raw", context.packageName)
        ).use { inputStream ->
            properties.load(inputStream)
        }
        return properties
    }

    fun login() {
        val currentContext = _context ?: run {
            Log.e(TAG, "Context is null")
            errorMessage = "Application error. Please restart the app."
            return
        }

        val currentAccount = _account ?: run {
            Log.e(TAG, "Auth0 account is null")
            errorMessage = "Authentication not initialized. Please restart the app."
            return
        }

        isLoading = true
        errorMessage = null

        try {
            val scheme = loadPropertiesFromRawResource(currentContext)
                .getProperty("com.auth0.scheme")
                ?: throw IllegalStateException("Missing scheme in config")

            WebAuthProvider
                .login(currentAccount)
                .withScheme(scheme)
                .withConnection("spotify")
                .withScope("openid profile email user-read-private")
                .withParameters(mapOf(
                    "prompt" to "login"  // Force login prompt
                ))
                .start(currentContext, object : Callback<Credentials, AuthenticationException> {
                    override fun onFailure(error: AuthenticationException) {
                        Log.e(TAG, "Login failed: $error")
                        errorMessage = "Login failed: ${error.getDescription()}"
                        isLoading = false
                    }

                    override fun onSuccess(result: Credentials) {
                        user = User(result.idToken)
                        userIsAuthenticated = true
                        appJustLaunched = false
                        isLoading = false
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error during login: $e")
            errorMessage = "Login error. Please try again."
            isLoading = false
        }
    }

    fun logout() {
        val currentContext = _context ?: return
        val currentAccount = _account ?: return

        try {
            val scheme = loadPropertiesFromRawResource(currentContext).getProperty("com.auth0.scheme")
            WebAuthProvider
                .logout(currentAccount)
                .withScheme(scheme)
                .start(currentContext, object : Callback<Void?, AuthenticationException> {
                    override fun onFailure(error: AuthenticationException) {
                        Log.e(TAG, "Logout failed: $error")
                        errorMessage = "Logout failed. Please try again."
                    }

                    override fun onSuccess(result: Void?) {
                        user = User()
                        userIsAuthenticated = false
                        errorMessage = null
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout: $e")
            errorMessage = "Logout error. Please try again."
        }
    }
}