package com.nammavastra.app

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppMainView()
        }
    }
}

private enum class Screen { Splash, Auth, Home, Failure }
private enum class UserRole(val label: String) { Weaver("Weaver"), Buyer("Buyer") }
private enum class HomeTab(val title: String) { Dashboard("Home"), Gallery("Gallery"), Upload("Upload"), Calculator("Costing"), Settings("Account") }

private val Gold = Color(0xFFD4A574)
private val Burgundy = Color(0xFF6B2C3E)
private val Ink = Color(0xFF2B2523)
private val Paper = Color(0xFFFFF8EF)
private val Teal = Color(0xFF2F6F6D)

private fun safeParseColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return Color.Gray
    val colorString = if (hex.startsWith("#")) hex.substring(1) else hex
    return try {
        when (colorString.length) {
            6 -> Color(colorString.toLong(16) or 0xFF000000L)
            8 -> Color(colorString.toLong(16))
            else -> Color.Gray
        }
    } catch (e: Exception) {
        Color.Gray
    }
}

@Composable
private fun AppMainView() {
    var screen by remember { mutableStateOf(Screen.Splash) }
    var profile by remember { mutableStateOf(WeaverProfile()) }
    val supabase = NammaVastraApp.supabaseClient
    val repository = remember(supabase) { supabase?.let { SupabaseRepository(it) } }
    val scope = rememberCoroutineScope()

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Burgundy, onPrimary = Color.White, secondary = Teal, background = Paper, surface = Color.White, onSurface = Ink
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Paper) {
            if (repository == null && screen != Screen.Splash) {
                ErrorScreen("App configuration failed. Please check your internet or contact support.")
            } else {
                when (screen) {
                    Screen.Splash -> SplashScreen(onFinished = { 
                        if (repository == null) {
                            screen = Screen.Failure
                        } else {
                            val userId = repository.getCurrentUserId()
                            if (userId != null) {
                                scope.launch {
                                    try {
                                        val p = repository.getProfile(userId)
                                        if (p != null) {
                                            profile = p
                                            screen = Screen.Home
                                        } else screen = Screen.Auth
                                    } catch (e: Exception) { screen = Screen.Auth }
                                }
                            } else screen = Screen.Auth 
                        }
                    })
                    Screen.Auth -> repository?.let { AuthScreen(it, onAuthenticated = { p ->
                        profile = p
                        screen = Screen.Home 
                    }) }
                    Screen.Home -> repository?.let { HomeScreen(profile, it, onSignOut = {
                        scope.launch {
                            repository.signOut()
                            profile = WeaverProfile()
                            screen = Screen.Auth
                        }
                    }) }
                    Screen.Failure -> ErrorScreen("Failed to initialize connection to server.")
                }
            }
        }
    }
}

@Composable
private fun ErrorScreen(message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Oops!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Burgundy)
            Spacer(Modifier.height(16.dp))
            Text(message, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        delay(1200)
        onFinished()
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Gold, Burgundy))), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = visible) {
            Text("Namma Vastra", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AuthScreen(repository: SupabaseRepository, onAuthenticated: (WeaverProfile) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.Buyer) }
    var isLoading by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Gold, Burgundy)))) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Welcome", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Text("Enter your details to join", color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(40.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Your Name", color = Color.White) },
                modifier = Modifier.fillMaxWidth(), enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, 
                    unfocusedTextColor = Color.White, 
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    focusedLabelColor = Gold,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                )
            )
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoleButton("Buyer", selectedRole == UserRole.Buyer, Modifier.weight(1f)) { selectedRole = UserRole.Buyer }
                RoleButton("Weaver", selectedRole == UserRole.Weaver, Modifier.weight(1f)) { selectedRole = UserRole.Weaver }
            }
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = { 
                    if (name.isBlank()) {
                        Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        try {
                            repository.signInAnonymously()
                            val uid = repository.getCurrentUserId() ?: ""
                            if (uid.isNotEmpty()) {
                                val p = WeaverProfile(id = uid, name = name, role = selectedRole.label)
                                try {
                                    repository.saveProfile(p)
                                    onAuthenticated(p)
                                } catch (e: Exception) {
                                    Log.e("Auth", "Profile save failed", e)
                                    Toast.makeText(context, "Auth succeeded, but profile creation failed.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Error: User ID is empty", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("Auth", "Sign in failed", e)
                            val msg = e.message ?: "Unknown Error"
                            Toast.makeText(context, "Login Error: $msg", Toast.LENGTH_LONG).show()
                        } finally { isLoading = false }
                    }
                },
                enabled = !isLoading, modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = Ink)
                else Text("Start Exploring", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RoleButton(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick, 
        modifier = modifier, 
        colors = ButtonDefaults.buttonColors(containerColor = if(isSelected) Gold else Color.White.copy(alpha = 0.1f))
    ) {
        Text(text, color = if(isSelected) Ink else Color.White)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(profile: WeaverProfile, repository: SupabaseRepository, onSignOut: () -> Unit) {
    var tab by remember { mutableStateOf(HomeTab.Dashboard) }
    val tabs = HomeTab.entries.filter { if (profile.role != "Weaver") it != HomeTab.Upload else true }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Namma Vastra", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Burgundy, titleContentColor = Color.White)) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        label = { Text(t.title, fontSize = 10.sp) },
                        icon = { Box(Modifier.size(8.dp).background(if(tab==t) Burgundy else Color.LightGray, CircleShape)) }
                    )
                }
            }
        }
    ) { p ->
        Box(Modifier.padding(p)) {
            when (tab) {
                HomeTab.Dashboard -> DashboardScreen(repository)
                HomeTab.Gallery -> GalleryScreen(repository)
                HomeTab.Upload -> UploadSareeScreen(repository) { tab = HomeTab.Gallery }
                HomeTab.Calculator -> CalculatorScreen()
                HomeTab.Settings -> SettingsScreen(profile, onSignOut)
            }
        }
    }
}

