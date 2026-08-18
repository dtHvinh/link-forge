package com.dthv.linkforge.trackingservice.repository

data class LeaderboardEntry(val code: String, val hits: Long)

interface TrackingDataStore {
    fun hit(code: String)
    fun leaderboard(limit: Long): List<LeaderboardEntry>
}