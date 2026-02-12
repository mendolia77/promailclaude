package com.smartmail.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmail.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val emoji: String,
    val gradient: List<Color>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pages = remember {
        listOf(
            OnboardingPage(
                title = "Organizzazione Automatica",
                description = "Le tue email vengono categorizzate automaticamente in base a filtri intelligenti. Niente più caos!",
                emoji = "🎯",
                gradient = listOf(DeepPurple, IndigoBlue)
            ),
            OnboardingPage(
                title = "Categorie Personalizzate",
                description = "Crea le tue categorie personalizzate con filtri potenti. Lavoro, Personale, Shopping e molto altro!",
                emoji = "📁",
                gradient = listOf(ElectricBlue, TurquoiseBlue)
            ),
            OnboardingPage(
                title = "Focus sul Importante",
                description = "Ricevi notifiche solo per le email che contano davvero. Massima produttività, zero distrazioni!",
                emoji = "⚡",
                gradient = listOf(CoralOrange, BrightPink)
            ),
            OnboardingPage(
                title = "Design Spettacolare",
                description = "Un'interfaccia moderna e fluida che rende la gestione delle email un piacere!",
                emoji = "✨",
                gradient = listOf(NeonPink, MagentaPurple)
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                OnboardingPageContent(pages[page], page)
            }

            // Indicatori e pulsanti
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicatori pagina
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    pages.indices.forEach { index ->
                        PageIndicator(
                            isSelected = pagerState.currentPage == index,
                            color = pages[index].gradient[0]
                        )
                    }
                }

                // Pulsanti
                if (pagerState.currentPage == pages.lastIndex) {
                    // Ultima pagina - pulsante "Inizia"
                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepPurple
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Inizia Ora! 🚀",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Altre pagine - pulsanti Avanti e Salta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = onComplete,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Salta",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier
                                .weight(2f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = pages[pagerState.currentPage].gradient[0]
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Avanti",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage, pageIndex: Int) {
    // Animazioni
    val scale by rememberInfiniteTransition(label = "scale_$pageIndex").animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_scale"
    )

    val rotation by rememberInfiniteTransition(label = "rotation_$pageIndex").animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_rotation"
    )

    // Particelle animate
    val particleY1 by rememberInfiniteTransition(label = "particle1_$pageIndex").animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particle_y1"
    )

    val particleY2 by rememberInfiniteTransition(label = "particle2_$pageIndex").animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particle_y2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        // Particelle decorative di sfondo
        Box(
            modifier = Modifier
                .offset(x = 30.dp, y = (100 + particleY1).dp)
                .size(60.dp)
                .alpha(0.15f)
                .background(
                    color = page.gradient[0],
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-40).dp, y = (80 + particleY2).dp)
                .size(80.dp)
                .alpha(0.1f)
                .background(
                    color = page.gradient[1],
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 50.dp, y = (-150 - particleY1).dp)
                .size(70.dp)
                .alpha(0.12f)
                .background(
                    color = page.gradient[0],
                    shape = CircleShape
                )
        )

        // Contenuto principale
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Card con gradient per l'emoji
            Card(
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale),
                shape = RoundedCornerShape(40.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = page.gradient
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = page.emoji,
                        fontSize = 100.sp,
                        modifier = Modifier.scale(scale)
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Titolo
            Text(
                text = page.title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Descrizione
            Text(
                text = page.description,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun PageIndicator(
    isSelected: Boolean,
    color: Color
) {
    val width by animateDpAsState(
        targetValue = if (isSelected) 32.dp else 8.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "indicator_width"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.3f,
        animationSpec = tween(300),
        label = "indicator_alpha"
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(8.dp)
            .alpha(alpha)
            .background(
                color = color,
                shape = RoundedCornerShape(4.dp)
            )
    )
}
