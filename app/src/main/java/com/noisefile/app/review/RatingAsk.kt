package com.noisefile.app.review

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Asks for stars with Google Play's own rating card, right after a case is marked filed:
 * the moment the job is done. Google decides whether the card actually shows (it caps how
 * often), and on top of that a person is asked at most once every [GAP_MS].
 *
 * The card is drawn by the Play Store app, the same way the purchase sheet is, so NoiseFile
 * still has no internet permission. On a copy not installed from Google Play the card never
 * shows and nothing happens.
 */
object RatingAsk {
    const val GAP_MS: Long = 120L * 24 * 60 * 60 * 1000
    private const val PREFS = "rating_ask"
    private const val KEY_LAST_ASKED = "last_asked_ms"
    private const val TAG = "NoiseFileRating"

    /** True the first time, then again only after [GAP_MS]. */
    fun shouldAsk(lastAskedMs: Long, nowMs: Long): Boolean =
        lastAskedMs <= 0L || nowMs - lastAskedMs >= GAP_MS

    fun afterFiled(activity: Activity, nowMs: Long = System.currentTimeMillis()) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!shouldAsk(prefs.getLong(KEY_LAST_ASKED, 0L), nowMs)) {
            Log.i(TAG, "rating card skipped: asked within the last 120 days")
            return
        }
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (!request.isSuccessful) {
                Log.i(TAG, "rating card not available: ${request.exception?.message}")
                return@addOnCompleteListener
            }
            prefs.edit().putLong(KEY_LAST_ASKED, nowMs).apply()
            if (activity.isFinishing || activity.isDestroyed) return@addOnCompleteListener
            Log.i(TAG, "rating card requested")
            manager.launchReviewFlow(activity, request.result).addOnCompleteListener {
                Log.i(TAG, "rating card flow finished")
            }
        }
    }
}
