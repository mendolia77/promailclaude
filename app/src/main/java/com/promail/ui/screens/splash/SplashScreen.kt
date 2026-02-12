package com.smartmail.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmail.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    // Animazioni
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")

    // Gradient animato
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_offset"
    )

    // Scala logo
    var logoScale by remember { mutableStateOf(0f) }
    val logoScaleAnim by animateFloatAsState(
        targetValue = logoScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    // Alpha logo
    var logoAlpha by remember { mutableStateOf(0f) }
    val logoAlphaAnim by animateFloatAsState(
        targetValue = logoAlpha,
        animationSpec = tween(800),
        label = "logo_alpha"
    )

    // Alpha testo
    var textAlpha by remember { mutableStateOf(0f) }
    val textAlphaAnim by animateFloatAsState(
        targetValue = textAlpha,
        animationSpec = tween(600, delayMillis = 400),
        label = "text_alpha"
    )

    // Alpha tagline
    var taglineAlpha by remember { mutableStateOf(0f) }
    val taglineAlphaAnim by animateFloatAsState(
        targetValue = taglineAlpha,
        animationSpec = tween(600, delayMillis = 800),
        label = "tagline_alpha"
    )

    // Particelle fluttuanti
    val particle1Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -50f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particle1"
    )

    val particle2Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particle2"
    )

    // Effetto pulsazione
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Avvia animazioni in sequenza
    LaunchedEffect(Unit) {
        delay(100)
        logoAlpha = 1f
        logoScale = 1f

        delay(400)
        textAlpha = 1f

        delay(400)
        taglineAlpha = 1f

        // Durata totale splash
        delay(2000)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        DeepPurple,
                        IndigoBlue,
                        ElectricBlue,
                        DeepPurple
                    ),
                    start = androidx.compose.ui.geometry.Offset(gradientOffset, gradientOffset),
                    end = androidx.compose.ui.geometry.Offset(
                        gradientOffset + 1000f,
                        gradientOffset + 1000f
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Particelle decorative fluttuanti
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Particella 1 - top left
            Box(
                modifier = Modifier
                    .offset(x = 50.dp, y = (100 + particle1Y).dp)
                    .size(80.dp)
                    .alpha(0.1f)
                    .background(
                        color = Color.White,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )

            // Particella 2 - top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-80).dp, y = (150 + particle2Y).dp)
                    .size(120.dp)
                    .alpha(0.08f)
                    .background(
                        color = Color.White,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )

            // Particella 3 - bottom left
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 100.dp, y = (-200 - particle1Y).dp)
                    .size(100.dp)
                    .alpha(0.06f)
                    .background(
                        color = Color.White,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )

            // Particella 4 - bottom right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-50).dp, y = (-100 - particle2Y).dp)
                    .size(90.dp)
                    .alpha(0.07f)
                    .background(
                        color = Color.White,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }

        // Contenuto centrale
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo emoji con glow effect
            Box(
                modifier = Modifier
                    .scale(logoScaleAnim * pulseScale)
                    .alpha(logoAlphaAnim)
            ) {
                // Glow esterno
                Text(
                    text = "✉️",
                    fontSize = 120.sp,
                    modifier = Modifier
                        .alpha(0.3f)
                        .scale(1.2f)
                )

                // Logo principale
                Text(
                    text = "✉️",
                    fontSize = 120.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Nome app
            Text(
                text = "SmartMail",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .alpha(textAlphaAnim)
                    .scale(if (textAlpha > 0) pulseScale else 1f),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Email organizzate. Tempo risparmiato.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.alpha(taglineAlphaAnim)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading indicator stilizzato
            Box(
                modifier = Modifier
                    .alpha(taglineAlphaAnim)
            ) {
                LoadingDots()
            }
        }
    }
}

@Composable
fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .alpha(dot1Alpha)
                .background(
                    color = Color.White,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .alpha(dot2Alpha)
                .background(
                    color = Color.White,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .alpha(dot3Alpha)
                .background(
                    color = Color.White,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
    }
}
