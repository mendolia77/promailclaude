package com.smartmail.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmail.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBack: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Analytics,
                            contentDescription = null,
                            tint = DeepPurple
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Dashboard",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + slideInVertically(),
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    TimesSavedCard(animationDelay = 0)
                }

                item {
                    EmailStatsOverview(animationDelay = 200)
                }

                item {
                    CategoryBreakdownCard(animationDelay = 400)
                }

                item {
                    ProductivityInsightsCard(animationDelay = 600)
                }

                item {
                    WeeklyActivityCard(animationDelay = 800)
                }
            }
        }
    }
}

@Composable
fun TimesSavedCard(animationDelay: Int = 0) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    // Animazione contatore
    var targetMinutes by remember { mutableStateOf(0) }
    val animatedMinutes by animateIntAsState(
        targetValue = targetMinutes,
        animationSpec = tween(1500, easing = EaseOutCubic),
        label = "minutes"
    )

    LaunchedEffect(isVisible) {
        if (isVisible) {
            targetMinutes = 127 // Minuti risparmiati questa settimana
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                DeepPurple.copy(alpha = 0.15f),
                                IndigoBlue.copy(alpha = 0.1f),
                                ElectricBlue.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⏱️",
                        fontSize = 48.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$animatedMinutes",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepPurple
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "min",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tempo risparmiato questa settimana",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SmallStat(
                            icon = "📧",
                            value = "847",
                            label = "Email gestite"
                        )
                        SmallStat(
                            icon = "✅",
                            value = "94%",
                            label = "Auto-categorizzate"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmallStat(icon: String, value: String, label: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DeepPurple
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun EmailStatsOverview(animationDelay: Int = 0) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Panoramica Email",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Filled.Inbox,
                        value = "47",
                        label = "Non lette",
                        color = IndigoBlue,
                        animationDelay = 0
                    )
                    StatItem(
                        icon = Icons.Filled.Star,
                        value = "12",
                        label = "Importanti",
                        color = CoralOrange,
                        animationDelay = 100
                    )
                    StatItem(
                        icon = Icons.Filled.CheckCircle,
                        value = "286",
                        label = "Lette oggi",
                        color = FolderPersonal,
                        animationDelay = 200
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    animationDelay: Int = 0
) {
    var targetValue by remember { mutableStateOf(0) }
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(1000, delayMillis = animationDelay, easing = EaseOutCubic),
        label = "stat_value"
    )

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        targetValue = value.toIntOrNull() ?: 0
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (value.contains("%")) value else animatedValue.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )

        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun CategoryBreakdownCard(animationDelay: Int = 0) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    val categories = remember {
        listOf(
            CategoryData("🏢 Lavoro", 156, FolderWork),
            CategoryData("👥 Personale", 89, FolderPersonal),
            CategoryData("🛒 Shopping", 45, FolderShopping),
            CategoryData("📰 Newsletter", 287, FolderNewsletter),
            CategoryData("📎 Allegati", 67, FolderAttachments)
        )
    }

    val totalEmails = categories.sumOf { it.count }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Distribuzione Categorie",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$totalEmails email",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Donut chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DonutChart(
                        categories = categories,
                        totalEmails = totalEmails
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Legend
                categories.forEachIndexed { index, category ->
                    CategoryLegendItem(
                        category = category,
                        percentage = (category.count.toFloat() / totalEmails * 100).toInt(),
                        animationDelay = index * 100
                    )
                    if (index < categories.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    categories: List<CategoryData>,
    totalEmails: Int
) {
    var animationProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        animationProgress = 1f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(1500, easing = EaseOutCubic),
        label = "donut_progress"
    )

    Canvas(modifier = Modifier.size(180.dp)) {
        val strokeWidth = 40.dp.toPx()
        var startAngle = -90f

        categories.forEach { category ->
            val sweepAngle = (category.count.toFloat() / totalEmails * 360f) * animatedProgress

            drawArc(
                color = category.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )

            startAngle += (category.count.toFloat() / totalEmails * 360f)
        }
    }
}

@Composable
fun CategoryLegendItem(
    category: CategoryData,
    percentage: Int,
    animationDelay: Int = 0
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(category.color)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = category.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${category.count}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = category.color
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "$percentage%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun ProductivityInsightsCard(animationDelay: Int = 0) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    val insights = remember {
        listOf(
            InsightItem(
                icon = "🔥",
                title = "Ottima gestione!",
                description = "Rispondi al 85% delle email entro 2 ore",
                color = CoralOrange
            ),
            InsightItem(
                icon = "🎯",
                title = "Focus migliorato",
                description = "Le categorie riducono il tempo di ricerca del 40%",
                color = IndigoBlue
            ),
            InsightItem(
                icon = "⚡",
                title = "Più efficiente",
                description = "Leggi 30% di email in meno grazie ai filtri smart",
                color = FolderPersonal
            )
        )
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "💡 Insights di Produttività",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                insights.forEachIndexed { index, insight ->
                    InsightCard(
                        insight = insight,
                        animationDelay = index * 150
                    )
                    if (index < insights.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun InsightCard(insight: InsightItem, animationDelay: Int = 0) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = insight.color.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = insight.icon,
                    fontSize = 32.sp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = insight.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = insight.color
                    )
                    Text(
                        text = insight.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyActivityCard(animationDelay: Int = 0) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    val weekDays = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")
    val emailCounts = listOf(45, 67, 53, 78, 92, 34, 21)

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "📊 Attività Settimanale",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    emailCounts.forEachIndexed { index, count ->
                        DayBar(
                            day = weekDays[index],
                            count = count,
                            maxCount = emailCounts.maxOrNull() ?: 100,
                            animationDelay = index * 100
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayBar(day: String, count: Int, maxCount: Int, animationDelay: Int = 0) {
    var animationProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        animationProgress = 1f
    }

    val animatedHeight by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bar_height"
    )

    val heightFraction = (count.toFloat() / maxCount).coerceAtLeast(0.1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = DeepPurple
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .width(28.dp)
                .height((100.dp * heightFraction * animatedHeight).coerceAtLeast(10.dp))
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ElectricBlue, DeepPurple)
                    )
                )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = day,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// Data classes
data class CategoryData(
    val name: String,
    val count: Int,
    val color: Color
)

data class InsightItem(
    val icon: String,
    val title: String,
    val description: String,
    val color: Color
)
