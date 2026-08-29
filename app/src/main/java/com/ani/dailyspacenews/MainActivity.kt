package com.ani.dailyspacenews

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.ani.dailyspacenews.billing.BillingRepository
import com.ani.dailyspacenews.data.*
import com.ani.dailyspacenews.ui.billing.PaywallSheet
import com.ani.dailyspacenews.ui.components.ObservatoryCard
import com.ani.dailyspacenews.ui.events.EventsScreen
import com.ani.dailyspacenews.ui.theme.*
import com.ani.dailyspacenews.util.AppReviewManager
import com.ani.dailyspacenews.util.ConsentManager
import com.ani.dailyspacenews.util.NotificationScheduler
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

// --- DATA MODELS ---
data class ApodResponse(
    val title: String,
    val url: String,
    val hdurl: String?,
    val explanation: String?,
    val date: String?
)

data class NewsArticle(
    val id: Int,
    val title: String,
    val url: String,
    val summary: String,
    @SerializedName("image_url") val image_url: String?,
    @SerializedName("published_at") val published_at: String,
    @SerializedName("news_site") val news_site: String
)

data class Launch(
    val id: String,
    val name: String,
    @SerializedName("window_start") val window_start: String,
    val image: String?,
    val mission: Mission?,
    val pad: Pad?
)

data class Mission(val name: String?, val description: String?)
data class Pad(val location: Location?)
data class Location(val name: String?)

// --- NAVIGATION ROUTES ---
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Outlined.Home)
    object Launches : Screen("launches", "Launches", Icons.Outlined.RocketLaunch)
    object Gallery : Screen("gallery", "Gallery", Icons.Outlined.GridView)
    object Events : Screen("events", "Events", Icons.Outlined.Event)
    object More : Screen("more", "More", Icons.Outlined.MoreHoriz)
    object ImagePreview : Screen("preview", "Preview", Icons.Outlined.Image)
}

val navigationItems = listOf(Screen.Home, Screen.Launches, Screen.Gallery, Screen.Events, Screen.More)

class MainActivity : ComponentActivity() {

    private lateinit var adManager: LevelPlayAdManager
    private lateinit var billingRepository: BillingRepository
    private lateinit var consentManager: ConsentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        RetrofitClient.init(cacheDir)
        NotificationScheduler.createNotificationChannels(this)

        consentManager = ConsentManager(applicationContext)
        adManager = LevelPlayAdManager(this)
        billingRepository = BillingRepository(applicationContext, lifecycleScope)

        // Observe premium state to update ad manager
        lifecycleScope.launch {
            billingRepository.isPremiumUser.collect { isPremium ->
                adManager.isPremiumUser = isPremium
            }
        }

        // Request UMP Consent and initialize ads if permitted
        consentManager.gatherConsent(this) { canRequestAds ->
            adManager.init(canRequestAds)
        }

        // Contextual review check
        lifecycleScope.launch {
            AppReviewManager.incrementAndRequestReviewIfQualified(this@MainActivity)
        }

        setContent {
            DailySpaceNewsTheme {
                MainScreen(
                    billingRepository = billingRepository,
                    consentManager = consentManager,
                    adManager = adManager
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        adManager.onResume()
        billingRepository.checkEntitlements()
    }

    override fun onPause() {
        super.onPause()
        adManager.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        adManager.onDestroy()
    }
}

@Composable
fun MainScreen(
    billingRepository: BillingRepository,
    consentManager: ConsentManager,
    adManager: LevelPlayAdManager
) {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val context = LocalContext.current
    var showPaywallSheet by remember { mutableStateOf(false) }

    // Consolidated single startup fetch
    LaunchedEffect(Unit) {
        homeViewModel.fetchData(context)
    }

    Scaffold(
        containerColor = BgBase,
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().background(BgBase)) {
                AppBottomNavigationBar(navController)
                BannerAdView(adManager, billingRepository, consentManager)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgBase)
        ) {
            AppNavigationHost(
                navController = navController,
                homeViewModel = homeViewModel,
                billingRepository = billingRepository,
                consentManager = consentManager,
                onOpenPaywall = { showPaywallSheet = true }
            )

            if (showPaywallSheet) {
                PaywallSheet(
                    billingRepository = billingRepository,
                    onDismiss = { showPaywallSheet = false }
                )
            }
        }
    }
}

