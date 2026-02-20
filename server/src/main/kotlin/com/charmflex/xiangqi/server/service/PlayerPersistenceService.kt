package com.charmflex.xiangqi.server.service

import com.charmflex.xiangqi.server.model.Player
import com.charmflex.xiangqi.server.model.PlayerEntity
import com.charmflex.xiangqi.server.repository.PlayerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Service
class PlayerPersistenceService(private val playerRepository: PlayerRepository) {

    @Transactional
    fun persistNewPlayer(id: String, name: String) {
        playerRepository.save(PlayerEntity(id = id, name = name, xp = 0, level = 1))
    }

    @Transactional
    fun persistXpGain(playerId: String, xpGain: Int) : Player? {
        val entity = playerRepository.findByIdForUpdate(playerId).getOrNull() ?: return null
        val newXp = entity.xp + xpGain
        val newLevel = Player.computeLevel(newXp)
        val newEntity = entity.copy(xp = newXp, level = newLevel)
        return playerRepository.save(newEntity).toPlayer()
    }

    /**
     * Get from database. If found no existing player, create one and save into database.
     */
    fun getOrCreatePlayer(userId: String, name: String): Player {
        // 2. Try loading from DB (authenticated players persist across restarts)
        val fromDb = findById(userId)
        if (fromDb != null) {
            val player = fromDb.toPlayer()
            return player
        }

        // 3. Create new player, persist to DB
        val player = Player(id = userId, name = name)
        persistNewPlayer(userId, name)
        return player
    }

    fun findById(id: String): PlayerEntity? = playerRepository.findById(id).orElse(null)
}
