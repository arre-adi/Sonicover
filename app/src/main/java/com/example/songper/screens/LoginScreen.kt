package com.example.songper.screens


import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    var showUnsupportedDialog by remember { mutableStateOf(false) }
    var unsupportedServiceName by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val response = AuthorizationClient.getResponse(result.resultCode, result.data)
            viewModel.handleLoginResult(context, response)
        }
    }

    fun handleSpotifyLogin() {
        val builder = AuthorizationRequest.Builder(
            CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            REDIRECT_URI
        ).apply {
            setScopes(arrayOf("user-read-currently-playing", "user-read-private"))
            setShowDialog(true) // Force show dialog to ensure fresh login
        }

        val request = builder.build()
        val intent = AuthorizationClient.createLoginActivityIntent(
            context as Activity,
            request
        )
        launcher.launch(intent)
    }

    fun showUnsupportedService(serviceName: String) {
        unsupportedServiceName = serviceName
        showUnsupportedDialog = true
    }

    // Main layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose your music service",
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { handleSpotifyLogin() },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text("Spotify")
            }

            Button(
                onClick = { showUnsupportedService("JioSaavn") },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text("JioSaavn")
            }

            Button(
                onClick = { showUnsupportedService("YouTube Music") },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text("YT Music")
            }
        }
    }

    // Unsupported service dialog
    if (showUnsupportedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsupportedDialog = false },
            title = { Text("Service Not Available") },
            text = { Text("$unsupportedServiceName support is coming soon!") },
            confirmButton = {
                TextButton(onClick = { showUnsupportedDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewLoginScreen() {
    LoginScreen(viewModel = SpotifyViewModel())
}