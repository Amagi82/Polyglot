import kotlinx.serialization.Serializable

/**
 * Make a resource platform-specific
 */
@Serializable
data class Platform(val name: String) {
    companion object {
        val Android = Platform("Android")
        val iOS = Platform("iOS")

        val AndroidOnly = listOf(Android)
        val iosOnly = listOf(iOS)
    }
}
