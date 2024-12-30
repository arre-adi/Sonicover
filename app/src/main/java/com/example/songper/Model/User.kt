// Data Models
data class UserProfile(
    val display_name: String?,
    val id: String
)

data class CurrentlyPlaying(
    val item: Track?,
    val is_playing: Boolean
)

data class Track(
    val name: String,
    val artists: List<Artist>,
    val album: Album
)

data class Artist(
    val name: String
)

data class Album(
    val images: List<Image>
)

data class Image(
    val url: String,
    val height: Int,
    val width: Int
)