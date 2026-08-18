package com.dthv.linkforge.trackingservice.web

import com.dthv.linkforge.trackingservice.repository.LeaderboardEntry
import com.dthv.linkforge.trackingservice.repository.TrackingDataStore
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class LeaderboardController(private val trackingDataStore: TrackingDataStore) {
    @GetMapping("/api/v1/leaderboard")
    fun leaderboard(@RequestParam(defaultValue = "10") limit: Long): List<LeaderboardEntry> =
        trackingDataStore.leaderboard(limit)
}
