package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import com.example.data.ShopItem
import com.example.data.UserEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Represents a 3D Block inside our orthographic platform engine
data class Block3D(
    val x: Float,       // Center X (-150 to 150)
    val y: Float,       // Height of base (0 is main level)
    val z: Float,       // Distance from start (Z)
    val width: Float,   // X-span
    val height: Float,  // Y-span
    val depth: Float,   // Z-span
    val color: Color,
    val type: BlockType = BlockType.NORMAL,
    var isCollected: Boolean = false // Only for collectible items
) {
    enum class BlockType {
        NORMAL,       // Regular platform
        TRUSS,        // Walkable but vertical
        KILL_BRICK,   // Red glow. Instant damage
        JUMP_PAD,     // Trampoline boost
        SPEED_PAD,    // Boost pad
        CHECKPOINT,   // Spawns safe spot
        GOLD_COIN     // Collective coin yielding Robux/Tix
    }
}

@Composable
fun GameCanvas(
    user: UserEntity,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Player position
    var playerX by remember { mutableStateOf(0f) }
    var playerY by remember { mutableStateOf(20f) }
    var playerZ by remember { mutableStateOf(0f) }

    // Physics velocities
    var velX by remember { mutableStateOf(0f) }
    var velY by remember { mutableStateOf(0f) }
    var velZ by remember { mutableStateOf(0f) }

    // Game track state
    val trackBlocks = remember { mutableStateListOf<Block3D>() }
    var currentCheckpointZ by remember { mutableStateOf(0f) }
    var currentCheckpointX by remember { mutableStateOf(0f) }
    var currentCheckpointY by remember { mutableStateOf(10f) }
    var oofAlertActive by remember { mutableStateOf(false) }
    var oofRotation by remember { mutableStateOf(0f) }
    
    // Parkour States
    var doubleJumpAvailable by remember { mutableStateOf(true) }
    var isWallRunning by remember { mutableStateOf(false) }
    var isLedgeGrabbing by remember { mutableStateOf(false) }
    var showDoubleJumpBurst by remember { mutableStateOf(false) }
    var doubleJumpBurstY by remember { mutableStateOf(0f) }
    var doubleJumpBurstZ by remember { mutableStateOf(0f) }

    // Multi-player chat console visible
    var chatBoxOpen by remember { mutableStateOf(false) }
    var chatInputText by remember { mutableStateOf("") }
    
    // Input buttons state
    var leftPressed by remember { mutableStateOf(false) }
    var rightPressed by remember { mutableStateOf(false) }
    var jumpTriggered by remember { mutableStateOf(false) }

    // Coil coefficients
    val speedMultiplier = if (user.equippedCoil == "speed_coil") 1.8f else 1.0f
    val jumpMultiplier = if (user.equippedCoil == "gravity_coil") 1.5f else 1.0f

    // Particle effect
    val animProgress = rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )

    // Setup map
    LaunchedEffect(Unit) {
        trackBlocks.clear()
        
        val blockCount = if (user.hasCustomPlace) user.customPlaceBlockCount else 28
        val theme = if (user.hasCustomPlace) user.customPlaceTheme else "classic"
        
        val baseBlockColor = when (theme) {
            "retro_green" -> Color(0xFF2E8B57)
            "lava_volcano" -> Color(0xFF4A1525)
            "glass_sky" -> Color(0xBB5CACEE)
            else -> Color(0xFF6B809E)
        }
        
        // Spawn/Starting platform
        trackBlocks.add(Block3D(0f, 0f, 50f, 150f, 10f, 160f, baseBlockColor, Block3D.BlockType.NORMAL))
        
        var nextZ = 180f
        var currentY = 0f
        val random = Random(42)

        // Procedural generation of 3D Obby blocks!
        for (i in 1..blockCount) {
            val typeVal = random.nextFloat()
            val spacing = 70f + random.nextInt(40)
            nextZ += spacing

            when {
                // Truss stairs
                typeVal < 0.2f -> {
                    trackBlocks.add(Block3D(0f, currentY + 15f, nextZ, 80f, 8f, 60f, Color(0xFFE25B5B), Block3D.BlockType.NORMAL))
                    trackBlocks.add(Block3D(0f, currentY + 18f, nextZ + 20f, 40f, 25f, 5f, Color(0xFFFFCC00), Block3D.BlockType.GOLD_COIN))
                    currentY += 15f
                    nextZ += 30f
                }
                // Kill Bricks (Holographic lasers)
                typeVal < 0.4f -> {
                    trackBlocks.add(Block3D(0f, currentY, nextZ, 120f, 10f, 180f, Color(0xFF4CA64C), Block3D.BlockType.NORMAL))
                    // Obstacle kill brick on top
                    trackBlocks.add(Block3D(0f, currentY + 11f, nextZ + 40f, 100f, 12f, 15f, Color(0xFFFF0D0D), Block3D.BlockType.KILL_BRICK))
                    // Golden Robux coin behind laser
                    trackBlocks.add(Block3D(0f, currentY + 22f, nextZ + 90f, 15f, 15f, 15f, Color(0xFFFFCC00), Block3D.BlockType.GOLD_COIN))
                }
                // Jump Trampoline Pad
                typeVal < 0.6f -> {
                    trackBlocks.add(Block3D(0f, currentY, nextZ, 90f, 10f, 80f, Color(0xFF555555), Block3D.BlockType.NORMAL))
                    trackBlocks.add(Block3D(0f, currentY + 11f, nextZ + 20f, 60f, 5f, 40f, Color(0xFFFFFF00), Block3D.BlockType.JUMP_PAD))
                    nextZ += 40f
                }
                // Vanishing Checkpoint and path splits
                typeVal < 0.8f -> {
                    // Checkpoint block
                    trackBlocks.add(Block3D(0f, currentY, nextZ, 120f, 10f, 100f, Color(0xFF00FF77), Block3D.BlockType.CHECKPOINT))
                    
                    // Left and right split block
                    trackBlocks.add(Block3D(-50f, currentY + 10f, nextZ + 120f, 35f, 10f, 35f, Color(0xFF1E90FF), Block3D.BlockType.NORMAL))
                    trackBlocks.add(Block3D(50f, currentY + 10f, nextZ + 120f, 35f, 10f, 35f, Color(0xFF1E90FF), Block3D.BlockType.NORMAL))
                    
                    trackBlocks.add(Block3D(-50f, currentY + 28f, nextZ + 120f, 10f, 10f, 10f, Color(0xFFFFCC00), Block3D.BlockType.GOLD_COIN))
                    trackBlocks.add(Block3D(50f, currentY + 28f, nextZ + 120f, 10f, 10f, 10f, Color(0xFFFFCC00), Block3D.BlockType.GOLD_COIN))
                    currentY += 10f
                    nextZ += 150f
                }
                // Standard block blocks
                else -> {
                    val sideOffset = (random.nextFloat() * 80f) - 40f
                    trackBlocks.add(Block3D(sideOffset, currentY, nextZ, 70f, 10f, 70f, Color(0xFFFFA500), Block3D.BlockType.NORMAL))
                    trackBlocks.add(Block3D(sideOffset, currentY + 18f, nextZ, 12f, 12f, 12f, Color(0xFFFFCC00), Block3D.BlockType.GOLD_COIN))
                }
            }
        }

        // Final victory trophy platform!
        trackBlocks.add(Block3D(0f, currentY, nextZ + 100f, 200f, 20f, 200f, Color(0xFFFFD700), Block3D.BlockType.NORMAL))
        trackBlocks.add(Block3D(0f, currentY + 30f, nextZ + 100f, 40f, 40f, 40f, Color(0xFFFFFF00), Block3D.BlockType.GOLD_COIN))
        trackBlocks.add(Block3D(0f, currentY + 70f, nextZ + 100f, 50f, 50f, 50f, Color(0xFFFF8800), Block3D.BlockType.GOLD_COIN))
    }

    // Active Physics Frame ticks
    LaunchedEffect(viewModel.gameState.collectAsState().value) {
        while (viewModel.gameState.value == GameState.Playing) {
            delay(16) // Target 60fps physics loop

            val isSpeedHack = viewModel.speedHackToggle.value
            val isNoclipHack = viewModel.noclipHackToggle.value

            val actualSpeedMultiplier = speedMultiplier * (if (isSpeedHack) 2.4f else 1.0f)
            val actualJumpMultiplier = jumpMultiplier * (if (isNoclipHack) 1.8f else 1.0f)

            // Forward speed is constant along Z (runs automatically to match linear dynamic run style)
            velZ = (if (isNoclipHack) 7.5f else 4.8f) * actualSpeedMultiplier

            // Movement X physics
            if (leftPressed) {
                velX = -3.8f * actualSpeedMultiplier
            } else if (rightPressed) {
                velX = 3.8f * actualSpeedMultiplier
            } else {
                velX *= 0.70f // ground friction
            }

            // Ground/Platform collisions detection
            var isGrounded = playerY <= 12f // bottom boundary
            for (block in trackBlocks) {
                if (block.type == Block3D.BlockType.GOLD_COIN) {
                    val distSq = (playerX - block.x)*(playerX - block.x) + 
                                 (playerY - block.y)*(playerY - block.y) + 
                                 (playerZ - block.z)*(playerZ - block.z)
                    if (distSq < 1500f && !block.isCollected) {
                        block.isCollected = true
                        viewModel.updateScoreAndCoin(scoreBonus = 100, coinCollected = 15)
                    }
                    continue
                }

                val xMin = block.x - block.width / 2f - 22f
                val xMax = block.x + block.width / 2f + 22f
                val zMin = block.z - block.depth / 2f - 22f
                val zMax = block.z + block.depth / 2f + 22f

                if (playerX in xMin..xMax && playerZ in zMin..zMax) {
                    val blockTop = block.y + block.height / 2f
                    // Surface landing check
                    if (playerY >= blockTop - 12f && velY <= 0) {
                        if (playerY - blockTop <= 15f) {
                            playerY = blockTop + 10f
                            velY = 0f
                            isGrounded = true

                            // Special blocks effects
                            if (block.type == Block3D.BlockType.KILL_BRICK) {
                                coroutineScope.launch {
                                    if (!oofAlertActive) {
                                        oofAlertActive = true
                                        oofRotation = 90f
                                        viewModel.decreaseHp(35)
                                        delay(400)
                                        oofAlertActive = false
                                        playerX = currentCheckpointX
                                        playerY = currentCheckpointY + 25f
                                        playerZ = currentCheckpointZ - 10f
                                        velX = 0f; velY = 0f
                                        isWallRunning = false
                                        isLedgeGrabbing = false
                                    }
                                }
                            } else if (block.type == Block3D.BlockType.JUMP_PAD) {
                                velY = 22.0f * actualJumpMultiplier // Mega bounce!
                            } else if (block.type == Block3D.BlockType.CHECKPOINT) {
                                currentCheckpointZ = block.z
                                currentCheckpointX = block.x
                                currentCheckpointY = block.y
                            }
                        }
                    }
                }
            }

            if (isGrounded) {
                doubleJumpAvailable = true
                isWallRunning = false
                isLedgeGrabbing = false
            }

            // WALL RUN PHYSICS check
            if (!isGrounded && !isLedgeGrabbing) {
                var nearLeftWall = false
                var nearRightWall = false
                for (block in trackBlocks) {
                    if (block.type == Block3D.BlockType.GOLD_COIN || block.type == Block3D.BlockType.KILL_BRICK) continue
                    val yMin = block.y - block.height / 2f - 5f
                    val yMax = block.y + block.height / 2f + 20f
                    val zMin = block.z - block.depth / 2f - 18f
                    val zMax = block.z + block.depth / 2f + 18f

                    if (playerY in yMin..yMax && playerZ in zMin..zMax) {
                        val leftEdge = block.x - block.width / 2f
                        val rightEdge = block.x + block.width / 2f
                        if (kotlin.math.abs(playerX - leftEdge) in 10f..28f) nearLeftWall = true
                        if (kotlin.math.abs(playerX - rightEdge) in 10f..28f) nearRightWall = true
                    }
                }

                if ((nearLeftWall && leftPressed) || (nearRightWall && rightPressed)) {
                    isWallRunning = true
                    doubleJumpAvailable = true // wall run resets jump!
                } else {
                    isWallRunning = false
                }
            }

            // LEDGE GRAB DESIGN check
            if (!isGrounded && !isWallRunning && velY < 0) {
                var canGrab = false
                var blockTopY = 0f
                for (block in trackBlocks) {
                    if (block.type == Block3D.BlockType.GOLD_COIN || block.type == Block3D.BlockType.KILL_BRICK) continue
                    val xMin = block.x - block.width / 2f - 24f
                    val xMax = block.x + block.width / 2f + 24f
                    val zMin = block.z - block.depth / 2f - 24f
                    val zMax = block.z + block.depth / 2f + 24f

                    if (playerX in xMin..xMax && playerZ in zMin..zMax) {
                        val blockTop = block.y + block.height / 2f
                        if (playerY >= blockTop - 18f && playerY < blockTop - 2f) {
                            canGrab = true
                            blockTopY = blockTop
                            break
                        }
                    }
                }

                if (canGrab) {
                    isLedgeGrabbing = true
                    playerY = blockTopY - 6f // suspend player placement
                    velY = 0f
                }
            }

            // Displace along velocities
            if (isLedgeGrabbing) {
                velY = 0f
            } else if (isWallRunning) {
                velY = -1.2f // slow crawl slide
                velZ *= 1.3f // burst speed
            } else {
                velY -= 0.65f // gravity pull
            }

            // Apply Jump / parkour actions
            if (jumpTriggered) {
                jumpTriggered = false
                if (isLedgeGrabbing) {
                    velY = 11.5f * actualJumpMultiplier
                    isLedgeGrabbing = false
                    doubleJumpAvailable = true
                } else if (isWallRunning) {
                    velY = 12.5f * actualJumpMultiplier
                    velX = if (leftPressed) 5f else -5f
                    isWallRunning = false
                } else if (isGrounded) {
                    velY = 12.0f * actualJumpMultiplier
                } else if (doubleJumpAvailable) {
                    velY = 11.0f * actualJumpMultiplier
                    doubleJumpAvailable = false
                    showDoubleJumpBurst = true
                    doubleJumpBurstY = playerY
                    doubleJumpBurstZ = playerZ
                    coroutineScope.launch {
                        delay(200)
                        showDoubleJumpBurst = false
                    }
                }
            }

            // Apply displacement
            playerX += velX
            playerY += velY
            playerZ += velZ

            // Boundaries
            playerX = playerX.coerceIn(-180f, 180f)

            // If fell down or died
            if (playerY < -80f) {
                coroutineScope.launch {
                    if (!oofAlertActive) {
                        oofAlertActive = true
                        viewModel.decreaseHp(25)
                        delay(400)
                        oofAlertActive = false
                        playerX = currentCheckpointX
                        playerY = currentCheckpointY + 25f
                        playerZ = currentCheckpointZ - 10f
                        velX = 0f; velY = 0f
                        isWallRunning = false
                        isLedgeGrabbing = false
                    }
                }
            }

            // Earn passive score based on distance run
            if (playerZ.toInt() / 5 > viewModel.activeScore.value / 10) {
                viewModel.updateScoreAndCoin(scoreBonus = 1, coinCollected = 0)
            }

            // Check victory of obby
            val finalBlock = trackBlocks.lastOrNull { it.type == Block3D.BlockType.NORMAL }
            if (finalBlock != null && playerZ > finalBlock.z + finalBlock.depth / 2f) {
                viewModel.endObbyGame()
            }
        }
    }

    // Interactive Layout structure
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE5F1FF)) // Classic retro skybox color
    ) {
        
        // 3D Canvas Rendering
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .focusable()
                .pointerInput(Unit) {
                    detectTapGestures {
                        jumpTriggered = true
                    }
                }
                .testTag("game_render_canvas")
        ) {
            val width = size.width
            val height = size.height

            // 1. Draw Retro Roblox Gradient Sky & fluffy classic sun/clouds
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF3B95FF), Color(0xFFC7E2FF)),
                    start = Offset(0f, 0f),
                    end = Offset(0f, height * 0.7f)
                )
            )

            // Draw nostalgic square clouds
            for (i in 0..10) {
                val cloudX = (150f + (i * 240f) - (playerZ * 0.15f)) % (width + 300f) - 150f
                val cloudY = 40f + (i % 3) * 35f
                drawRect(
                    color = Color.White.copy(alpha = 0.55f),
                    topLeft = Offset(cloudX, cloudY),
                    size = Size(110f + (i % 2) * 40f, 60f)
                )
            }

            // Look slightly ahead of player
            val camZ = playerZ

            // 2. Render all blocks using custom orthographic/perspective projecting
            // We sort back-to-front so closer objects overlap further ones.
            // Distance is block.z
            val visibleBlocks = trackBlocks.filter { 
                it.z > camZ - 120f && it.z < camZ + 850f 
            }.sortedByDescending { it.z }

            for (block in visibleBlocks) {
                if (block.type == Block3D.BlockType.GOLD_COIN && block.isCollected) continue

                // Project vertices of the cuboid block
                val halfW = block.width / 2f
                val halfH = block.height / 2f
                val halfD = block.depth / 2f

                // Frontend face points
                val fTL = project3d(block.x - halfW, block.y + halfH, block.z - halfD, width, height, playerZ)
                val fTR = project3d(block.x + halfW, block.y + halfH, block.z - halfD, width, height, playerZ)
                val fBL = project3d(block.x - halfW, block.y - halfH, block.z - halfD, width, height, playerZ)
                val fBR = project3d(block.x + halfW, block.y - halfH, block.z - halfD, width, height, playerZ)

                // Backend face points
                val bTL = project3d(block.x - halfW, block.y + halfH, block.z + halfD, width, height, playerZ)
                val bTR = project3d(block.x + halfW, block.y + halfH, block.z + halfD, width, height, playerZ)
                val bBL = project3d(block.x - halfW, block.y - halfH, block.z + halfD, width, height, playerZ)
                val bBR = project3d(block.x + halfW, block.y - halfH, block.z + halfD, width, height, playerZ)

                // Standard classic block colors. Let's color faces to simulate 3D directional lighting (sun on top)
                val isKillBrick = block.type == Block3D.BlockType.KILL_BRICK
                val isCheckPoint = block.type == Block3D.BlockType.CHECKPOINT
                val isCoin = block.type == Block3D.BlockType.GOLD_COIN

                val baseColor = if (isKillBrick) {
                    Color(0xFFFF1E1E)
                } else if (isCoin) {
                    Color(0xFFFFD400)
                } else if (isCheckPoint) {
                    Color(0xFF00FF77)
                } else {
                    block.color
                }

                // Shaded colors for faces
                val topColor = baseColor.lighten(0.18f)
                val frontColor = baseColor
                val sideColor = baseColor.darken(0.20f)

                // Draw solid polygons for visible faces!
                // 1. Bottom floor/top face (Y = positive height)
                drawPolygon(listOf(fTL, fTR, bTR, bTL), topColor)

                // For coins, let's draw them as rotating octahedrons
                if (isCoin) {
                    // Draw coin spin indicator
                    val spin = (animProgress.value * 2 * Math.PI).toFloat()
                    val cx = (fTL.x + fTR.x + bTL.x + bTR.x) / 4f
                    val cy = (fTL.y + fBL.y) / 2f
                    val coinRadius = 14f
                    val x1 = cx + cos(spin) * coinRadius
                    val x2 = cx - cos(spin) * coinRadius
                    val yTop = cy - coinRadius * 1.3f
                    val yBottom = cy + coinRadius * 1.3f

                    drawPolygon(listOf(Offset(x1, cy), Offset(cx, yTop), Offset(x2, cy), Offset(cx, yBottom)), Color(0xFFFFD700))
                    // Shiny sparkle
                    drawCircle(Color.White.copy(alpha = 0.8f), 3f, Offset(cx - 3f, cy - 4f))
                    continue
                }

                // 2. Frontend face
                drawPolygon(listOf(fTL, fTR, fBR, fBL), frontColor)

                // 3. Side faces
                drawPolygon(listOf(fTR, bTR, bBR, fBR), sideColor)
                drawPolygon(listOf(fTL, bTL, bBL, fBL), sideColor)

                // Draw classic Roblox stud grid on top face! (Highly Nostalgic Detail)
                if (block.type == Block3D.BlockType.NORMAL || block.type == Block3D.BlockType.CHECKPOINT) {
                    // Draw simple studs as small dark grey circles
                    val strideX = block.width / 4f
                    val strideZ = block.depth / 4f
                    for (ix in 1..3) {
                        for (iz in 1..3) {
                            val studX = block.x - halfW + ix * strideX
                            val studZ = block.z - halfD + iz * strideZ
                            val studProj = project3d(studX, block.y + halfH, studZ, width, height, playerZ)
                            
                            // Projection scaling factor
                            val relativeZ = studZ - playerZ
                            val studsScale = (180f / (relativeZ + 200f).coerceAtLeast(10f)).coerceAtLeast(1f)
                            
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.08f),
                                radius = 6f * studsScale,
                                center = studProj
                            )
                        }
                    }
                }

                // If checkpoint, draw flag pole as simple aesthetic custom canvas overlay
                if (isCheckPoint) {
                    val poleBase = project3d(block.x, block.y + halfH, block.z, width, height, playerZ)
                    val poleTop = project3d(block.x, block.y + halfH + 40f, block.z, width, height, playerZ)
                    drawLine(Color.White, poleBase, poleTop, strokeWidth = 5f)
                    
                    val flagPoint = project3d(block.x + 20f, block.y + halfH + 30f, block.z, width, height, playerZ)
                    val flagBottom = project3d(block.x, block.y + halfH + 20f, block.z, width, height, playerZ)
                    val flagTop = project3d(block.x, block.y + halfH + 30f, block.z, width, height, playerZ)
                    drawPolygon(listOf(flagTop, flagPoint, flagBottom), Color.Red)
                }

                // If kill brick, draw a glowing red laser stroke around it
                if (isKillBrick) {
                    val strokeColor = Color(0xFFFF9999).copy(alpha = 0.6f + sin(animProgress.value * 10f) * 0.3f)
                    drawLine(strokeColor, fTL, fTR, strokeWidth = 3f)
                    drawLine(strokeColor, fBL, fBR, strokeWidth = 3f)
                    drawLine(strokeColor, fTR, bTR, strokeWidth = 3f)
                }
            }

            // 3. Draw Player 3D Character Avatar (Centered in screen but with local X coordinate offset)
            val pTL = project3d(playerX - 10f, playerY + 12f, playerZ - 10f, width, height, playerZ)
            val pTR = project3d(playerX + 10f, playerY + 12f, playerZ - 10f, width, height, playerZ)
            val pBL = project3d(playerX - 10f, playerY - 14f, playerZ - 10f, width, height, playerZ)
            val pBR = project3d(playerX + 10f, playerY - 14f, playerZ - 10f, width, height, playerZ)

            // Let's draw Head, Torso, Legs separately for beautiful roblox look!
            val angle = if (oofAlertActive) oofRotation else 0f
            
            // Draw Torso (Bright blue block)
            val torsoTopLeft = project3d(playerX - 10f, playerY + 3f, playerZ - 5f, width, height, playerZ)
            val torsoBottomRight = project3d(playerX + 10f, playerY - 7f, playerZ - 5f, width, height, playerZ)
            drawRect(
                color = Color(0xFF0D5FCD), // Bright Classic BlueTorso
                topLeft = Offset(torsoTopLeft.x, torsoTopLeft.y),
                size = Size(kotlin.math.abs(torsoBottomRight.x - torsoTopLeft.x).coerceAtLeast(8f), kotlin.math.abs(torsoBottomRight.y - torsoTopLeft.y).coerceAtLeast(10f))
            )

            // Draw Legs (Green block)
            val legsTop = project3d(playerX - 8f, playerY - 7f, playerZ - 5f, width, height, playerZ)
            val legsBottom = project3d(playerX + 8f, playerY - 15f, playerZ - 5f, width, height, playerZ)
            drawRect(
                color = Color(0xFF1E8D1E), // Classic Green Leg color
                topLeft = Offset(legsTop.x, legsTop.y),
                size = Size(kotlin.math.abs(legsBottom.x - legsTop.x).coerceAtLeast(6f), kotlin.math.abs(legsBottom.y - legsTop.y).coerceAtLeast(6f))
            )

            // Draw Yellow Head
            val headTopLeft = project3d(playerX - 6f, playerY + 12f, playerZ - 5f, width, height, playerZ)
            val headBottomRight = project3d(playerX + 6f, playerY + 3f, playerZ - 5f, width, height, playerZ)
            val headW = kotlin.math.abs(headBottomRight.x - headTopLeft.x).coerceAtLeast(10f)
            val headH = kotlin.math.abs(headBottomRight.y - headTopLeft.y).coerceAtLeast(10f)
            
            drawRect(
                color = Color(0xFFFFD400), // Bright Classic yellow head!
                topLeft = Offset(headTopLeft.x, headTopLeft.y),
                size = Size(headW, headH)
            )

            // Draw smiley faces on head
            val eyeX1 = headTopLeft.x + headW * 0.3f
            val eyeX2 = headTopLeft.x + headW * 0.7f
            val eyeY = headTopLeft.y + headH * 0.35f
            val mouthY = headTopLeft.y + headH * 0.7f
            
            // Draw classic smiley eyes
            drawCircle(Color.Black, 1.8f, Offset(eyeX1, eyeY))
            drawCircle(Color.Black, 1.8f, Offset(eyeX2, eyeY))
            
            // Draw curve face smile
            drawArc(
                color = Color.Black,
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(headTopLeft.x + headW * 0.25f, headTopLeft.y + headH * 0.4f),
                size = Size(headW * 0.5f, headH * 0.32f),
                style = Stroke(width = 1.5f)
            )

            // 4. Draw cosmetics!
            // FREE Fedora (Equipped hat) requested by user!
            if (user.equippedHat == "classic_fedora") {
                val fedoraBaseY = headTopLeft.y + 1f
                // Base Rim
                drawRect(
                    color = Color(0xFF1C1C1C), // black charcoal
                    topLeft = Offset(headTopLeft.x - 5f, fedoraBaseY),
                    size = Size(headW + 10f, 3f)
                )
                // Crown
                drawRect(
                    color = Color(0xFF121212),
                    topLeft = Offset(headTopLeft.x - 1f, fedoraBaseY - 6f),
                    size = Size(headW + 2f, 7f)
                )
                // Red strip on fedora (Classic detail)
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(headTopLeft.x - 1f, fedoraBaseY - 1f),
                    size = Size(headW + 2f, 1.5f)
                )
            } else if (user.equippedHat == "valkyrie_helm") {
                val helmBaseY = headTopLeft.y + 2f
                // Shield crown
                drawRect(Color(0xFFE2E2E2), Offset(headTopLeft.x - 1f, helmBaseY - 3f), Size(headW + 2f, 5f))
                // Beautiful wings left and right
                val leftWing = Path().apply {
                    moveTo(headTopLeft.x - 1f, helmBaseY)
                    lineTo(headTopLeft.x - 6f, helmBaseY - 11f)
                    lineTo(headTopLeft.x - 3f, helmBaseY - 13f)
                    lineTo(headTopLeft.x, helmBaseY - 4f)
                    close()
                }
                val rightWing = Path().apply {
                    moveTo(headTopLeft.x + headW + 1f, helmBaseY)
                    lineTo(headTopLeft.x + headW + 6f, helmBaseY - 11f)
                    lineTo(headTopLeft.x + headW + 3f, helmBaseY - 13f)
                    lineTo(headTopLeft.x + headW, helmBaseY - 4f)
                    close()
                }
                drawPath(leftWing, Color(0xFFFFE066))
                drawPath(rightWing, Color(0xFFFFE066))
            } else if (user.equippedHat == "dominus") {
                // Dominus black hood
                drawRect(
                    color = Color(0xFF2E0854), // Dominus purple
                    topLeft = Offset(headTopLeft.x - 3f, headTopLeft.y - 2f),
                    size = Size(headW + 6f, headH + 4f)
                )
                // Black hollow center face
                drawRect(
                    color = Color(0xFF111111),
                    topLeft = Offset(headTopLeft.x + 1f, headTopLeft.y + 2f),
                    size = Size(headW - 2f, headH - 1f)
                )
                // Golden rings at bottom of hood
                drawCircle(Color(0xFFFFD700), 2.5f, Offset(headTopLeft.x + 1f, headTopLeft.y + headH))
                drawCircle(Color(0xFFFFD700), 2.5f, Offset(headTopLeft.x + headW - 1f, headTopLeft.y + headH))
            } else if (user.equippedHat == "gold_crown") {
                val crownBaseY = headTopLeft.y
                val crownPath = Path().apply {
                    moveTo(headTopLeft.x - 1f, crownBaseY)
                    lineTo(headTopLeft.x - 1f, crownBaseY - 5f)
                    lineTo(headTopLeft.x + headW * 0.25f, crownBaseY - 2f)
                    lineTo(headTopLeft.x + headW * 0.5f, crownBaseY - 6f)
                    lineTo(headTopLeft.x + headW * 0.75f, crownBaseY - 2f)
                    lineTo(headTopLeft.x + headW + 1f, crownBaseY - 5f)
                    lineTo(headTopLeft.x + headW + 1f, crownBaseY)
                    close()
                }
                drawPath(crownPath, Color(0xFFFFD700))
                // Gems
                drawCircle(Color.Red, 1f, Offset(headTopLeft.x + headW * 0.5f, crownBaseY - 5f))
            } else if (user.equippedHat == "default_hat") {
                // Classic blue visor cap
                drawRect(
                    color = Color(0xFF0044BB), // Dark blue
                    topLeft = Offset(headTopLeft.x - 1f, headTopLeft.y - 1f),
                    size = Size(headW + 2f, 4f)
                )
                // Visor rim
                drawRect(
                    color = Color(0xFF00AAFF),
                    topLeft = Offset(headTopLeft.x + 2f, headTopLeft.y + 1f),
                    size = Size(headW - 4f, 2f)
                )
            }

            // Draw Classic Sword if equipped!
            if (user.ownedCosmetics.contains("classic_sword")) {
                val armX = headTopLeft.x + headW + 4f
                val armY = headTopLeft.y + headH * 1.5f
                
                // Draw arm holding sword
                drawLine(Color(0xFFFFD400), Offset(armX - 6f, armY), Offset(armX + 2f, armY - 3f), strokeWidth = 5f)
                
                // Draw classic sword blade
                val swordPath = Path().apply {
                    moveTo(armX + 2f, armY - 3f)
                    lineTo(armX + 18f, armY - 19f)
                    lineTo(armX + 20f, armY - 17f)
                    lineTo(armX + 4f, armY - 1f)
                    close()
                }
                drawPath(swordPath, Color(0xFFD4D4E8)) // light steel
                // Hilt
                drawLine(Color(0xFF8B4513), Offset(armX + 1f, armY - 2f), Offset(armX - 1f, armY + 1f), strokeWidth = 4f)
                drawCircle(Color(0xFFFFD700), 2f, Offset(armX - 1f, armY + 2f))
            }

            // Draw Coils around torso
            if (user.equippedCoil == "speed_coil" || user.equippedCoil == "gravity_coil") {
                val coilColor = if (user.equippedCoil == "speed_coil") Color(0xFFFFE30D) else Color(0xFF1EBBFF)
                val cx = torsoTopLeft.x + (torsoBottomRight.x - torsoTopLeft.x) / 2f
                val cy = torsoTopLeft.y + (torsoBottomRight.y - torsoTopLeft.y) / 2f
                val cw = kotlin.math.abs(torsoBottomRight.x - torsoTopLeft.x) + 4f
                val ch = kotlin.math.abs(torsoBottomRight.y - torsoTopLeft.y) / 4f

                // Glowing coil spirals
                for (j in 0..2) {
                    val sy = torsoTopLeft.y + j * ch * 1.5f + sin(animProgress.value * 6f + j) * 2f
                    drawArc(
                        color = coilColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(cx - cw/2, sy),
                        size = Size(cw, 6f),
                        style = Stroke(width = 2.5f)
                    )
                }
            }

            // OOF screen flash red overlay on damage
            if (oofAlertActive) {
                drawRect(color = Color.Red.copy(alpha = 0.28f))
            }
        }

        // Overlay Game HUD Layer
        // Top HUD indicators (Classic green health bar on right, Robux & Scores on left)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Stats (Robux + Score)
            Column(
                modifier = Modifier.padding(6.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Coin icon / Ticket icon
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color(0xFFFFD500), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Robux Collected: R$ ${viewModel.activeRobuxCollected.collectAsState().value}",
                                color = Color.White,
                                fontSize = 14.sp,
                                style = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Score: ${viewModel.activeScore.collectAsState().value} pts",
                            color = Color(0xFFA6E2FF),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Green/White Classic Health Bar Section
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${user.username}'s Health",
                    color = Color.White,
                    fontSize = 11.sp,
                    style = TextStyle(
                        shadow = Shadow(color = Color.Black, blurRadius = 2f, offset = Offset(1f, 1f))
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                val hp = viewModel.activeHp.collectAsState().value
                // Classic segmented striped Health Bar
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(18.dp)
                        .background(Color.White, shape = MaterialTheme.shapes.extraSmall)
                        .padding(2.dp)
                ) {
                    val fillPercent = hp / 100f
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fillPercent)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF32CD32), Color(0xFF00FF00))
                                )
                            )
                    )
                }
            }
        }

        // Joystick overlay for gameplay (Movement buttons: Left, Right, Jump)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Right Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .size(70.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        leftPressed = true
                                        tryAwaitRelease()
                                        leftPressed = false
                                    }
                                )
                            }
                            .testTag("key_left_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.55f)),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("◀", color = Color.White, fontSize = 28.sp)
                    }

                    Button(
                        onClick = {},
                        modifier = Modifier
                            .size(70.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        rightPressed = true
                                        tryAwaitRelease()
                                        rightPressed = false
                                    }
                                )
                            }
                            .testTag("key_right_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.55f)),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("▶", color = Color.White, fontSize = 28.sp)
                    }
                }

                // Jump button
                Button(
                    onClick = { jumpTriggered = true },
                    modifier = Modifier
                        .size(80.dp)
                        .testTag("key_jump_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE63946)),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("JUMP", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active State screens (GameOver overlays, Pause triggers, Exit Button)
        IconButton(
            onClick = { viewModel.endObbyGame() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
                .testTag("exit_game_back")
        ) {
            Text("X Leave Game", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.background(Color.White.copy(alpha = 0.8f), shape = MaterialTheme.shapes.small).padding(6.dp))
        }

        // Floating particle triggers
        if (oofAlertActive) {
            Text(
                text = "OOF!",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color.Red,
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("oof_pop_up")
            )
        }

        // DOUBLE JUMP POP UP BLAST
        if (showDoubleJumpBurst) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xCC00AAFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("★ DOUBLE JUMP BLAST ★", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }

        // RETRO PORTAL MULTIPLAYER CHAT CONSOLE
        val chatMessages by viewModel.chatMessages.collectAsState()
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 110.dp, start = 12.dp)
                .width(250.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MULTIPLAYER CONSOLE", color = Color(0xFFFFCC00), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                IconButton(
                    onClick = { chatBoxOpen = !chatBoxOpen },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (chatBoxOpen) Icons.Default.Close else Icons.Default.Email,
                        contentDescription = "Chat",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            chatMessages.takeLast(4).forEach { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                ) {
                    Text(
                        text = "${msg.sender}: ",
                        fontWeight = FontWeight.Black,
                        color = if (msg.isSystem) Color.Cyan else Color(0xFF9FFFCB),
                        fontSize = 11.sp
                    )
                    Text(text = msg.text, color = Color.White, fontSize = 11.sp)
                }
            }
            if (chatBoxOpen) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = chatInputText,
                        onValueChange = { chatInputText = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 10.sp),
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                            .padding(4.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00AAFF), RoundedCornerShape(3.dp))
                            .clickable {
                                if (chatInputText.isNotBlank()) {
                                    viewModel.sendChatMessage(chatInputText)
                                    chatInputText = ""
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("SEND", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ANTI-CHEAT MODAL SIRENS WARNING DISPLAY OVERLAY
        val exploitDetected by viewModel.exploitDetected.collectAsState()
        if (exploitDetected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = 0.90f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.width(320.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Cheat Warn",
                            tint = Color.Red,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "RETRO GUARD ANTI-CHEAT",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Memory mutation warning! Hack Injection Code 0x88F. Speedhack or Gravity/Noclip bypass detected by Server security filter.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.clearCheatAlert() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Disable Hacks & Reset Client", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// Projection helper of custom 3D system
fun project3d(x: Float, y: Float, z: Float, width: Float, height: Float, playerZ: Float): Offset {
    val relativeZ = z - playerZ // Distance forward from player
    val scale = 400f / (relativeZ + 220f).coerceAtLeast(15f)
    
    val centerX = width / 2f
    val centerY = height * 0.70f // slightly look down
    
    val sx = centerX + x * scale
    val sy = centerY - y * scale // positive Y goes UP
    
    return Offset(sx, sy)
}

// Shading utility extensions of color models
fun Color.lighten(percent: Float): Color {
    return Color(
        red = (red + percent).coerceIn(0f, 1f),
        green = (green + percent).coerceIn(0f, 1f),
        blue = (blue + percent).coerceIn(0f, 1f),
        alpha = alpha
    )
}

fun Color.darken(percent: Float): Color {
    return Color(
        red = (red - percent).coerceIn(0f, 1f),
        green = (green - percent).coerceIn(0f, 1f),
        blue = (blue - percent).coerceIn(0f, 1f),
        alpha = alpha
    )
}

// Fast generic polygon drawer
fun DrawScope.drawPolygon(points: List<Offset>, color: Color) {
    if (points.size < 3) return
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
        close()
    }
    drawPath(path, color)
}
