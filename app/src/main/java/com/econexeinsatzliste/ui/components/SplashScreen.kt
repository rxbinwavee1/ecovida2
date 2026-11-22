package com.econexeinsatzliste.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.econexeinsatzliste.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Stage 1: Intro (Fade In & Bounce)
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(80f) }
    val textAlpha = remember { Animatable(0f) }

    // Stage 2: Outro (Zoom Out / "Flying out of screen")
    val globalScale = remember { Animatable(1f) }
    val globalAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // --- PHASE 1: INTRO (Bounce in) ---
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)
            )
        }

        delay(200)

        launch {
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
        launch {
            textOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }

        // Hold the screen to read
        delay(900)

        // --- PHASE 2: OUTRO (Zoom out & fly into the app) ---
        launch {
            globalAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }
        launch {
            globalScale.animateTo(
                targetValue = 4.0f, // Zooms out massively towards the user / out of screen
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }

        delay(350)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)),
        contentAlignment = Alignment.Center
    ) {
        // Wrapper for the Outro Zoom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(globalScale.value)
                .alpha(globalAlpha.value),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Logo
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF111827),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Econex Planer Logo",
                        modifier = Modifier
                            .size(130.dp)
                            .padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Animated Text Block
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset { IntOffset(0, textOffsetY.value.roundToInt()) }
                        .alpha(textAlpha.value)
                ) {
                    Text(
                        text = "Econex Planer",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Einsatz- & Dienstplaner",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "created by rxbin_.wave",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF38BDF8),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
