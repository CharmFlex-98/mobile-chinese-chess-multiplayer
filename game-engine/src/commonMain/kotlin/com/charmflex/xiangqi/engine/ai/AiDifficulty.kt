package com.charmflex.xiangqi.engine.ai

import kotlinx.serialization.Serializable

@Serializable
enum class AiDifficulty(
    val label: String,
    val depth: Int,
    val noiseRange: Int,
    val timeLimitMs: Long = 0,
    val minDepth: Int = depth
) {
    BEGINNER("Beginner", 1, 200),
    EASY("Easy", 2, 100),
    MEDIUM("Medium", 3, 50),
    INTERMEDIATE("Intermediate", 4, 20),
    HARD("Hard", 5, 0),
    EXPERT("Expert", 6, 0),
    MASTER("Master", 7, 0),
    GRANDMASTER("Grandmaster", 8, 0),
    LEGEND("Legend", 20, 0, timeLimitMs = 5_000, minDepth = 8),
    IMMORTAL("Immortal", 20, 0, timeLimitMs = 10_000, minDepth = 8);
}
