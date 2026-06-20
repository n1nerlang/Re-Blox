package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = UserRepository(database.userDao())

    // UI screen states
    enum class Screen {
        LANDING,     // Login / signup
        MAIN_MENU,   // Play, Shop, Leaderboards
        OBBY_PLAY,   // The actual 3D custom obby game
        CATALOG,     // Cosmetic & gamepass shop
        LEADERBOARDS, // Sorted list of high-scores
        STUDIO,      // Create custom games, shirts & hats
        SERVERS      // Nostalgic custom regional servers
    }

    private val _currentScreen = MutableStateFlow(Screen.LANDING)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Leaderboard flow: includes both local registered players & preseeded legends
    val leaderboardUsers: StateFlow<List<UserEntity>> = repository.allLocalUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Game stats
    private val _gameState = MutableStateFlow<GameState>(GameState.Idle)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _activeScore = MutableStateFlow(0)
    val activeScore: StateFlow<Int> = _activeScore.asStateFlow()

    private val _activeHp = MutableStateFlow(100)
    val activeHp: StateFlow<Int> = _activeHp.asStateFlow()

    private val _activeRobuxCollected = MutableStateFlow(0)
    val activeRobuxCollected: StateFlow<Int> = _activeRobuxCollected.asStateFlow()

    // MULTIPLAYER SIMULATION STATE
    private val _servers = MutableStateFlow<List<GameServer>>(emptyList())
    val servers: StateFlow<List<GameServer>> = _servers.asStateFlow()

    private val _selectedServer = MutableStateFlow<GameServer?>(null)
    val selectedServer: StateFlow<GameServer?> = _selectedServer.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // ANTICHEAT PARAMETERS
    private val _antiCheatActive = MutableStateFlow(true)
    val antiCheatActive: StateFlow<Boolean> = _antiCheatActive.asStateFlow()

    private val _anticheatLogs = MutableStateFlow<List<String>>(emptyList())
    val anticheatLogs: StateFlow<List<String>> = _anticheatLogs.asStateFlow()

    private val _speedHackToggle = MutableStateFlow(false)
    val speedHackToggle: StateFlow<Boolean> = _speedHackToggle.asStateFlow()

    private val _noclipHackToggle = MutableStateFlow(false)
    val noclipHackToggle: StateFlow<Boolean> = _noclipHackToggle.asStateFlow()

    private val _exploitDetected = MutableStateFlow(false)
    val exploitDetected: StateFlow<Boolean> = _exploitDetected.asStateFlow()

    // Transaction feedback (error messages, purchase successes, etc.)
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    init {
        viewModelScope.launch {
            // Pre-populate with classic profiles
            repository.seedMockUsersIfEmpty()
            
            // Populate mock nostalgic gameservers
            val defaultServers = listOf(
                GameServer("srv_1", "Classic Crossroads [Official]", "[Crossroads Std Mode]", 5, 8, 42),
                GameServer("srv_2", "Sword Fight Heights Combat", "[Chaos Heights Mode]", 3, 10, 68),
                GameServer("srv_3", "Obby World Masters #01", "[Obsidian Peak Level]", 6, 12, 54),
                GameServer("srv_4", "Chaos Canyon Arena", "[Chaos Laser Combat]", 2, 8, 105),
                GameServer("srv_5", "My Custom Studio Sandbox", "[Baseplate Build Mode]", 1, 6, 15)
            )
            _servers.value = defaultServers
            _selectedServer.value = defaultServers[0]

            // Pre-populate chats
            _chatMessages.value = listOf(
                ChatMessage("m1", "System", "Welcome to Retro Blox Classic Game Servers! Double jump is enabled (JUMP twice in mid-air).", isSystem = true),
                ChatMessage("m2", "Builderman", "Hey! Use our robust Studio to build your own Places, Hats & Shirts for free! See you inside!", isSystem = false),
                ChatMessage("m3", "Telamon", "Whoa, have you tried Wall Running on vertical obstacles near platforms?! Press jump near sides!", isSystem = false),
                ChatMessage("m4", "clockwork", "If you fall near a block edge, simply hold climb to execute a Ledge Grab!", isSystem = false)
            )
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen != Screen.OBBY_PLAY) {
            _gameState.value = GameState.Idle
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUTHENTICATION SECTIONS
    // ─────────────────────────────────────────────────────────────────────────

    fun registerAccount(username: String, email: String?, isEmailSignUp: Boolean) {
        val trimmedUsername = username.trim()
        if (trimmedUsername.isEmpty()) {
            _feedbackMessage.value = "Username cannot be empty!"
            return
        }
        if (trimmedUsername.length < 3) {
            _feedbackMessage.value = "Username must be at least 3 characters!"
            return
        }

        viewModelScope.launch {
            val existing = repository.getUserByUsername(trimmedUsername)
            if (existing != null) {
                _feedbackMessage.value = "Username already taken on this server!"
                return@launch
            }

            // Determine if they qualify for the FREE hat (email provided during account creation)
            val hasEmail = isEmailSignUp && !email.isNullOrBlank()
            val initialBalance = if (hasEmail) 600 else 100 // 500 bonus + 100 standard
            val initialCosmetics = if (hasEmail) {
                listOf("default_hat", "classic_fedora")
            } else {
                listOf("default_hat")
            }
            val initialHat = if (hasEmail) "classic_fedora" else "default_hat"

            val newUser = UserEntity(
                username = trimmedUsername,
                email = if (hasEmail) email?.trim() else null,
                robuxBalance = initialBalance,
                ownedCosmetics = initialCosmetics,
                equippedHat = initialHat,
                equippedCoil = null,
                highScore = 0
            )

            repository.saveUser(newUser)
            _currentUser.value = newUser
            _feedbackMessage.value = if (hasEmail) {
                "Registered successfully! Received FREE Classic Fedora Hat & +500 Robux!"
            } else {
                "Guest Account Registered! Upgrade with an email anytime to get a FREE Hat!"
            }
            _currentScreen.value = Screen.MAIN_MENU
        }
    }

    fun loginExisting(username: String) {
        val trimmedUsername = username.trim()
        if (trimmedUsername.isEmpty()) {
            _feedbackMessage.value = "Please enter your username!"
            return
        }

        viewModelScope.launch {
            val user = repository.getUserByUsername(trimmedUsername)
            if (user != null) {
                _currentUser.value = user
                _feedbackMessage.value = "Welcome back, $trimmedUsername!"
                _currentScreen.value = Screen.MAIN_MENU
            } else {
                _feedbackMessage.value = "Account not found on this server. Create it below!"
            }
        }
    }

    // Convert Guest Account to Email Registered
    fun upgradeToEmail(email: String) {
        val user = _currentUser.value ?: return
        if (!user.email.isNullOrBlank()) {
            _feedbackMessage.value = "Your account is already email verified!"
            return
        }
        if (email.trim().isEmpty() || !email.contains("@")) {
            _feedbackMessage.value = "Please enter a valid email address!"
            return
        }

        viewModelScope.launch {
            val updatedCosmetics = user.ownedCosmetics.toMutableList()
            if (!updatedCosmetics.contains("classic_fedora")) {
                updatedCosmetics.add("classic_fedora")
            }
            val updatedUser = user.copy(
                email = email.trim(),
                robuxBalance = user.robuxBalance + 500,
                ownedCosmetics = updatedCosmetics,
                equippedHat = "classic_fedora" // auto-equip the free fedora!
            )
            repository.saveUser(updatedUser)
            _currentUser.value = updatedUser
            _feedbackMessage.value = "Email Linked! Unlocked FREE Fedora Hat & +500 Robux!"
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentScreen.value = Screen.LANDING
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHOP / ECONOMY SECTIONS
    // ─────────────────────────────────────────────────────────────────────────

    fun purchaseItem(shopItem: ShopItem) {
        val user = _currentUser.value ?: return
        if (user.ownedCosmetics.contains(shopItem.id)) {
            _feedbackMessage.value = "You already own this item!"
            return
        }

        if (user.robuxBalance < shopItem.price) {
            _feedbackMessage.value = "Insufficient Robux! Earn more in the Obby, or obtain a developer grant below."
            return
        }

        viewModelScope.launch {
            val updatedCosmetics = user.ownedCosmetics.toMutableList()
            updatedCosmetics.add(shopItem.id)

            val updatedUser = user.copy(
                robuxBalance = user.robuxBalance - shopItem.price,
                ownedCosmetics = updatedCosmetics,
                hasVip = user.hasVip || shopItem.id == "vip_pass"
            )

            repository.saveUser(updatedUser)
            _currentUser.value = updatedUser
            _feedbackMessage.value = "Successfully purchased ${shopItem.name}!"
        }
    }

    fun equipHat(itemId: String?) {
        val user = _currentUser.value ?: return
        if (itemId != null && !user.ownedCosmetics.contains(itemId)) {
            _feedbackMessage.value = "You must purchase this cosmetic first!"
            return
        }

        viewModelScope.launch {
            val updatedUser = user.copy(equippedHat = itemId)
            repository.saveUser(updatedUser)
            _currentUser.value = updatedUser
        }
    }

    fun equipCoil(itemId: String?) {
        val user = _currentUser.value ?: return
        if (itemId != null && !user.ownedCosmetics.contains(itemId)) {
            _feedbackMessage.value = "You must purchase this gamepass first!"
            return
        }

        viewModelScope.launch {
            val updatedUser = user.copy(equippedCoil = itemId)
            repository.saveUser(updatedUser)
            _currentUser.value = updatedUser
        }
    }

    // Simulate an in-app "Robux / Tix purchase card" for retro authenticity!
    fun obtainDeveloperGrants(amount: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updatedUser = user.copy(
                robuxBalance = user.robuxBalance + amount
            )
            repository.saveUser(updatedUser)
            _currentUser.value = updatedUser
            _feedbackMessage.value = "Grant approved! Added +$amount Robux to your Account!"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OBBY PARKOUR GAMEPLAY AGENT
    // ─────────────────────────────────────────────────────────────────────────

    fun startObbyGame() {
        _activeScore.value = 0
        _activeHp.value = 100
        _activeRobuxCollected.value = 0
        _gameState.value = GameState.Playing
    }

    fun updateScoreAndCoin(scoreBonus: Int, coinCollected: Int) {
        val user = _currentUser.value ?: return
        val currentScore = _activeScore.value + scoreBonus
        _activeScore.value = currentScore
        
        // Multiplier from VIP gamepass
        val multiplier = if (user.hasVip) 2 else 1
        _activeRobuxCollected.value += coinCollected * multiplier
    }

    fun decreaseHp(damage: Int) {
        val currentHp = _activeHp.value - damage
        if (currentHp <= 0) {
            _activeHp.value = 0
            endObbyGame()
        } else {
            _activeHp.value = currentHp
        }
    }

    fun endObbyGame() {
        _gameState.value = GameState.GameOver
        val finalScore = _activeScore.value
        val finalRobuxCollected = _activeRobuxCollected.value
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            val isNewHighScore = finalScore > user.highScore
            val updatedUser = user.copy(
                highScore = if (isNewHighScore) finalScore else user.highScore,
                robuxBalance = user.robuxBalance + finalRobuxCollected
            )
            repository.saveUser(updatedUser)
            _currentUser.value = updatedUser
            _feedbackMessage.value = if (isNewHighScore) {
                "NEW HIGH SCORE: $finalScore! Earned +$finalRobuxCollected Robux!"
            } else {
                "Obby complete! Final Score: $finalScore. Earned +$finalRobuxCollected Robux!"
            }
        }
    }

    fun selectServer(server: GameServer) {
        _selectedServer.value = server
    }

    fun connectToServer(server: GameServer, onConnected: () -> Unit) {
        viewModelScope.launch {
            _connectionStatus.value = "Authenticating with ${server.name}..."
            kotlinx.coroutines.delay(500)
            _connectionStatus.value = "Downloading ${server.placeName} obby level assets..."
            kotlinx.coroutines.delay(650)
            _connectionStatus.value = "Checking Anti-Cheat signature engine..."
            kotlinx.coroutines.delay(400)
            _connectionStatus.value = "Ready"
            onConnected()
        }
    }

    fun setSpeedHack(enabled: Boolean) {
        _speedHackToggle.value = enabled
        if (enabled && _antiCheatActive.value) {
            triggerAntiCheatAlert("Speed Hack / Cheat Engine (Injection Code: 0x88F)")
        }
    }

    fun setNoclipHack(enabled: Boolean) {
        _noclipHackToggle.value = enabled
        if (enabled && _antiCheatActive.value) {
            triggerAntiCheatAlert("Noclip / Gravity Bypass (Fly Hack Triggered)")
        }
    }

    fun toggleAntiCheat(enabled: Boolean) {
        _antiCheatActive.value = enabled
        if (enabled && (_speedHackToggle.value || _noclipHackToggle.value)) {
            triggerAntiCheatAlert("Retro Guard System Enabled while Hacks Active")
        }
    }

    fun triggerAntiCheatAlert(reason: String) {
        _exploitDetected.value = true
        val logMsg = "Exploit Attempt Detected: $reason [BANNED SIMULATOR WARNING]"
        val currentLogs = _anticheatLogs.value.toMutableList()
        currentLogs.add(0, logMsg)
        _anticheatLogs.value = currentLogs
    }

    fun clearCheatAlert() {
        _exploitDetected.value = false
        _speedHackToggle.value = false
        _noclipHackToggle.value = false
    }

    fun sendChatMessage(text: String) {
        val user = _currentUser.value ?: return
        val msgId = "m_" + System.currentTimeMillis()
        val userMsg = ChatMessage(msgId, user.username, text)
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(userMsg)
        _chatMessages.value = currentList

        // Mock automated replies from iconic NPCs!
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            val npcMsg = when {
                text.lowercase().contains("hi") || text.lowercase().contains("hello") -> {
                    ChatMessage("reply_" + System.currentTimeMillis(), "Builderman", "Greetings, ${user.username}! Welcome!")
                }
                text.lowercase().contains("how") || text.lowercase().contains("stuck") || text.lowercase().contains("guide") -> {
                    ChatMessage("reply_" + System.currentTimeMillis(), "clockwork", "Double jump (press JUMP in mid-air) to bypass high blocks, or wallrun!")
                }
                text.lowercase().contains("cheat") || text.lowercase().contains("hack") -> {
                    ChatMessage("reply_" + System.currentTimeMillis(), "Telamon", "Careful, Retro Blox anticheat system is watching! Don't speedhack! OOF!")
                }
                text.lowercase().contains("vip") -> {
                    ChatMessage("reply_" + System.currentTimeMillis(), "Merely", "VIP gamepass gives 2x Robux collection!")
                }
                else -> {
                    val quickReplies = listOf(
                        "OOF!",
                        "Epic place!",
                        "Telamon is on a streak!",
                        "Retro Roblox is the best!",
                        "Classic Crossroads has so many studs!"
                    )
                    ChatMessage("reply_" + System.currentTimeMillis(), listOf("Builderman", "Telamon", "clockwork", "NoobMaster").random(), quickReplies.random())
                }
            }
            val newList = _chatMessages.value.toMutableList()
            newList.add(npcMsg)
            _chatMessages.value = newList
        }
    }

    fun updateTShirtColor(color: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(customTShirtColor = color)
            repository.saveUser(updated)
            _currentUser.value = updated
        }
    }

    fun updateTShirtLogo(logo: String?) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(customTShirtLogo = logo)
            repository.saveUser(updated)
            _currentUser.value = updated
        }
    }

    fun updateShirtColor(color: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(customShirtColor = color)
            repository.saveUser(updated)
            _currentUser.value = updated
        }
    }

    fun updateHatColor(color: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(customHatColor = color)
            repository.saveUser(updated)
            _currentUser.value = updated
        }
    }

    fun updateHatShape(shape: String?) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(customHatShape = shape)
            repository.saveUser(updated)
            _currentUser.value = updated
        }
    }

    fun publishCustomPlace(blockCount: Int, theme: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(
                hasCustomPlace = true,
                customPlaceBlockCount = blockCount,
                customPlaceTheme = theme
            )
            repository.saveUser(updated)
            _currentUser.value = updated
            
            // Add custom local server
            val currentServersList = _servers.value.toMutableList()
            val customSrv = GameServer(
                "srv_custom",
                "${user.username}'s Custom Place Server",
                theme.uppercase().replace("_", " "),
                1,
                6,
                24
            )
            currentServersList.add(0, customSrv)
            _servers.value = currentServersList
            
            _feedbackMessage.value = "Studio place published successfully to servers! PLAY or HOST now!"
        }
    }
}

data class ChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val isSystem: Boolean = false
)

data class GameServer(
    val id: String,
    val name: String,
    val placeName: String,
    val playersCount: Int,
    val maxPlayers: Int,
    val ping: Int,
    val status: String = "Online"
)

sealed class GameState {
    object Idle : GameState()
    object Playing : GameState()
    object GameOver : GameState()
}
