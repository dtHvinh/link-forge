package com.dthv.linkforge.trackingservice.repository.impl

import com.dthv.linkforge.trackingservice.repository.LeaderboardEntry
import com.dthv.linkforge.trackingservice.repository.TrackingDataStore
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class RedisTrackingDataStore(val redisTemplate: StringRedisTemplate) : TrackingDataStore {
    override fun hit(code: String) {
        redisTemplate.opsForZSet().incrementScore(LEADERBOARD_KEY, code, 1.0)
    }

    override fun leaderboard(limit: Long): List<LeaderboardEntry> {
        val entries = redisTemplate.opsForZSet().reverseRangeWithScores(LEADERBOARD_KEY, 0, limit - 1)
        return entries.orEmpty().mapNotNull { tuple ->
            val code = tuple.value ?: return@mapNotNull null
            val hits = tuple.score ?: return@mapNotNull null
            LeaderboardEntry(code, hits.toLong())
        }
    }

    companion object {
        private const val LEADERBOARD_KEY = "link:clicks:leaderboard"
    }
}