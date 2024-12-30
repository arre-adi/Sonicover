package com.example.songper.screens


import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.example.songper.viewModel.SpotifyViewModel
import com.example.songper.BuildConfig

private const val CLIENT_ID = BuildConfig.SPOTIFY_CLIENT_ID // Replace with your Spotify Client ID
private const val REDIRECT_URI = "songper://callback" // Replace with your Redirect URI
private const val REQUEST_CODE = 1337

@Composable
fun LoginScreen(viewModel: SpotifyViewModel) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val response = AuthorizationClient.getResponse(result.resultCode, result.data)
            viewModel.handleLoginResult(response)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                val builder = AuthorizationRequest.Builder(
                    CLIENT_ID,
                    AuthorizationResponse.Type.TOKEN,
                    REDIRECT_URI
                )
                builder.setScopes(arrayOf("user-read-currently-playing", "user-read-private"))
                val request = builder.build()

                val intent = AuthorizationClient.createLoginActivityIntent(
                    context as Activity,
                    request
                )
                launcher.launch(intent)
            }
        ) {
            Text("Login with Spotify")
        }
    }
}