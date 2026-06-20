package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val username: String,
    val email: String? = null,
    val highScore: Int = 0,
    val robuxBalance: Int = 100, // Starts with some pocket money
    val ownedCosmetics: List<String> = listOf("default_hat"), // "default_hat" is unlocked initially
    val equippedHat: String? = "default_hat",
    val equippedCoil: String? = null,
    val hasVip: Boolean = false,
    val registerTime: Long = System.currentTimeMillis(),
    
    // Custom Wardrobe & Design parameters
    val customTShirtColor: Int = 0xFFFFFFFF.toInt(),
    val customTShirtLogo: String? = "classic_r", // "classic_r", "bloxxer", "guest", "skull", "none"
    val customShirtColor: Int = 0xFFE63946.toInt(), // Retro Blox Red Torso
    val customHatColor: Int = 0xFFFFD700.toInt(),
    val customHatShape: String? = "none", // "none", "visor", "horns", "top_hat"
    
    // Custom Studio Place/Game parameters
    val hasCustomPlace: Boolean = false,
    val customPlaceBlockCount: Int = 12,
    val customPlaceTheme: String = "retro_green" // "retro_green", "lava_volcano", "glass_sky"
)