@Composable
private fun DashboardScreen(repo: SupabaseRepository) {
    var trends by remember { mutableStateOf(emptyList<Trend>()) }
    LaunchedEffect(Unit) { 
        trends = repo.getTrends().ifEmpty { 
            listOf(Trend("1", "Heritage Red", "Ilkal", "", listOf("#6B2C3E", "#D4A574"))) 
        } 
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { 
            Box(Modifier.fillMaxWidth().height(140.dp).background(Burgundy, RoundedCornerShape(12.dp)), Alignment.Center) { 
                Text("Authentic Karnataka Handlooms", color = Color.White, fontWeight = FontWeight.Bold) 
            } 
        }
        item { Text("Trending Palettes", fontWeight = FontWeight.Bold) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(trends) { t ->
                    Card(Modifier.width(140.dp), shape = RoundedCornerShape(12.dp)) {
                        Column {
                            Row(Modifier.height(70.dp).fillMaxWidth()) { 
                                t.colors?.forEach { c -> Box(Modifier.weight(1f).fillMaxHeight().background(safeParseColor(c))) } 
                            }
                            Column(Modifier.padding(12.dp)) {
                                Text(t.title ?: "", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(t.city ?: "", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryScreen(repo: SupabaseRepository) {
    var items by remember { mutableStateOf(emptyList<Saree>()) }
    var isLoading by remember { mutableStateOf(true) }
    var debugInfo by remember { mutableStateOf("Initializing...") }
    var rawJson by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) { 
        isLoading = true
        val result = repo.getSareesResult()
        items = result.items
        rawJson = result.rawJson
        debugInfo = result.error ?: "Successfully loaded ${items.size} items."
        isLoading = false
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Burgundy)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // DEBUG OVERLAY
            item {
                Surface(
                    color = Color.Black.copy(alpha = 0.05f), 
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text("DEBUG STATUS: $debugInfo", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("RAW DATA: $rawJson", fontSize = 9.sp, color = Color.Gray, maxLines = 3)
                    }
                }
            }
            
            if (items.isEmpty()) item { 
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No sarees found.", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("Check the Debug Status above to see why.", fontSize = 12.sp, color = Color.Gray)
                }
            }
            
            items(items) { s ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column {
                        AsyncImage(
                            model = s.imageUrl, 
                            contentDescription = null, 
                            modifier = Modifier.fillMaxWidth().height(240.dp), 
                            contentScale = ContentScale.Crop
                        )
                        Column(Modifier.padding(16.dp)) {
                            Text(s.title ?: "No Title", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Rs. ${s.price}", color = Burgundy, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Spacer(Modifier.weight(1f))
                                Text(s.origin ?: "Karnataka", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadSareeScreen(repo: SupabaseRepository, onUploaded: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var uri by remember { mutableStateOf<Uri?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri = it }
    
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Upload New Saree", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray.copy(alpha = 0.4f))
                .clickable { launcher.launch("image/*") }, 
            Alignment.Center
        ) {
            if (uri != null) {
                AsyncImage(uri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text("Select Saree Photo", color = Burgundy, fontWeight = FontWeight.Bold)
            }
        }
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = price, 
            onValueChange = { price = it }, 
            label = { Text("Price (Rs.)") }, 
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Button(
            onClick = { 
                if(uri == null || title.isBlank() || price.isBlank()) {
                    Toast.makeText(context, "Please fill all details", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                loading = true
                scope.launch { 
                    try {
                        repo.uploadSaree(context, title, price, "Karnataka", uri!!, listOf("#6B2C3E")) 
                        Toast.makeText(context, "Uploaded Successfully!", Toast.LENGTH_SHORT).show()
                        onUploaded()
                    } catch(e:Exception) { 
                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show() 
                    } finally { loading = false }
                }
            }, 
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) { 
            if(loading) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White) 
            else Text("Upload Saree") 
        }
    }
}

@Composable
private fun CalculatorScreen() {
    var yarn by remember { mutableStateOf("1000") }
    val total = (yarn.toIntOrNull() ?: 0) * 2.7
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Fair Price Calculator", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        OutlinedTextField(
            value = yarn, 
            onValueChange = { yarn = it }, 
            label = { Text("Yarn/Silk Cost") }, 
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Card(
            Modifier.fillMaxWidth(), 
            colors = CardDefaults.cardColors(containerColor = Burgundy),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Suggested Retail Price", color = Color.White.copy(alpha = 0.7f))
                Text("Rs. ${total.toInt()}", fontSize = 36.sp, color = Gold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SettingsScreen(profile: WeaverProfile, onSignOut: () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text("Account", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Name", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(profile.name ?: "Unknown", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Text("Role", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(profile.role ?: "Buyer", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onSignOut, 
            modifier = Modifier.fillMaxWidth().height(56.dp), 
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            shape = RoundedCornerShape(12.dp)
        ) { 
            Text("Logout") 
        }
    }
}

@Composable
private fun LoomMark(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val stroke = Stroke(width = size.width * 0.08f)
        drawRect(color, style = stroke)
        drawLine(color, Offset(size.width * 0.33f, 0f), Offset(size.width * 0.33f, size.height), stroke.width)
        drawLine(color, Offset(size.width * 0.66f, 0f), Offset(size.width * 0.66f, size.height), stroke.width)
    }
}
