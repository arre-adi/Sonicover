package com.example.songper.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.W900
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.songper.R
import com.example.songper.viewmodel.SpotifyViewModel

@Composable
fun WelcomeScreen(viewModel: SpotifyViewModel) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    Column(

        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.ywhite))
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp , horizontal = 16.dp)
    ) {
        // Header
        Text(
            text = "Hello, ${viewModel.userName}!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.PurplePantone),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Currently Playing Card

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                ){
                    viewModel.albumArtUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Album Artwork",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .padding(start = 16.dp, top = 70.dp)
                ) {

                    Text(
                        text = "${viewModel.nowPlaying}",
                        fontWeight = W900,
                        color = colorResource(id = R.color.PurplePantone),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "artist name",
                        color = colorResource(id = R.color.PurplePantone),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Your Picks Section
        Text(
            text = "your picks",
            color = colorResource(id = R.color.PurplePantone),
            fontWeight = W900,
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
                        .width(120.dp)
                        .height(213.dp)
                        .background(Color(0xFFE0E0E0))
                        .clickable { viewModel.updateWallpaper(item) }

                ){
                    Text(text = item, modifier = Modifier.align(Alignment.Center))
                }
            }
        }

        // New Arrivals Section
        Text(
            text = "new arrivals",
            color = colorResource(id = R.color.PurplePantone),
            fontWeight = W900,
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
                        .width(120.dp)
                        .height(213.dp)
                        .background(Color(0xFFE0E0E0))
                )
            }
        }

        // Trending Section
        Text(
            text = "Trending",
            color = colorResource(id = R.color.PurplePantone),
            fontWeight = W900,
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
                        .width(120.dp)
                        .height(213.dp)
                        .background(Color(0xFFE0E0E0))
                )
            }
        }
    }
}






@Composable@Preview(showBackground = true, showSystemUi = true)
fun HomeScrenPreview() {
    // Create a mock SpotifyViewModel with sample data
    val mockViewModel = remember {
        object : SpotifyViewModel() {
            override var userName: String? = "John Doe"
            override var nowPlaying: String? = "Sample Song"
            override fun initialize(context: android.content.Context) {}
            override fun updateWallpaper(item: String) {}
        }
    }

    // Use the mock view model in the WelcomeScreen composable
    WelcomeScreen(viewModel = mockViewModel)
}

@Composable
fun App(viewModel: SpotifyViewModel) {
    if (viewModel.isLoggedIn) {
        WelcomeScreen(viewModel = viewModel)
    } else {
        LoginScreen(viewModel = viewModel)
    }
}

