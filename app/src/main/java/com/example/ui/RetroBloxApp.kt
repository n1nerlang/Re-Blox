package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ShopItem
import com.example.data.UserEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun RetroBloxApp(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    GameViewModel.Screen.LANDING -> {
                        LandingScreen(viewModel)
                    }
                    GameViewModel.Screen.MAIN_MENU -> {
                        currentUser?.let { MainMenuScreen(it, viewModel) }
                    }
                    GameViewModel.Screen.OBBY_PLAY -> {
                        currentUser?.let { GameCanvas(it, viewModel) }
                    }
                    GameViewModel.Screen.CATALOG -> {
                        currentUser?.let { CatalogScreen(it, viewModel) }
                    }
                    GameViewModel.Screen.LEADERBOARDS -> {
                        currentUser?.let { LeaderboardsScreen(it, viewModel) }
                    }
                    GameViewModel.Screen.STUDIO -> {
                        currentUser?.let { StudioScreen(it, viewModel) }
                    }
                    GameViewModel.Screen.SERVERS -> {
                        currentUser?.let { ServersScreen(viewModel) }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// LANDING / AUTHENTICATION SCREEN
// ─────────────────────────────────────────────────────────────────────────
@Composable
fun LandingScreen(viewModel: GameViewModel) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    var withEmailSignUp by remember { mutableStateOf(true) }

    // Floating logo animation
    val infiniteTransition = rememberInfiniteTransition()
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoFloat"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2C3E50), Color(0xFF1A252F))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Red Roblox-inspired blocky logo
        Box(
            modifier = Modifier
                .offset(y = floatAnim.dp)
                .background(Color(0xFFE63946), shape = RoundedCornerShape(12.dp))
                .border(2.dp, Color.White, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .shadow(8.dp)
                .testTag("app_logo")
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "RETRO BLOX",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "2010 CLASSIC SERVER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFCC00),
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Auth Form Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFD)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRegistering) "Create Character" else "Login to Server",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE63946),
                        focusedLabelColor = Color(0xFFE63946)
                    )
                )

                if (isRegistering) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Email Optional toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = withEmailSignUp,
                            onCheckedChange = { withEmailSignUp = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE63946))
                        )
                        Column {
                            Text(
                                "Register with Email address",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                "Gives FREE Fedora hat +500 Robux instantly!",
                                fontSize = 11.sp,
                                color = Color(0xFFD04848)
                            )
                        }
                    }

                    if (withEmailSignUp) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input"),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (isRegistering) {
                            viewModel.registerAccount(username, email, withEmailSignUp)
                        } else {
                            viewModel.loginExisting(username)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E8D1E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isRegistering) "Spawn Account" else "Join Epic Server",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Switch Mode text
                TextButton(
                    onClick = { isRegistering = !isRegistering },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1C5D99))
                ) {
                    Text(
                        text = if (isRegistering) "Already have an account? Log In" else "New to the server? Register Here",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Fast Guest Spawn Bypass
        Button(
            onClick = {
                val tempId = Random.nextInt(1000, 9999)
                viewModel.registerAccount("Guest_$tempId", null, false)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.35f)),
            modifier = Modifier.testTag("anonymous_guest_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Spawn instantly as Guest", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// MAIN MENU VIEW
// ─────────────────────────────────────────────────────────────────────────
@Composable
fun MainMenuScreen(user: UserEntity, viewModel: GameViewModel) {
    var isUpgradingEmail by remember { mutableStateOf(false) }
    var upgradeEmailAddress by remember { mutableStateOf("") }

    val animProgress = rememberInfiniteTransition().animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "podiumWave"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEEF5FC))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // App Header Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Icon placeholder
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .background(Color(0xFFFFD400), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (user.username.startsWith("Guest")) "G" else user.username.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.username,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFF1E293B)
                            )
                            if (user.hasVip) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFFD700), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("VIP", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = if (user.email != null) "Premium Member" else "Guest Account",
                            fontSize = 11.sp,
                            color = if (user.email != null) Color(0xFF1E8D1E) else Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Balance Panel
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(0xFFFFCC00), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "R$ ${user.robuxBalance}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC49A00),
                        fontSize = 15.sp,
                        modifier = Modifier.testTag("robux_balance_text")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center Profile 3D Avatar Podium!
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .shadow(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFDCEBFA))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Background cloud banner
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                ) {
                    Text(
                        "MY CHARACTER",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = Color(0xFF1C5D99)
                    )
                    Text(
                        "Equipped: ${user.equippedHat ?: "No Hat"}",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }

                // Call custom mini character rendering canvas inside menu!
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .offset(y = animProgress.value.dp)
                        .testTag("avatar_container")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height * 0.70f

                        // Draw podium base
                        drawOval(
                            color = Color(0xFF4291D1),
                            topLeft = Offset(cx - 45f, cy + 15f),
                            size = Size(90f, 20f)
                        )

                        // 3D Blocky Avatar drawing inside menu!
                        // Legs
                        drawRect(Color(0xFF1E8D1E), Offset(cx - 15f, cy - 10f), Size(30f, 30f))
                        // Torso
                        drawRect(Color(0xFF0D5FCD), Offset(cx - 20f, cy - 40f), Size(40f, 30f))
                        // Head
                        drawRect(Color(0xFFFFD400), Offset(cx - 12f, cy - 64f), Size(24f, 24f))
                        // Face smiley
                        drawCircle(Color.Black, 1.8f, Offset(cx - 5f, cy - 54f))
                        drawCircle(Color.Black, 1.8f, Offset(cx + 5f, cy - 54f))
                        drawArc(
                            color = Color.Black, startAngle = 10f, sweepAngle = 160f, useCenter = false,
                            topLeft = Offset(cx - 6f, cy - 50f), size = Size(12f, 8f), style = Stroke(width = 1.5f)
                        )

                        // Equipped cosmetics rendering inside menu!
                        if (user.equippedHat == "classic_fedora") {
                            // Fedora Rim
                            drawRect(Color(0xFF1C1C1C), Offset(cx - 22f, cy - 64f), Size(44f, 4f))
                            drawRect(Color(0xFF121212), Offset(cx - 14f, cy - 74f), Size(28f, 10f))
                            drawRect(Color.Red, Offset(cx - 14f, cy - 67f), Size(28f, 3f))
                        } else if (user.equippedHat == "valkyrie_helm") {
                            drawRect(Color(0xFFE2E2E2), Offset(cx - 14f, cy - 68f), Size(28f, 7f))
                            // Wings
                            drawPolygon(listOf(Offset(cx - 14f, cy - 62f), Offset(cx - 22f, cy - 80f), Offset(cx - 14f, cy - 70f)), Color(0xFFFFD700))
                            drawPolygon(listOf(Offset(cx + 14f, cy - 62f), Offset(cx + 22f, cy - 80f), Offset(cx + 14f, cy - 70f)), Color(0xFFFFD700))
                        } else if (user.equippedHat == "dominus") {
                            drawRect(Color(0xFF2E0854), Offset(cx - 17f, cy - 68f), Size(34f, 30f))
                            drawRect(Color(0xFF111111), Offset(cx - 9f, cy - 60f), Size(18f, 16f))
                        } else if (user.equippedHat == "gold_crown") {
                            drawPolygon(
                                listOf(
                                    Offset(cx - 14f, cy - 64f),
                                    Offset(cx - 14f, cy - 74f),
                                    Offset(cx - 7f, cy - 69f),
                                    Offset(cx, cy - 76f),
                                    Offset(cx + 7f, cy - 69f),
                                    Offset(cx + 14f, cy - 74f),
                                    Offset(cx + 14f, cy - 64f)
                                ), Color(0xFFFFD500)
                            )
                        } else if (user.equippedHat == "default_hat") {
                            // Classic cap
                            drawRect(Color(0xFF0044BB), Offset(cx - 14f, cy - 65f), Size(28f, 4f))
                            drawRect(Color(0xFF00AAFF), Offset(cx - 8f, cy - 68f), Size(16f, 3f))
                        }

                        // Carrying gear: Classic linked sword
                        if (user.ownedCosmetics.contains("classic_sword")) {
                            drawLine(Color.Gray, Offset(cx + 18f, cy - 10f), Offset(cx + 34f, cy - 36f), strokeWidth = 3f)
                            drawCircle(Color(0xFFFFD500), 2f, Offset(cx + 18f, cy - 10f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick action buttons
        Button(
            onClick = { viewModel.startObbyGame(); viewModel.navigateTo(GameViewModel.Screen.OBBY_PLAY) },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("play_game_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E8D1E)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("🚀 ENTER 3D PARKOUR OBBY", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.navigateTo(GameViewModel.Screen.CATALOG) },
                modifier = Modifier
                    .weight(1f)
                    .height(55.dp)
                    .testTag("catalog_shop_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("🛒 shop")
            }

            Button(
                onClick = { viewModel.navigateTo(GameViewModel.Screen.LEADERBOARDS) },
                modifier = Modifier
                    .weight(1f)
                    .height(55.dp)
                    .testTag("leaderboards_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C5D99)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("🏆 podium")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Email Sign-Up Promo card inside guest flow
        if (user.email.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFD04848), shape = RoundedCornerShape(12.dp))
                    .shadow(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFD04848))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "FREE Fedora & +500 Robux Reward!",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD04848)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Upgrade your guest account to an email account below to secure your progress and receive your free reward hats!",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isUpgradingEmail) {
                        Button(
                            onClick = { isUpgradingEmail = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD04848))
                        ) {
                            Text("Upgrade with Email now", color = Color.White)
                        }
                    } else {
                        OutlinedTextField(
                            value = upgradeEmailAddress,
                            onValueChange = { upgradeEmailAddress = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth().testTag("upgrade_email_field"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.upgradeToEmail(upgradeEmailAddress) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E8D1E))
                            ) {
                                Text("Link & Claim Fedora")
                            }
                            TextButton(onClick = { isUpgradingEmail = false }) {
                                Text("Cancel", color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF1E8D1E))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Verified email linked!", fontWeight = FontWeight.Bold, color = Color(0xFF1E8D1E))
                        Text("Linked to: ${user.email}", fontSize = 11.sp, color = Color.DarkGray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Obtain robux grant (Simulated Package/Store buy for retro fidelity)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECECEC))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Robux Dev Grants (Developer Test Tools)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.obtainDeveloperGrants(500) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF808080)),
                        modifier = Modifier.testTag("grant_500_button")
                    ) {
                        Text("+500 R$", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.obtainDeveloperGrants(2000) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF808080)),
                        modifier = Modifier.testTag("grant_2000_button")
                    ) {
                        Text("+2000 R$", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // System Log out
        TextButton(
            onClick = { viewModel.logout() },
            modifier = Modifier.align(Alignment.CenterHorizontally).testTag("logout_back")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Leave Current Server (Logout)", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// LEADERBOARDS PODIUM SCREEN
// ─────────────────────────────────────────────────────────────────────────
@Composable
fun LeaderboardsScreen(user: UserEntity, viewModel: GameViewModel) {
    val rankingUsers by viewModel.leaderboardUsers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .padding(16.dp)
    ) {
        // Back Button in Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(GameViewModel.Screen.MAIN_MENU) },
                modifier = Modifier.testTag("leaderboards_back_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Return", tint = Color.DarkGray)
            }
            Text(
                "GLOBAL SERVER LEADERBOARD",
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = Color(0xFF1E293B)
            )
            Box(modifier = Modifier.size(24.dp)) // horizontal spacer
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Top explanation notice
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Beat legendary figures like Builderman, Telamon, and clockwork in the orthographic obby to claim top server rank!",
                    fontSize = 12.sp,
                    color = Color(0xFF7F5F00)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List layout
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(rankingUsers) { index, item ->
                    val rankNum = index + 1
                    val isClient = item.username == user.username
                    
                    val rankColor = when (rankNum) {
                        1 -> Color(0xFFFFD700) // Gold
                        2 -> Color(0xFFC0C0C0) // Silver
                        3 -> Color(0xFFCD7F32) // Bronze
                        else -> Color.Transparent
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isClient) Color(0xFFE0F2FE) else Color.Transparent)
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Rank Number badge
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        if (rankNum <= 3) rankColor else Color(0xFFE2E8F0),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$rankNum",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rankNum <= 3) Color.Black else Color.DarkGray
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            
                            // Username with VIP check
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.username,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    if (item.hasVip) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFE63946), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("VIP", color = Color.White, fontSize = 8.sp)
                                        }
                                    }
                                    if (isClient) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("(You)", color = Color(0xFF0369A1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (item.email != null) {
                                    Text("Premium Developer", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }

                        // Score value
                        Text(
                            text = "${item.highScore} pts",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Divider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// CATALOG SHOP & INVENTORY SCREEN
// ─────────────────────────────────────────────────────────────────────────
@Composable
fun CatalogScreen(user: UserEntity, viewModel: GameViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0 is Shop, 1 is Wardrobe Inventory

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .padding(16.dp)
    ) {
        // Shop Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(GameViewModel.Screen.MAIN_MENU) },
                modifier = Modifier.testTag("catalog_back_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Return", tint = Color.DarkGray)
            }
            Text(
                "ROBLOX CATALOG & DEV SHOP",
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = Color(0xFF1E293B)
            )
            // Show Wallet directly in page header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(Color(0xFFFFCC00), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("R$ ${user.robuxBalance}", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF9A7B00))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Catalog Tab Row (Shop Catalog vs Wardrobe Inventory)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color(0xFFE63946)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("🛒 buy store items", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("🎒 my wardrobe", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_wardrobe")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // SHOP GRID
            val shopItems = ShopItem.allItems

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(shopItems) { item ->
                    val alreadyOwns = user.ownedCosmetics.contains(item.id) || 
                                       (item.id == "vip_pass" && user.hasVip)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Item Thumbnail box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(95.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE2EFFD)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(50.dp)) {
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    
                                    // Render 2D Item symbols
                                    when (item.id) {
                                        "default_hat" -> drawRect(Color(0xFF0044BB), Offset(cx - 15f, cy - 8f), Size(30f, 16f))
                                        "classic_fedora" -> {
                                            drawRect(Color(0xFF222222), Offset(cx - 20f, cy - 4f), Size(40f, 6f))
                                            drawRect(Color(0xFF111111), Offset(cx - 12f, cy - 14f), Size(24f, 10f))
                                        }
                                        "valkyrie_helm" -> {
                                            drawArc(Color(0xFFC0C0C0), 180f, 180f, true, Offset(cx - 15f, cy - 10f), Size(30f, 20f))
                                            drawOval(Color(0xFFFFD500), Offset(cx - 22f, cy - 15f), Size(7f, 15f))
                                            drawOval(Color(0xFFFFD500), Offset(cx + 15f, cy - 15f), Size(7f, 15f))
                                        }
                                        "dominus" -> drawArc(Color(0xFF2E0854), 180f, 180f, true, Offset(cx - 15f, cy - 12f), Size(30f, 24f))
                                        "gold_crown" -> drawPolygon(listOf(Offset(cx - 15f, cy + 5f), Offset(cx - 15f, cy - 10f), Offset(cx - 7f, cy - 4f), Offset(cx, cy - 12f), Offset(cx + 7f, cy - 4f), Offset(cx + 15f, cy - 10f), Offset(cx + 15f, cy + 5f)), Color(0xFFFFD500))
                                        "classic_sword" -> drawLine(Color.Gray, Offset(cx - 12f, cy + 12f), Offset(cx + 12f, cy - 12f), strokeWidth = 5f)
                                        "speed_coil" -> drawCircle(Color(0xFFFFD500), 15f, center = Offset(cx, cy), style = Stroke(width = 3.5f))
                                        "gravity_coil" -> drawCircle(Color(0xFF10BBFF), 15f, center = Offset(cx, cy), style = Stroke(width = 3.5f))
                                        "vip_pass" -> drawCircle(Color(0xFFFFD700), 16f, center = Offset(cx, cy))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = item.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                color = Color(0xFF1E293B)
                            )
                            
                            Text(
                                text = item.description,
                                fontSize = 10.sp,
                                color = Color.Gray,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.height(30.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            if (alreadyOwns) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp)
                                        .background(Color(0xFFE2E8F0), shape = RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Owned", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.purchaseItem(item) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp)
                                        .testTag("buy_button_${item.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E8D1E)),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = if (item.price == 0) "Claim Free" else "R$ ${item.price}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // WARDROBE INVENTORY MANAGEMENT & EQUIP FLOW
            val itemsOwned = ShopItem.allItems.filter { 
                user.ownedCosmetics.contains(it.id) || (it.id == "vip_pass" && user.hasVip)
            }

            if (itemsOwned.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No items owned! Visit the shop tab to purchase hats with Robux.", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(itemsOwned) { item ->
                        val isEquipped = when (item.type) {
                            ShopItem.ShopItemType.COSMETIC -> user.equippedHat == item.id
                            ShopItem.ShopItemType.GAMEPASS -> user.equippedCoil == item.id
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, if (isEquipped) Color(0xFF1E8D1E) else Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isEquipped) Color(0xFFE8F5E9) else Color(0xFFECF0F1)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (item.type == ShopItem.ShopItemType.COSMETIC) Icons.Default.Face else Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isEquipped) Color(0xFF1E8D1E) else Color.Gray,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                Text(
                                    text = if (item.type == ShopItem.ShopItemType.COSMETIC) "Hat cosmetic" else "Gamepass tool",
                                    fontSize = 11.sp, color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        when (item.type) {
                                            ShopItem.ShopItemType.COSMETIC -> {
                                                viewModel.equipHat(if (isEquipped) null else item.id)
                                            }
                                            ShopItem.ShopItemType.GAMEPASS -> {
                                                viewModel.equipCoil(if (isEquipped) null else item.id)
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .testTag("equip_button_${item.id}"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isEquipped) Color(0xFFD04848) else Color(0xFF1C5D99)
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = if (isEquipped) "Unequip" else "Equip",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// RETRO STUDIO CREATOR WORKSPACE
// Allows creating custom place (Map size/theme), customize clothing stamps/colors of shirt
// ─────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(user: UserEntity, viewModel: GameViewModel) {
    var blockCountInput by remember { mutableStateOf(user.customPlaceBlockCount.toString()) }
    var selectedTheme by remember { mutableStateOf(user.customPlaceTheme) }
    
    val shirtColors = listOf(
        "Yellow" to 0xFFFFD700.toInt(),
        "Red" to 0xFFE63946.toInt(),
        "Green" to 0xFF1E8D1E.toInt(),
        "Blue" to 0xFF1C5D99.toInt(),
        "Purple" to 0xFF70139C.toInt()
    )
    
    val logoStamps = listOf("Noob", "Sword", "Roblox R", "Skull")
    
    val hatShapes = listOf("visor_cap", "classic_fedora", "valkyrie_helm", "gold_crown")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEAEEF3))
            .padding(16.dp)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "🛠️ ROBLOX STUDIO SDK v2010",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1E293B)
            )
            Button(
                onClick = { viewModel.navigateTo(GameViewModel.Screen.MAIN_MENU) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.testTag("studio_exit_button")
            ) {
                Text("◀ Leave Studio", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Classic Place Builder
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(1.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFD2D6DC))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("🎮 LEVEL MAKER / PLACE DESIGNER", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0055BB))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Construct procedural blocks of customized obby track elements.", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Theme Select
                        Text("Place Sky Theme:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themes = listOf("classic" to "☀ Classic", "retro_green" to "🌳 Forest", "lava_volcano" to "🌋 Lava", "glass_sky" to "☁ Glass")
                            themes.forEach { (id, label) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (selectedTheme == id) Color(0xFF00AAFF) else Color(0xFFEEEEEE),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable { selectedTheme = id }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, fontSize = 11.sp, color = if (selectedTheme == id) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Block count size
                        Text("Obby Block Length (10 - 80 blocks):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = blockCountInput,
                            onValueChange = { blockCountInput = it },
                            modifier = Modifier.fillMaxWidth().testTag("studio_block_input"),
                            shape = RoundedCornerShape(4.dp),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                val count = blockCountInput.toIntOrNull()?.coerceIn(10, 80) ?: 28
                                viewModel.publishCustomPlace(count, selectedTheme)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.fillMaxWidth().testTag("studio_build_place_btn"),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("🛠️ BUILD & PUBLISH PLACE FOR FREE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Section 2: Clothing Creator stamps & stamp logo stamp
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(1.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFD2D6DC))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("👕 FREE SHIRT & T-SHIRT MAKER", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFFF8800))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Weave custom thread stamps and colors on classic Roblox jerseys.", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Shirt Colors
                        Text("Jersey Color Base:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            shirtColors.forEach { (name, colorVal) ->
                                val isSelected = user.customShirtColor == colorVal
                                Box(
                                    modifier = Modifier
                                        .background(Color(colorVal), RoundedCornerShape(4.dp))
                                        .clickable { viewModel.updateShirtColor(colorVal) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        name, 
                                        color = if (name == "Yellow") Color.Black else Color.White, 
                                        fontSize = 11.sp, 
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // logo Stamps
                        Text("T-Shirt Graphics Stamp Logo:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            logoStamps.forEach { stamp ->
                                val isSelected = user.customTShirtLogo == stamp
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) Color(0xFFFF8800) else Color(0xFFEEEEEE),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable { viewModel.updateTShirtLogo(stamp) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stamp, fontSize = 11.sp, color = if (isSelected) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Hat Modulator
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(1.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFD2D6DC))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("🎩 FREE 3D HAT SHAPE MODULATOR", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF6B809E))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Mutate the 3D grid shape representation of your avatar hats.", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Shapes select
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            hatShapes.forEach { shape ->
                                val isSelected = user.customHatShape == shape
                                val prettyLabel = when (shape) {
                                    "visor_cap" -> "🧢 Visor"
                                    "classic_fedora" -> "🎩 Fedora"
                                    "valkyrie_helm" -> "🛡️ Valkyrie"
                                    else -> "👑 Crown"
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) Color(0xFF6B809E) else Color(0xFFEEEEEE),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable { viewModel.updateHatShape(shape) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(prettyLabel, fontSize = 10.sp, color = if (isSelected) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// NOSTALGIC MULTIPLAYER SERVERS & CHEATS PANEL
// ─────────────────────────────────────────────────────────────────────────
@Composable
fun ServersScreen(viewModel: GameViewModel) {
    val serversList = viewModel.servers.collectAsState().value
    val speedHack = viewModel.speedHackToggle.collectAsState().value
    val noclipHack = viewModel.noclipHackToggle.collectAsState().value
    val antiCheatActive = viewModel.antiCheatActive.collectAsState().value
    val antiCheatOverride = !antiCheatActive

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE5F1FF))
            .padding(16.dp)
    ) {
        // Top row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "🌐 NOSTALGIC MULTIPLAYER LOBBY",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1E293B)
            )
            Button(
                onClick = { viewModel.navigateTo(GameViewModel.Screen.MAIN_MENU) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("◀ Back", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // List of servers
            item {
                Text("Select Regional Server Location:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(6.dp))
                
                serversList.forEach { server ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.selectServer(server)
                                viewModel.connectToServer(server) {
                                    viewModel.navigateTo(GameViewModel.Screen.OBBY_PLAY)
                                }
                            }
                            .shadow(1.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF9FCFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(server.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📶 Ping: ${server.ping}ms", fontSize = 11.sp, color = colorForPing(server.ping))
                                    Text("👥 Active Players: ${server.playersCount}/${server.maxPlayers}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF00AAFF), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("CONNECT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // CHEAT ENGINE mutation dashboard
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)), 
                    border = BorderStroke(2.dp, Color(0xFFFFC107))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️ RETRO CHEAT ENGINE COMPILER", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFFA57C00))
                        }
                        Text("Clients can edit memory variables. However, if Anti-Cheat is enabled on servers, cheats will trigger immediate BAN warnings!", fontSize = 10.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Speedhack Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Inject SpeedHack [2.5x speed]", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Forces client translation updates velocity", fontSize = 10.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = speedHack,
                                onCheckedChange = {
                                    viewModel.setSpeedHack(it)
                                },
                                modifier = Modifier.testTag("cheat_speed_switch")
                            )
                        }

                        // Fly/Noclip Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Noclip Gravity Overrides [Fly jump]", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Bypasses standard acceleration controls", fontSize = 10.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = noclipHack,
                                onCheckedChange = {
                                    viewModel.setNoclipHack(it)
                                },
                                modifier = Modifier.testTag("cheat_noclip_switch")
                            )
                        }

                        // Anti-cheat Override Bypass
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Override Server Firewall [Adios Anti-Cheat]", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Cloaks packet injection from Retro-Guard", fontSize = 10.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = antiCheatOverride,
                                onCheckedChange = {
                                    viewModel.toggleAntiCheat(!it)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun colorForPing(latency: Int): Color {
    return when {
        latency < 40 -> Color(0xFF388E3C)
        latency < 120 -> Color(0xFFF57C00)
        else -> Color(0xFFD32F2F)
    }
}