@Composable
fun BannerAdView(
    adManager: LevelPlayAdManager,
    billingRepository: BillingRepository,
    consentManager: ConsentManager
) {
    val isPremium by billingRepository.isPremiumUser.collectAsState()
    val canRequestAds by consentManager.canRequestAds.collectAsState()

    if (!isPremium && canRequestAds) {
        val bannerLayout = remember { adManager.getOrCreateBannerView() }
        if (bannerLayout != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(BgBase),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            (bannerLayout.parent as? android.view.ViewGroup)?.removeView(bannerLayout)
                            addView(bannerLayout)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )
            }
        } else {
            Spacer(Modifier.height(0.dp))
        }
    } else {
        Spacer(Modifier.height(0.dp))
    }
}

@Composable
fun AppBottomNavigationBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BgElevated,
        border = BorderStroke(1.dp, BorderHairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navigationItems.forEach { screen ->
                val isSelected = currentRoute == screen.route
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) AccentAmber else TextTertiary,
                    animationSpec = tween(150),
                    label = "navColor"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = screen.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigationHost(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    billingRepository: BillingRepository,
    consentManager: ConsentManager,
    onOpenPaywall: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                homeViewModel = homeViewModel,
                onNavigateToPreview = { apod ->
                    homeViewModel.selectApod(apod)
                    navController.navigate(Screen.ImagePreview.route)
                },
                onOpenPaywall = onOpenPaywall
            )
        }
        composable(Screen.Launches.route) {
            LaunchesScreen(homeViewModel = homeViewModel)
        }
        composable(Screen.Gallery.route) {
            GalleryScreen(
                homeViewModel = homeViewModel,
                navController = navController
            )
        }
        composable(Screen.Events.route) {
            EventsScreen(homeViewModel = homeViewModel)
        }
        composable(Screen.More.route) {
            MoreScreen(
                billingRepository = billingRepository,
                consentManager = consentManager,
                onOpenPaywall = onOpenPaywall
            )
        }
        composable(Screen.ImagePreview.route) {
            ImagePreviewScreen(
                homeViewModel = homeViewModel,
                billingRepository = billingRepository,
                navController = navController,
                onOpenPaywall = onOpenPaywall
            )
        }
    }
}

