package locales

import androidx.compose.runtime.Stable
import utils.extensions.loadResource

/**
 * Region
 * @param isoCode ISO 3166-1 alpha-2 region codes, e.g. JP, EE, DE
 * @param name Anglicized name of the region, e.g. Japan, Estonia, Germany
 */
@Stable
data class Region(val isoCode: RegionIsoCode, val name: String) {
    init {
        when {
            isoCode.value.isBlank() -> throw IllegalArgumentException("Region isoCode cannot be blank")
            isoCode.value.uppercase().filter(Char::isLetter) != isoCode.value ->
                throw IllegalArgumentException("Region isoCode $isoCode should contain uppercase letters only")
            isoCode.value.length == 2 -> Unit // Good
            else -> throw IllegalArgumentException("Invalid Region isoCode: $isoCode. Must comply with ISO 3166-1 alpha-2, e.g. \"US\"")
        }
    }

    companion object {
        operator fun get(isoCode: RegionIsoCode) = Region(isoCode = isoCode, name = names[isoCode] ?: "Unknown")

        /**
         * United States, Argentina, French Guiana, etc from region isoCode
         */
        private val names: Map<RegionIsoCode, String> = loadResource("regions.properties") { k, v -> RegionIsoCode(k) to v }
    }
}

/**
 * ISO 3166-1 alpha-2 Two digit code, e.g. US, AR, GF
 */
@Stable
@JvmInline
value class RegionIsoCode(val value: String)
