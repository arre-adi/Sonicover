package com.example.songper.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.songper.viewModel.SpotifyViewModel

@Composable
fun WelcomeScreen(viewModel: SpotifyViewModel) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Hello, ${viewModel.userName}!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Currently Playing Card

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                ){
                    viewModel.albumArtUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Album Artwork",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = "Currently Playing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${viewModel.currentlyPlaying}",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "artist name",
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "platform",
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Your Picks Section
        Text(
            text = "Your picks",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val items = listOf("A", "B", "C")

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            items(items) {item->
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(180.dp)
                        .background(Color(0xFFE0E0E0))
                        .clickable { viewModel.updateWallpaper(item) }

                ){
                    Text(text = item, modifier = Modifier.align(Alignment.Center))
                }
            }
        }

        // New Arrivals Section
        Text(
            text = "New arrivals",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            items(3) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(180.dp)
                        .background(Color(0xFFE0E0E0))
                )
            }
        }

        // Trending Section
        Text(
            text = "Trending",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(3) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(180.dp)
                        .background(Color(0xFFE0E0E0))
                )
            }
        }
    }




//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text(
//            text = "Hello, ${viewModel.userName}!",
//            fontSize = 24.sp,
//            textAlign = TextAlign.Center
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Album Artwork
//        viewModel.albumArtUrl?.let { url ->
//            AsyncImage(
//                model = url,
//                contentDescription = "Album Artwork",
//                modifier = Modifier
//                    .size(200.dp)
//                    .clip(RoundedCornerShape(8.dp)),
//                contentScale = ContentScale.Crop
//            )
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//        Text(
//            text = "Currently Playing:\n${viewModel.currentlyPlaying}",
//            fontSize = 18.sp,
//            textAlign = TextAlign.Center
//        )
//        Spacer(modifier = Modifier.height(32.dp))
//        Button(onClick = { viewModel.onLogout() }) {
//            Text("Logout")
//        }
//
//
//        Row(
//            horizontalArrangement = Arrangement.SpaceEvenly,
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(100.dp)
//                    .background(Color.Gray)
//                    .clickable { viewModel.updateWallpaper("A") }
//            ) { Text("A") }
//
//            Box(
//                modifier = Modifier
//                    .size(100.dp)
//                    .background(Color.Gray)
//                    .clickable { viewModel.updateWallpaper("B") }
//            ) { Text("B") }
//
//            Box(
//                modifier = Modifier
//                    .size(100.dp)
//                    .background(Color.Gray)
//                    .clickable { viewModel.updateWallpaper("C") }
//            ) { Text("C") }
//        }
//    }
}



@Composable
fun App(viewModel: SpotifyViewModel) {
    if (viewModel.isLoggedIn) {
        WelcomeScreen(viewModel = viewModel)
    } else {
        LoginScreen(viewModel = viewModel)
    }
}

