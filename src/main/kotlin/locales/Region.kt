package locales

import androidx.compose.runtime.Stable
import utils.extensions.loadResource

/**
 * Two digit code, e.g. US, AR, GF
 * Android: values.en_US, values.es_AR, values.fr_GF, etc
 * iOS: en_US.lproj, es_AR.lproj, fr_GF.lproj, etc
 */
//@JvmInline
//value class RegionIsoCode(val value: String)
//
///**
// * locales.Region
// */
//@Stable
//data class Region(val isoCode: RegionIsoCode, val name: String) {
//    companion object {
////        operator fun get(isoCode: RegionIsoCode) = Region(isoCode = isoCode, name = all[isoCode] ?: "Unknown")
//
//        /**
//         * United States, Argentina, French Guiana, etc from region isoCode
//         */
////        private val all = loadResource<Map<RegionIsoCode, String>>("regions.json")
//    }
//}
