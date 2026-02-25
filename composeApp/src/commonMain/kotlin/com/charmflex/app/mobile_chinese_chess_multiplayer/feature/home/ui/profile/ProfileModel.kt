package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.home.ui.profile

import kotlinx.serialization.Serializable
import kotlin.math.pow

@Serializable
data class Player(
    val id: String,
    val name: String,
    val xp: Int = 0,
    val level: Int = 1
) {
    companion object {

        fun xpToNextLevel(level: Int): Int {
            return (100 * 1.025.pow(level - 1)).toInt()
        }

        fun cumulativeXpForLevel(level: Int): Int {
            var total = 0
            for (l in 1 until level) {
                total += xpToNextLevel(l)
            }
            return total
        }
    }
}
