package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class UserRepository(private val userDao: UserDao) {
    val allLocalUsers: Flow<List<UserEntity>> = userDao.getAllUsers()

    suspend fun getUserByUsername(username: String): UserEntity? {
        return userDao.getUserByUsername(username)
    }

    suspend fun saveUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun seedMockUsersIfEmpty() {
        // If there are no users, we seed classic Roblox accounts to form the competitive "multiplayer" atmosphere!
        // These are retro platform legends with nostalgic high scores.
        val defaultLegends = listOf(
            UserEntity(username = "Erik.Cassel", email = "erik@roblox.com", highScore = 5800, robuxBalance = 25000, ownedCosmetics = listOf("default_hat", "valkyrie_helm", "fedora"), equippedHat = "valkyrie_helm", hasVip = true),
            UserEntity(username = "Telamon", email = "shedletsky@roblox.com", highScore = 4200, robuxBalance = 13370, ownedCosmetics = listOf("default_hat", "fedora", "classic_sword"), equippedHat = "fedora", equippedCoil = "speed_coil", hasVip = true),
            UserEntity(username = "Builderman", email = "builderman@roblox.com", highScore = 3100, robuxBalance = 99999, ownedCosmetics = listOf("default_hat"), equippedHat = "default_hat", hasVip = true),
            UserEntity(username = "clockwork", email = "clockwork@roblox.com", highScore = 2850, robuxBalance = 5000, ownedCosmetics = listOf("default_hat", "gold_crown"), equippedHat = "default_hat"),
            UserEntity(username = "W00t3val", email = "w00t@roblox.com", highScore = 1950, robuxBalance = 800, ownedCosmetics = listOf("default_hat", "classic_sword")),
            UserEntity(username = "NoobPlayer", email = null, highScore = 350, robuxBalance = 50, ownedCosmetics = listOf("default_hat"), equippedHat = "default_hat")
        )
        
        for (legend in defaultLegends) {
            val existing = userDao.getUserByUsername(legend.username)
            if (existing == null) {
                userDao.insertUser(legend)
            }
        }
    }
}
