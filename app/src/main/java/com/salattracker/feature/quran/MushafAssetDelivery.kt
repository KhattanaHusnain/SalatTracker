package com.salattracker.feature.quran

import android.content.Context
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import java.io.File

/** Fonts live in Play's on-demand asset pack so they do not inflate the base APK. */
internal const val MUSHAf_ASSET_PACK = "mushaf_assets"

internal fun requestMushafAssets(context: Context) {
    AssetPackManagerFactory.getInstance(context).fetch(listOf(MUSHAf_ASSET_PACK))
}

internal fun mushafFontFile(context: Context, pageNumber: Int): File? =
    AssetPackManagerFactory.getInstance(context).getPackLocation(MUSHAf_ASSET_PACK)
        ?.assetsPath()
        ?.let { File(it, "quran/mushaf_fonts/v2/p$pageNumber.ttf") }
        ?.takeIf(File::exists)
