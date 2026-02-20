package com.charmflex.xiangqi.server.repository

import com.charmflex.xiangqi.server.model.PlayerEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PlayerRepository : JpaRepository<PlayerEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PlayerEntity p WHERE p.id = :id")
    fun findByIdForUpdate(id: String): Optional<PlayerEntity>
}