// --- HOME SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onNavigateToPreview: (ApodResponse) -> Unit,
    onOpenPaywall: () -> Unit
) {
    val apodData by homeViewModel.apodData
    val newsList by homeViewModel.newsList
    val isLoading by homeViewModel.isLoading
    var searchQuery by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("All") }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Screen Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DAILY SPACE NEWS",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Cosmic Telemetry & Headlines",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Search Input
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search cosmic database...", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BgElevated,
                    unfocusedContainerColor = BgElevated,
                    focusedBorderColor = AccentAmberDim,
                    unfocusedBorderColor = BorderHairline,
                    cursorColor = AccentAmberDim,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
        }

        // Source Filter Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("All", "NASA", "SpaceX", "ESA")) { source ->
                    val isSelected = selectedSource == source
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedSource = source },
                        color = if (isSelected) BgElevated2 else BgElevated,
                        border = BorderStroke(1.dp, if (isSelected) AccentAmberDim else BorderHairline)
                    ) {
                        Text(
                            text = source,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // Featured APOD
        apodData?.let { apod ->
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(
                        text = "ASTRONOMY PICTURE OF THE DAY",
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    ObservatoryCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigateToPreview(apod) }
                    ) {
                        Column {
                            AsyncImage(
                                model = apod.url,
                                contentDescription = apod.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = apod.title,
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            apod.explanation?.let { exp ->
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = exp,
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cosmic News Feed
        item {
            Text(
                text = "TRANSMISSIONS",
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            )
        }

        val filteredNews = newsList.filter {
            (selectedSource == "All" || it.news_site.contains(selectedSource, true)) &&
            it.title.contains(searchQuery, true)
        }

        if (filteredNews.isEmpty() && !isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transmissions found", color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(filteredNews) { article ->
                ObservatoryCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                    onClick = {
                        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(article.url))
                    }
                ) {
                    Column {
                        if (!article.image_url.isNullOrEmpty()) {
                            AsyncImage(
                                model = article.image_url,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = article.news_site.uppercase(),
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatPublishedDate(article.published_at),
                                color = TextTertiary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = article.title,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (!article.summary.isNullOrEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = article.summary,
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- LAUNCHES SCREEN ---
@Composable
fun LaunchesScreen(homeViewModel: HomeViewModel) {
    val launches by homeViewModel.launchList
    val isLoading by homeViewModel.isLoading

    // Hoisted single shared ticker (pure arithmetic, zero per-item coroutines)
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase)
    ) {
        if (isLoading && launches.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = TextSecondary
            )
        } else if (launches.isEmpty()) {
            Text(
                text = "No upcoming launch telemetry found",
                modifier = Modifier.align(Alignment.Center),
                color = TextTertiary
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = "LAUNCH TELEMETRY",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Live Mission Clocks & Orbital Trajectories",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                items(launches) { launch ->
                    ObservatoryCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            if (!launch.image.isNullOrEmpty()) {
                                AsyncImage(
                                    model = launch.image,
                                    contentDescription = launch.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            Text(
                                text = launch.name,
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            launch.pad?.location?.name?.let { loc ->
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = loc,
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            launch.mission?.description?.let { desc ->
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = desc,
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Monospace Mission Clock Telemetry Readout
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = BgElevated2,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderHairline)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LaunchCountdownTelemetry(
                                        windowStart = launch.window_start,
                                        now = now
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

@Composable
fun LaunchCountdownTelemetry(windowStart: String?, now: Long) {
    if (windowStart == null) {
        Text("TBA", style = TelemetryMonoStyle, fontSize = 14.sp)
        return
    }

    val sdf = remember {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    val launchTime = remember(windowStart) {
        try { sdf.parse(windowStart)?.time ?: 0L } catch (_: Exception) { 0L }
    }

    if (launchTime == 0L) {
        Text(windowStart, style = TelemetryMonoStyle, fontSize = 13.sp)
        return
    }

    val diff = launchTime - now
    if (diff <= 0) {
        Text(
            text = "MISSION IN FLIGHT / LAUNCHED",
            style = TelemetryMonoStyle,
            fontSize = 13.sp,
            color = SemanticSuccess
        )
    } else {
        val seconds = (diff / 1000) % 60
        val minutes = (diff / (1000 * 60)) % 60
        val hours = (diff / (1000 * 60 * 60)) % 24
        val days = diff / (1000 * 60 * 60 * 24)

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "T-",
                style = TelemetryMonoStyle,
                fontSize = 15.sp,
                color = AccentAmberDim
            )
            TelemetryUnit(value = days, label = "D")
            Text(":", style = TelemetryMonoStyle, fontSize = 15.sp, color = TextTertiary)
            TelemetryUnit(value = hours, label = "H")
            Text(":", style = TelemetryMonoStyle, fontSize = 15.sp, color = TextTertiary)
            TelemetryUnit(value = minutes, label = "M")
            Text(":", style = TelemetryMonoStyle, fontSize = 15.sp, color = TextTertiary)
            TelemetryUnit(value = seconds, label = "S")
        }
    }
}

@Composable
fun TelemetryUnit(value: Long, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = String.format("%02d", value),
            style = TelemetryMonoStyle,
            fontSize = 15.sp
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontSize = 9.sp
        )
    }
}

// --- GALLERY SCREEN ---
@Composable
fun GalleryScreen(homeViewModel: HomeViewModel, navController: NavController) {
    val galleryList by homeViewModel.galleryList
    val isLoading by homeViewModel.isLoading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase)
    ) {
        if (isLoading && galleryList.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = TextSecondary
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(span = { GridItemSpan(2) }) {
                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                        Text(
                            text = "DEEP-SKY ARCHIVE",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "NASA Astronomical Imagery & Astrophotography",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                itemsIndexed(
                    items = galleryList,
                    span = { index, _ ->
                        if (index % 5 == 0) GridItemSpan(2) else GridItemSpan(1)
                    }
                ) { index, apod ->
                    val isHero = (index % 5 == 0)
                    ObservatoryCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = if (isHero) 10.dp else 8.dp,
                        onClick = {
                            homeViewModel.selectApod(apod)
                            navController.navigate(Screen.ImagePreview.route)
                        }
                    ) {
                        AsyncImage(
                            model = apod.url,
                            contentDescription = apod.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isHero) 170.dp else 110.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = apod.title,
                            color = TextPrimary,
                            style = if (isHero) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = apod.date ?: "NASA Deep Space",
                            color = TextTertiary,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

// --- IMAGE PREVIEW SCREEN ---
@Composable
fun ImagePreviewScreen(
    homeViewModel: HomeViewModel,
    billingRepository: BillingRepository,
    navController: NavController,
    onOpenPaywall: () -> Unit
) {
    val apod = homeViewModel.selectedApod.value
    val isPremium by billingRepository.isPremiumUser.collectAsState()

    if (apod == null) {
        Box(Modifier.fillMaxSize().background(BgBase), contentAlignment = Alignment.Center) {
            Text("Observation not found", color = TextTertiary)
        }
        return
    }

    val activeImageUrl = if (isPremium && !apod.hdurl.isNullOrEmpty()) apod.hdurl else apod.url

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase)
    ) {
        AsyncImage(
            model = activeImageUrl,
            contentDescription = apod.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Top Back Button
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .background(BgElevated.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }

        // Bottom Details & HD Gating Banner
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(BgElevated.copy(alpha = 0.92f))
                .border(BorderStroke(1.dp, BorderHairline))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = apod.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                if (isPremium) {
                    Surface(
                        color = BgElevated2,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, SemanticSuccess)
                    ) {
                        Text(
                            text = "LOSSLESS HD",
                            style = MaterialTheme.typography.labelSmall,
                            color = SemanticSuccess,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        color = BgElevated2,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, AccentAmber),
                        modifier = Modifier.clickable { onOpenPaywall() }
                    ) {
                        Text(
                            text = "UNLOCK HD PRO",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            apod.explanation?.let { exp ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = exp,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// --- MORE / SETTINGS SCREEN ---
@Composable
fun MoreScreen(
    billingRepository: BillingRepository,
    consentManager: ConsentManager,
    onOpenPaywall: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val isPremium by billingRepository.isPremiumUser.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationScheduler.scheduleDailyApodReminder(context)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "OBSERVATORY CONFIGURATION",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        // --- SECTION 1: MEMBERSHIP ---
        item {
            Column {
                Text(
                    text = "MEMBERSHIP",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                ObservatoryCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = BgElevated,
                    borderColor = if (isPremium) SemanticSuccess else AccentAmber,
                    onClick = onOpenPaywall
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = if (isPremium) SemanticSuccess else AccentAmber,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPremium) "OBSERVATORY PRO ACTIVE" else "UPGRADE TO OBSERVATORY PRO",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (isPremium) "Ad-free exploration & uncompressed HD NASA imagery enabled." else "Unlock ad-free exploration and uncompressed NASA photography.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Hairline Divider
        item {
            HorizontalDivider(color = BorderHairline, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
        }

        // --- SECTION 2: PREFERENCES ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PREFERENCES",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // Daily Photo Reminders
                ObservatoryCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                NotificationScheduler.scheduleDailyApodReminder(context)
                            }
                        } else {
                            NotificationScheduler.scheduleDailyApodReminder(context)
                        }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                        Column {
                            Text("Daily Photo Reminders", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Receive morning transmissions for new space photos", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Privacy Options (UMP)
                if (consentManager.isPrivacyOptionsRequired && activity != null) {
                    ObservatoryCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { consentManager.showPrivacyOptionsForm(activity) {} }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(Icons.Outlined.PrivacyTip, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                            Column {
                                Text("Privacy Choices", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("Manage personalized advertising consent preferences", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // Hairline Divider
        item {
            HorizontalDivider(color = BorderHairline, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
        }

        // --- SECTION 3: SHOP ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SHOP",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // 1. Official Space Gear
                ObservatoryCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { uriHandler.openUri("https://amzn.to/4sDshwQ") }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(Icons.Outlined.ShoppingBag, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                        Column {
                            Text("Official Space Gear", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Browse authentic mission merchandise & apparel", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 2. Deep-Sky Projectors
                ObservatoryCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { uriHandler.openUri("https://oklumi.com/discount/DSN10?ref=vhjvgjai") }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                        Column {
                            Text("Deep-Sky Projectors", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Transform your room with HD cosmos projection", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Hairline Divider
        item {
            HorizontalDivider(color = BorderHairline, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
        }

        // --- SECTION 4: SUPPORT ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SUPPORT",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                ObservatoryCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:dailyspacenews@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Daily Space News Support")
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(Icons.Outlined.Email, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                        Column {
                            Text("Contact Mission Support", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Send inquiries, feedback, or telemetry bug reports", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

fun formatPublishedDate(publishedAt: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(publishedAt)
        if (date != null) {
            val diff = System.currentTimeMillis() - date.time
            val minutes = diff / (60 * 1000)
            val hours = minutes / 60
            val days = hours / 24
            when {
                diff < 0 -> "Just now"
                minutes < 1 -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                else -> "${days}d ago"
            }
        } else {
            ""
        }
    } catch (_: Exception) {
        ""
    }
}
