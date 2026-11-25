package com.example.ads

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

private const val BANNER_TAG = "KenariBanner"

/**
 * Responsive anchored adaptive banner. The view is destroyed when the composable
 * leaves composition so ads are not retained across tab changes.
 */
@Composable
fun KenariAdaptiveBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adUnitId = remember(context) { context.getString(com.example.R.string.admob_banner_unit_id) }

    if (adUnitId.isBlank()) return

    Box(modifier = modifier.fillMaxWidth().wrapContentHeight()) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            factory = { ctx ->
                AdView(ctx).apply {
                    adUnitId = adUnitId
                    val displayMetrics = ctx.resources.displayMetrics
                    val widthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt()
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp))
                    loadAd(AdRequest.Builder().build())
                }
            },
            update = { view ->
                if (view.adUnitId != adUnitId) view.adUnitId = adUnitId
            }
        )
    }
}
