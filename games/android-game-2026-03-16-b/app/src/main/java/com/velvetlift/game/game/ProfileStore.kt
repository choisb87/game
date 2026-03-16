package com.velvetlift.game.game

import android.content.Context

object ProfileStore {
    private const val PREFS_NAME = "velvet_lift_profile"
    private const val KEY_UNLOCKED = "unlocked_contract"
    private const val KEY_HIGH_SCORE = "high_score"
    private const val KEY_LIFETIME_GUESTS = "lifetime_guests"
    private const val KEY_BEST_STARS = "best_stars"
    private const val KEY_BEST_SCORES = "best_scores"

    fun load(context: Context): CareerProfile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return CareerProfile(
            unlockedContractId = prefs.getInt(KEY_UNLOCKED, 1),
            bestStars = decodeMap(prefs.getString(KEY_BEST_STARS, null)),
            bestScores = decodeMap(prefs.getString(KEY_BEST_SCORES, null)),
            highScore = prefs.getInt(KEY_HIGH_SCORE, 0),
            lifetimeGuests = prefs.getInt(KEY_LIFETIME_GUESTS, 0)
        )
    }

    fun save(context: Context, profile: CareerProfile) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_UNLOCKED, profile.unlockedContractId)
            .putInt(KEY_HIGH_SCORE, profile.highScore)
            .putInt(KEY_LIFETIME_GUESTS, profile.lifetimeGuests)
            .putString(KEY_BEST_STARS, encodeMap(profile.bestStars))
            .putString(KEY_BEST_SCORES, encodeMap(profile.bestScores))
            .apply()
    }

    private fun encodeMap(source: Map<Int, Int>): String =
        source.entries
            .sortedBy { it.key }
            .joinToString(separator = ",") { "${it.key}:${it.value}" }

    private fun decodeMap(raw: String?): Map<Int, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(",")
            .mapNotNull { pair ->
                val parts = pair.split(":")
                val key = parts.getOrNull(0)?.toIntOrNull()
                val value = parts.getOrNull(1)?.toIntOrNull()
                if (key == null || value == null) null else key to value
            }
            .toMap()
    }
}
