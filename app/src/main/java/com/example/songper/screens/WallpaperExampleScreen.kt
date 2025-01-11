package com.example.songper.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.example.songper.viewModel.SpotifyViewModel


@Composable
fun LandingPage(viewModel: SpotifyViewModel) {
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
                // Album Art Placeholder
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
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val previewViewModel = SpotifyViewModel()
    LandingPage(viewModel = previewViewModel)
}