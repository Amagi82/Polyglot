package resources

/**
 * Make a resource platform-specific
 */
//@Serializable
data class Platform(val name: String, val iconUrl: String) {
    companion object {
        val Android = Platform(name = "Android", iconUrl = "src/main/resources/android.png")
        val iOS = Platform(name = "iOS", iconUrl = "src/main/resources/apple.png")

        val AndroidOnly = listOf(Android)
        val iosOnly = listOf(iOS)
    }
}
