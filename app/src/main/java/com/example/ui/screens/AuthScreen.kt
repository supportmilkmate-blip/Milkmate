package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.net.Uri
import com.example.util.PhoneAuthHelper
import com.google.firebase.auth.PhoneAuthProvider
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BiometricAuthHelper
import com.example.ui.util.BusinessModeFeatures
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Redesigned, User-Friendly Authentication & Signup Onboarding Screen
 * 
 * Features:
 * 1. Top Mode Switcher: [✨ Create New Dairy (Sign Up)] vs [🔑 Quick Log In]
 * 2. Activity-Based Customization during Signup:
 *    - Manage Herd (Yes/No -> Cow, Buffalo, or Both; Feed & Consumables Compulsory)
 *    - Source Procurement (Village Farmers, Bulk Inward)
 *    - Selling Channels (Individual Households, Restaurants/B2B, Traders, BMC Collection Centre, Retail Counter)
 *    - Own Processing Plant (Paneer, Curd, Ghee, Khoya)
 *    - Dynamic AI / Smart Model Recommendation & Live Capabilities Preview
 * 3. Comprehensive Login Options:
 *    - Mobile OTP Login
 *    - 4-Digit Fast PIN & Biometric Fingerprint Unlock
 *    - Staff & Delivery Boy Login
 *    - Multi-Dairy Local Account Switcher
 *    - Instant Sample Demo Mode
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: MilkMateViewModel) {
    val session by viewModel.sessionState.collectAsStateWithLifecycle()

    // Top Auth Mode: "SIGNUP" (New Dairy Onboarding) or "LOGIN" (Returning Owner / Staff)
    var topAuthMode by remember { mutableStateOf("SIGNUP") }

    // Login Role: "OWNER" or "STAFF"
    var loginRole by remember { mutableStateOf("OWNER") }

    // Owner Login Mode: "OTP" (Phone + OTP + Google) or "PIN" (4-Digit PIN & Biometrics)
    var ownerLoginMode by remember { mutableStateOf("OTP") }

    // Guided Signup Flow Step: 1 = Verification & Demo, 2 = Model & Functions, 3 = Dairy Name & PIN
    var signupFlowStep by remember { mutableIntStateOf(1) }
    var hasCattleFeed by remember { mutableStateOf(true) }

    // 1-Screen Quick Signup Collapsible Advanced Settings
    var showAdvancedSignup by remember { mutableStateOf(false) }

    // Legacy wizard step and sub-tab compatibility
    var signupStep by remember { mutableIntStateOf(1) }
    var loginSubTab by remember { mutableStateOf("OTP") }

    // Identity Inputs
    var phoneInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf<String?>(null) }
    var firebaseVerificationId by remember { mutableStateOf<String?>(null) }
    var firebaseResendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var isOtpVerified by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(60) }
    var isVerifying by remember { mutableStateOf(false) }
    var isSmsSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var dairyName by remember { mutableStateOf("Gokul Milk Dairy & Farm") }
    var ownerName by remember { mutableStateOf("") }
    var securityPin by remember { mutableStateOf("") }
    var enableBiometric by remember { mutableStateOf(true) }

    // ================= ACTIVITY-BASED SELECTIONS =================
    // 1. Source Activities
    var actManageHerd by remember { mutableStateOf(true) }
    var actHerdSpecies by remember { mutableStateOf("BOTH") } // COW, BUFFALO, BOTH
    var actProcureFromFarmers by remember { mutableStateOf(false) }
    var actBulkInward by remember { mutableStateOf(false) }

    // 2. Sales & Distribution Channels (Multi-Select)
    var actSellHouseholds by remember { mutableStateOf(true) }
    var actSellRestaurants by remember { mutableStateOf(false) }
    var actSellTraders by remember { mutableStateOf(false) }
    var actSellCollectionCentre by remember { mutableStateOf(false) }
    var actSellRetailCounter by remember { mutableStateOf(false) }

    // Customisation Options during Signup
    var customMilkType by remember { mutableStateOf("BOTH") } // COW_ONLY, BUFFALO_ONLY, BOTH, MIXED
    var customPricingMode by remember { mutableStateOf("DIRECT") } // FAT_SNF, DIRECT, FAT_ONLY, PANEER_YIELD
    var customCowRate by remember { mutableStateOf("50.0") }
    var customBuffaloRate by remember { mutableStateOf("65.0") }
    var hasPaneer by remember { mutableStateOf(true) }
    var hasGheeButter by remember { mutableStateOf(true) }
    var hasCurdChaas by remember { mutableStateOf(true) }
    var hasKhoyaSweets by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf(session.languageCode) }

    // Google Sign-In Dialog & Credential Manager
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showGoogleAuthDialog by remember { mutableStateOf(false) }
    var googleEmailInput by remember { mutableStateOf("dairy.owner@gmail.com") }
    var googleNameInput by remember { mutableStateOf("") }

    val performGoogleSignIn: () -> Unit = {
        coroutineScope.launch {
            try {
                isVerifying = true
                errorMessage = null
                val activity = context as? Activity ?: (context as? FragmentActivity)
                if (activity != null) {
                    val user = com.example.util.GoogleSignInHelper.signInWithGoogle(activity)
                    if (user != null) {
                        val email = user.email ?: "owner@milkmate.com"
                        val name = user.displayName ?: ownerName.ifBlank { dairyName.ifBlank { "Dairy Owner" } }
                        val uid = user.uid
                        if (topAuthMode == "SIGNUP") {
                            ownerName = name
                            googleEmailInput = email
                            isOtpVerified = true
                            signupFlowStep = 2
                            viewModel.showMessage("Google Account verified: $email ✓")
                        } else {
                            viewModel.loginWithGoogleAccount(email, name, uid)
                        }
                    }
                } else {
                    showGoogleAuthDialog = true
                }
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                errorMessage = "Google Sign-In was cancelled."
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Google Sign-In failed"
                showGoogleAuthDialog = true
            } finally {
                isVerifying = false
            }
        }
    }

    val performSendRealOtp: (String) -> Unit = { targetPhone ->
        val clean = targetPhone.trim()
        if (clean.length < 10) {
            errorMessage = "Please enter a valid 10-digit mobile number."
        } else {
            errorMessage = null
            isSmsSending = true
            val activity = context as? Activity ?: (context as? FragmentActivity)
            if (activity != null) {
                PhoneAuthHelper.sendSmsOtp(
                    activity = activity,
                    fullPhoneNumber = clean,
                    resendToken = firebaseResendToken
                ) { result ->
                    isSmsSending = false
                    when (result) {
                        is PhoneAuthHelper.PhoneAuthResult.CodeSent -> {
                            firebaseVerificationId = result.verificationId
                            firebaseResendToken = result.token
                            viewModel.setFirebaseVerificationId(result.verificationId)
                            generatedOtp = "SMS-SENT"
                            timerSeconds = 60
                            viewModel.showMessage("Real Google SMS OTP sent to +91 $clean. Please enter the 6-digit code received.")
                        }
                        is PhoneAuthHelper.PhoneAuthResult.AutoVerified -> {
                            isOtpVerified = true
                            isVerifying = false
                            val uid = result.user.uid
                            viewModel.verifyOtpAndLogin(clean, "AUTO", uid) { isNewUser, _ ->
                                if (isNewUser) {
                                    topAuthMode = "SIGNUP"
                                    signupFlowStep = 2
                                }
                            }
                            viewModel.showMessage("Mobile number auto-verified by Android! ✓")
                        }
                        is PhoneAuthHelper.PhoneAuthResult.Error -> {
                            errorMessage = result.message
                            // Also allow WhatsApp OTP fallback
                            viewModel.sendOtp(clean) { simCode ->
                                generatedOtp = simCode
                                timerSeconds = 60
                            }
                            viewModel.showMessage("${result.message} — You can also verify via WhatsApp OTP below.")
                        }
                    }
                }
            } else {
                isSmsSending = false
                viewModel.sendOtp(clean) { simCode ->
                    generatedOtp = simCode
                    timerSeconds = 60
                }
            }
        }
    }

    val performWhatsAppOtp: (String) -> Unit = { targetPhone ->
        val clean = targetPhone.trim()
        if (clean.length < 10) {
            errorMessage = "Please enter a valid 10-digit mobile number."
        } else {
            errorMessage = null
            viewModel.sendOtp(clean) { code ->
                generatedOtp = code
                timerSeconds = 60
                val formattedPhone = if (clean.startsWith("+")) clean else "91$clean"
                val message = "Your MilkMate Dairy OS verification code is: $code. Valid for 10 minutes. Do not share this OTP with anyone."
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                try {
                    context.startActivity(intent)
                    viewModel.showMessage("Opening WhatsApp to send OTP verification code to $clean ✓")
                } catch (_: Exception) {
                    viewModel.showMessage("WhatsApp not installed. Verification code: $code")
                }
            }
        }
    }

    val performVerifyRealOtp: () -> Unit = {
        if (otpInput.length < 6) {
            errorMessage = "Please enter 6-digit verification code."
        } else {
            isVerifying = true
            errorMessage = null
            val verId = firebaseVerificationId
            if (!verId.isNullOrBlank()) {
                coroutineScope.launch {
                    try {
                        val user = PhoneAuthHelper.verifyOtpCode(verId, otpInput.trim())
                        isVerifying = false
                        if (user != null) {
                            isOtpVerified = true
                            viewModel.verifyOtpAndLogin(phoneInput, otpInput, user.uid) { isNewUser, _ ->
                                if (isNewUser) {
                                    topAuthMode = "SIGNUP"
                                    signupFlowStep = 2
                                    viewModel.showMessage("Mobile number verified successfully! ✓")
                                }
                            }
                        } else {
                            errorMessage = "Invalid verification code. Please try again."
                        }
                    } catch (e: Exception) {
                        isVerifying = false
                        // Check if fallback to direct ViewModel verification works (e.g., WhatsApp OTP code)
                        viewModel.verifyOtpAndLogin(phoneInput, otpInput) { isNewUser, _ ->
                            if (isNewUser) {
                                topAuthMode = "SIGNUP"
                                signupFlowStep = 2
                                viewModel.showMessage("Code verified successfully! ✓")
                            }
                        }
                    }
                }
            } else {
                viewModel.verifyOtpAndLogin(phoneInput, otpInput) { isNewUser, _ ->
                    isVerifying = false
                    isOtpVerified = true
                    if (isNewUser) {
                        topAuthMode = "SIGNUP"
                        signupFlowStep = 2
                    }
                }
            }
        }
    }

    // 1-Screen Quick Signup Launch Handler
    val handleQuickLaunch: () -> Unit = {
        if (phoneInput.length < 10) {
            errorMessage = "Please enter a valid 10-digit mobile number."
        } else if (dairyName.isBlank()) {
            errorMessage = "Please enter your Dairy / Business Name."
        } else {
            errorMessage = null
            val cRate = customCowRate.toDoubleOrNull() ?: 50.0
            val bRate = customBuffaloRate.toDoubleOrNull() ?: 65.0

            viewModel.repository.sessionManager.setLanguage(selectedLanguage)
            viewModel.completeFirstLaunchWithCapabilities(
                businessName = dairyName.trim(),
                ownerName = ownerName.trim().ifBlank { "Dairy Owner" },
                phone = phoneInput.trim(),
                sourceOwnCattle = actManageHerd,
                sourceVillageFarmers = actProcureFromFarmers || actBulkInward,
                destHouseholds = actSellHouseholds || actSellRetailCounter,
                destCollectionCenter = actSellCollectionCentre,
                destBulkCommercial = actSellRestaurants || actSellTraders,
                destFactoryTankers = actSellCollectionCentre,
                hasPaneer = hasPaneer,
                hasCurdChaas = hasCurdChaas,
                hasGheeButter = hasGheeButter,
                hasKhoyaSweets = hasKhoyaSweets,
                hasCattleFeed = actManageHerd,
                supportedMilkTypes = customMilkType,
                cowRate = cRate,
                buffaloRate = bRate,
                pricingMode = customPricingMode
            )

            if (securityPin.length == 4 || enableBiometric) {
                viewModel.setSecuritySettings(
                    appLockEnabled = securityPin.length == 4,
                    pin = if (securityPin.length == 4) securityPin else null,
                    biometricEnabled = enableBiometric
                )
            }
        }
    }

    // Language selection dialog
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Timer countdown for OTP resend
    LaunchedEffect(loginRole, ownerLoginMode, timerSeconds) {
        if (timerSeconds > 0 && generatedOtp != null) {
            delay(1000)
            timerSeconds -= 1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = "MilkMate Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MilkMate Dairy OS",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Smart Milk Delivery, Collection & Farm Accounting",
                                fontSize = 10.5.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                actions = {
                    // Quick Language Switcher Action
                    IconButton(onClick = { showLanguageDialog = true }) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Language, contentDescription = "Language", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedLanguage.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RoyalBluePrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ================= TOP MAIN MODE SELECTOR (SIGN UP vs LOG IN) =================
            Surface(
                color = RoyalBluePrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sign Up Tab
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (topAuthMode == "SIGNUP") Color.White else Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                topAuthMode = "SIGNUP"
                                errorMessage = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddBusiness,
                                contentDescription = null,
                                tint = if (topAuthMode == "SIGNUP") RoyalBluePrimary else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Create Dairy (Sign Up)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (topAuthMode == "SIGNUP") RoyalBluePrimary else Color.White
                            )
                        }
                    }

                    // Log In Tab
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (topAuthMode == "LOGIN") Color.White else Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                topAuthMode = "LOGIN"
                                errorMessage = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                tint = if (topAuthMode == "LOGIN") RoyalBluePrimary else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Quick Log In",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (topAuthMode == "LOGIN") RoyalBluePrimary else Color.White
                            )
                        }
                    }
                }
            }

            // Error Banner if present
            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { errorMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // ================= BODY CONTENT =================
            if (topAuthMode == "SIGNUP") {
                // ================= GUIDED 3-STEP SIGN UP =================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Step Progress Header
                    SignupProgressHeader(
                        currentStep = signupFlowStep,
                        onStepClick = { step ->
                            if (step < signupFlowStep) {
                                signupFlowStep = step
                                errorMessage = null
                            }
                        }
                    )

                    when (signupFlowStep) {
                        // STEP 1: Phone Verification / Google Sign Up + Instant Demo
                        1 -> {
                            SignupStep1VerificationCard(
                                phoneInput = phoneInput,
                                onPhoneChange = { phoneInput = it; errorMessage = null },
                                otpInput = otpInput,
                                onOtpChange = { otpInput = it; errorMessage = null },
                                generatedOtp = generatedOtp,
                                isVerifying = isVerifying,
                                isSmsSending = isSmsSending,
                                timerSeconds = timerSeconds,
                                onSendRealSmsOtp = { performSendRealOtp(phoneInput) },
                                onSendWhatsAppOtp = { performWhatsAppOtp(phoneInput) },
                                onVerifyOtp = performVerifyRealOtp,
                                onGoogleSignUp = performGoogleSignIn,
                                onSwitchToLogin = {
                                    topAuthMode = "LOGIN"
                                    errorMessage = null
                                }
                            )

                            // Prominent Demo Dairy Card on Signup Page
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, FreshGold.copy(alpha = 0.5f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.quickDemoLogin() }
                                    .testTag("signup_demo_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = FreshGoldLight,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color(0xFF7A5800),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "⚡ Try Instant Demo Dairy",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF7A5800)
                                        )
                                        Text(
                                            text = "Explore complete features with sample data — no login or phone needed",
                                            fontSize = 11.5.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFF7A5800),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // STEP 2: Business Models & Functions (Settings Style)
                        2 -> {
                            SignupStep2FunctionsCard(
                                manageHerd = actManageHerd,
                                onManageHerdChange = { actManageHerd = it },
                                procureFromFarmers = actProcureFromFarmers,
                                onProcureFarmersChange = { actProcureFromFarmers = it },
                                sellHouseholds = actSellHouseholds,
                                onSellHouseholdsChange = { actSellHouseholds = it },
                                sellCommercial = actSellRestaurants,
                                onSellCommercialChange = { actSellRestaurants = it },
                                hasByproducts = hasPaneer,
                                onByproductsChange = {
                                    hasPaneer = it
                                    hasGheeButter = it
                                    hasCurdChaas = it
                                    hasKhoyaSweets = it
                                },
                                hasCattleFeed = hasCattleFeed,
                                onCattleFeedChange = { hasCattleFeed = it },
                                onBack = { signupFlowStep = 1 },
                                onNext = { signupFlowStep = 3 }
                            )
                        }

                        // STEP 3: Dairy Name, Owner Name & Security PIN
                        3 -> {
                            SignupStep3DetailsCard(
                                phone = phoneInput,
                                dairyName = dairyName,
                                onDairyNameChange = { dairyName = it; errorMessage = null },
                                ownerName = ownerName,
                                onOwnerNameChange = { ownerName = it },
                                securityPin = securityPin,
                                onPinChange = { securityPin = it },
                                cowRate = customCowRate,
                                onCowRateChange = { customCowRate = it },
                                buffaloRate = customBuffaloRate,
                                onBuffaloRateChange = { customBuffaloRate = it },
                                selectedLanguage = selectedLanguage,
                                onLanguageClick = { showLanguageDialog = true },
                                onBack = { signupFlowStep = 2 },
                                onLaunch = handleQuickLaunch
                            )
                        }
                    }
                }
            } else {
                // ================= LOGIN WORKFLOW =================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Dedicated 2-Tab Switcher: Owner vs Staff
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (loginRole == "OWNER") RoyalBluePrimary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        loginRole = "OWNER"
                                        errorMessage = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (loginRole == "OWNER") Color.White else TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Owner Login",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (loginRole == "OWNER") Color.White else TextPrimary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (loginRole == "STAFF") RoyalBluePrimary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        loginRole = "STAFF"
                                        viewModel.loadExistingBusinesses()
                                        errorMessage = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.TwoWheeler,
                                        contentDescription = null,
                                        tint = if (loginRole == "STAFF") Color.White else TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Staff Login",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (loginRole == "STAFF") Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (loginRole == "OWNER") {
                        if (ownerLoginMode == "OTP") {
                            Step1PhoneInput(
                                phoneInput = phoneInput,
                                onPhoneChange = { phoneInput = it; errorMessage = null },
                                onSendOtp = { performSendRealOtp(phoneInput) },
                                onSendWhatsAppOtp = { performWhatsAppOtp(phoneInput) },
                                onSwitchToStaff = { loginRole = "STAFF" },
                                onSwitchToPin = { ownerLoginMode = "PIN" },
                                onSwitchToAccounts = {
                                    viewModel.loadExistingBusinesses()
                                },
                                onQuickDemo = { viewModel.quickDemoLogin() },
                                onGoogleSignIn = performGoogleSignIn
                            )

                            if (generatedOtp != null) {
                                Step2OtpVerification(
                                    phone = phoneInput,
                                    otpInput = otpInput,
                                    generatedOtp = generatedOtp,
                                    timerSeconds = timerSeconds,
                                    isVerifying = isVerifying,
                                    onOtpChange = { otpInput = it; errorMessage = null },
                                    onEditPhone = { generatedOtp = null; otpInput = "" },
                                    onResendOtp = { performSendRealOtp(phoneInput) },
                                    onVerifyOtp = performVerifyRealOtp
                                )
                            }
                        } else {
                            PinBiometricLoginCard(
                                viewModel = viewModel,
                                onSwitchToOtp = { ownerLoginMode = "OTP" },
                                onSwitchToRegister = {
                                    topAuthMode = "SIGNUP"
                                }
                            )
                        }
                    } else {
                        // Dedicated Staff Login Card
                        StaffAuthCard(viewModel = viewModel)
                    }

                    // Single, Clean Demo Dairy Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, FreshGold.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.quickDemoLogin() }
                            .testTag("single_demo_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = FreshGoldLight,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF7A5800),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "⚡ Try Instant Demo Dairy",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF7A5800)
                                )
                                Text(
                                    text = "Explore complete features with sample data — no login needed",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF7A5800),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }


    // ================= GOOGLE SIGN IN DIALOG =================
    if (showGoogleAuthDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleAuthDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G ", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)
                    Text("Sign In with Google", fontWeight = FontWeight.Bold, fontSize = 16.5.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Connect your Google account for secure cloud sync, automatic backup, and passwordless login.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = googleEmailInput,
                        onValueChange = { googleEmailInput = it },
                        label = { Text("Google Account Email") },
                        placeholder = { Text("e.g. yourname@gmail.com") },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = RoyalBluePrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = googleNameInput,
                        onValueChange = { googleNameInput = it },
                        label = { Text("Dairy Owner Name") },
                        placeholder = { Text("e.g. Ramesh Patel") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showGoogleAuthDialog = false
                                performGoogleSignIn()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("One-Tap Fast Sign In", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                                Text("Continue as ${googleEmailInput.ifBlank { "Google User" }}", fontSize = 10.5.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = googleEmailInput.ifBlank { "owner.dairy@gmail.com" }
                        val name = googleNameInput.ifBlank { dairyName.ifBlank { "Dairy Owner" } }
                        val uid = "GOOGLE_" + email.replace("@", "_").replace(".", "_")
                        viewModel.loginWithGoogleAccount(email, name, uid)
                        showGoogleAuthDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
                ) {
                    Text("Continue with Google", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleAuthDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ================= LANGUAGE SELECTION MODAL =================
    if (showLanguageDialog) {
        SignupLanguageModal(
            currentLang = selectedLanguage,
            onSelectLang = { langCode ->
                selectedLanguage = langCode
                viewModel.repository.sessionManager.setLanguage(langCode)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

/**
 * Guided Signup Progress Stepper Header
 */
@Composable
fun SignupProgressHeader(currentStep: Int, onStepClick: (Int) -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("signup_stepper_header")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SignupStepItem(step = 1, label = "1. Verify", active = currentStep == 1, completed = currentStep > 1, onClick = { onStepClick(1) })
            HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 6.dp), color = if (currentStep > 1) DairyGreen else MaterialTheme.colorScheme.outlineVariant)
            SignupStepItem(step = 2, label = "2. Functions", active = currentStep == 2, completed = currentStep > 2, onClick = { onStepClick(2) })
            HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 6.dp), color = if (currentStep > 2) DairyGreen else MaterialTheme.colorScheme.outlineVariant)
            SignupStepItem(step = 3, label = "3. Details & PIN", active = currentStep == 3, completed = false, onClick = { onStepClick(3) })
        }
    }
}

@Composable
fun SignupStepItem(step: Int, label: String, active: Boolean, completed: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(enabled = completed, onClick = onClick)
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                completed -> DairyGreen
                active -> RoyalBluePrimary
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (completed) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                } else {
                    Text(
                        text = "$step",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = if (active) RoyalBluePrimary else TextPrimary
        )
    }
}

/**
 * SIGNUP STEP 1: Verification Card (Phone OTP & Google 1-Tap)
 */
@Composable
fun SignupStep1VerificationCard(
    phoneInput: String,
    onPhoneChange: (String) -> Unit,
    otpInput: String,
    onOtpChange: (String) -> Unit,
    generatedOtp: String?,
    isVerifying: Boolean,
    isSmsSending: Boolean,
    timerSeconds: Int,
    onSendRealSmsOtp: () -> Unit,
    onSendWhatsAppOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    onGoogleSignUp: () -> Unit,
    onSwitchToLogin: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("signup_step1_card")
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = RoyalBlueLight,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Sign Up & Register Dairy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Step 1 of 3: Verify real mobile number or Google account",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Quick Google Sign Up
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = RoyalBlueLight.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onGoogleSignUp)
                    .testTag("signup_google_btn")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("G", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sign Up with Google Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = RoyalBluePrimary
                        )
                        Text(
                            text = "Instant verification with automatic cloud sync",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = RoyalBluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = " OR REAL MOBILE VERIFICATION ",
                    fontSize = 10.5.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            // Phone Field
            OutlinedTextField(
                value = phoneInput,
                onValueChange = {
                    if (it.length <= 10) onPhoneChange(it.filter { c -> c.isDigit() })
                },
                label = { Text("10-Digit Mobile Number *") },
                placeholder = { Text("e.g. 9876543210") },
                leadingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                    ) {
                        Text("+91", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RoyalBluePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("signup_phone_input")
            )

            // 2 Verification Buttons: SMS OTP (Primary) & WhatsApp OTP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSendRealSmsOtp,
                    enabled = phoneInput.length == 10 && !isSmsSending,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("signup_send_sms_otp_btn")
                ) {
                    if (isSmsSending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SMS OTP", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onSendWhatsAppOtp,
                    enabled = phoneInput.length == 10,
                    border = BorderStroke(1.dp, DairyGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("signup_send_whatsapp_otp_btn")
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp OTP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                }
            }

            // OTP Section if dispatched
            if (generatedOtp != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DairyGreenLight,
                    border = BorderStroke(1.dp, DairyGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Real OTP Sent to +91 $phoneInput", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DairyGreen)
                            if (timerSeconds > 0) {
                                Text("${timerSeconds}s", fontSize = 11.sp, color = TextSecondary)
                            } else {
                                TextButton(onClick = onSendRealSmsOtp, contentPadding = PaddingValues(0.dp)) {
                                    Text("Resend", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                                }
                            }
                        }
                        Text(
                            text = "Please check your incoming SMS messages or WhatsApp chat for the 6-digit verification code.",
                            fontSize = 10.5.sp,
                            color = TextSecondary
                        )
                    }
                }

                OutlinedTextField(
                    value = otpInput,
                    onValueChange = {
                        if (it.length <= 6) onOtpChange(it.filter { c -> c.isDigit() })
                    },
                    label = { Text("Enter 6-Digit OTP Code *") },
                    placeholder = { Text("123456") },
                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = RoyalBluePrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("signup_otp_input")
                )

                Button(
                    onClick = onVerifyOtp,
                    enabled = otpInput.length == 6 && !isVerifying,
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("signup_verify_otp_btn")
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify & Continue", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Footer Switch to Login
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already registered? ", fontSize = 13.sp, color = TextSecondary)
                Text(
                    text = "Log In Here",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBluePrimary,
                    modifier = Modifier.clickable(onClick = onSwitchToLogin)
                )
            }
        }
    }
}

/**
 * SIGNUP STEP 2: Business Models & Functions (Settings Style)
 */
@Composable
fun SignupStep2FunctionsCard(
    manageHerd: Boolean,
    onManageHerdChange: (Boolean) -> Unit,
    procureFromFarmers: Boolean,
    onProcureFarmersChange: (Boolean) -> Unit,
    sellHouseholds: Boolean,
    onSellHouseholdsChange: (Boolean) -> Unit,
    sellCommercial: Boolean,
    onSellCommercialChange: (Boolean) -> Unit,
    hasByproducts: Boolean,
    onByproductsChange: (Boolean) -> Unit,
    hasCattleFeed: Boolean,
    onCattleFeedChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("signup_step2_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = RoyalBlueLight,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Dairy Model & Functions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Step 2 of 3: Turn on operations your dairy handles",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Quick Preset Chips
            Text("QUICK PRESET PROFILES:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        onManageHerdChange(true)
                        onSellHouseholdsChange(true)
                        onProcureFarmersChange(false)
                        onSellCommercialChange(false)
                        onByproductsChange(false)
                        onCattleFeedChange(true)
                    },
                    label = { Text("🐄 Farm Producer", fontSize = 11.5.sp) }
                )
                AssistChip(
                    onClick = {
                        onManageHerdChange(false)
                        onSellHouseholdsChange(false)
                        onProcureFarmersChange(true)
                        onSellCommercialChange(true)
                        onByproductsChange(false)
                        onCattleFeedChange(false)
                    },
                    label = { Text("🏢 Collection Center", fontSize = 11.5.sp) }
                )
                AssistChip(
                    onClick = {
                        onManageHerdChange(false)
                        onSellHouseholdsChange(true)
                        onProcureFarmersChange(true)
                        onSellCommercialChange(false)
                        onByproductsChange(false)
                        onCattleFeedChange(false)
                    },
                    label = { Text("🚚 Doorstep Delivery", fontSize = 11.5.sp) }
                )
                AssistChip(
                    onClick = {
                        onManageHerdChange(true)
                        onSellHouseholdsChange(true)
                        onProcureFarmersChange(true)
                        onSellCommercialChange(true)
                        onByproductsChange(true)
                        onCattleFeedChange(true)
                    },
                    label = { Text("⭐ All-in-One Integrated", fontSize = 11.5.sp) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Visual Toggle Rows matching SettingsScreen
            SignupFeatureRow(
                icon = Icons.Default.Pets,
                iconColor = DairyGreen,
                title = "🐄 Cattle Herd & Milking Records",
                description = "Cow & buffalo profiles, milking yield records, AI breeding dates & veterinary health",
                checked = manageHerd,
                onCheckedChange = onManageHerdChange
            )

            SignupFeatureRow(
                icon = Icons.Default.WaterDrop,
                iconColor = RoyalBluePrimary,
                title = "🥛 Village Farmer Milk Collection Desk",
                description = "Record milk intake from neighbor farmers, test FAT/SNF, generate printed slips & farmer ledgers",
                checked = procureFromFarmers,
                onCheckedChange = onProcureFarmersChange
            )

            SignupFeatureRow(
                icon = Icons.Default.LocalShipping,
                iconColor = FreshGold,
                title = "🚚 Doorstep Customer Delivery Routes",
                description = "Morning & evening home delivery routes, bottle/packet drops & monthly customer khatas",
                checked = sellHouseholds,
                onCheckedChange = onSellHouseholdsChange
            )

            SignupFeatureRow(
                icon = Icons.Default.Storefront,
                iconColor = Color(0xFF0284C7),
                title = "🏨 Hotels, Restaurants & B2B Commercial Supply",
                description = "Dedicated commercial B2B accounts, wholesale pricing, bulk delivery challans",
                checked = sellCommercial,
                onCheckedChange = onSellCommercialChange
            )

            SignupFeatureRow(
                icon = Icons.Default.Fastfood,
                iconColor = DairyGreen,
                title = "🧀 Milk Byproducts & Product Inventory",
                description = "Manufacture paneer, ghee, curd/dahi, butter & khoya from raw milk, track yield & stock",
                checked = hasByproducts,
                onCheckedChange = onByproductsChange
            )

            SignupFeatureRow(
                icon = Icons.Default.Agriculture,
                iconColor = Color(0xFF795548),
                title = "🌾 Cattle Feed & Fodder Management",
                description = "Track cattle feed bags, feeding logs & farmer supply",
                checked = hasCattleFeed,
                onCheckedChange = onCattleFeedChange
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.8f).height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.2f).height(48.dp).testTag("signup_step2_next_btn")
                ) {
                    Text("Next: Dairy Details", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * Visual Feature Row with Switch for Step 2
 */
@Composable
fun SignupFeatureRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (checked) RoyalBlueLight.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, if (checked) RoyalBluePrimary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Text(text = description, fontSize = 10.5.sp, color = TextSecondary, lineHeight = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RoyalBluePrimary)
            )
        }
    }
}

/**
 * SIGNUP STEP 3: Dairy Name, Owner Name & Security PIN
 */
@Composable
fun SignupStep3DetailsCard(
    phone: String,
    dairyName: String,
    onDairyNameChange: (String) -> Unit,
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    securityPin: String,
    onPinChange: (String) -> Unit,
    cowRate: String,
    onCowRateChange: (String) -> Unit,
    buffaloRate: String,
    onBuffaloRateChange: (String) -> Unit,
    selectedLanguage: String,
    onLanguageClick: () -> Unit,
    onBack: () -> Unit,
    onLaunch: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("signup_step3_card")
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = RoyalBlueLight,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Dairy & Owner Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Step 3 of 3: Enter dairy name, owner name & PIN",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Verified Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DairyGreenLight,
                border = BorderStroke(1.dp, DairyGreen.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (phone.isNotBlank()) "Verified Account: +91 $phone" else "Google Account Verified ✓",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF1B5E20)
                    )
                }
            }

            // Dairy Name
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = dairyName,
                    onValueChange = onDairyNameChange,
                    label = { Text("Dairy / Business Name *") },
                    placeholder = { Text("e.g. Gokul Milk Dairy & Farm") },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = RoyalBluePrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("signup_dairy_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Gokul Dairy", "Kisan Milk", "Shree Krishna", "Green Valley Farm").forEach { suggestion ->
                        AssistChip(
                            onClick = { onDairyNameChange(suggestion) },
                            label = { Text(suggestion, fontSize = 11.sp) },
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }
            }

            // Owner Name & 4-Digit Security PIN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = onOwnerNameChange,
                    label = { Text("Owner / Incharge Name") },
                    placeholder = { Text("e.g. Ramesh Patel") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RoyalBluePrimary) },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f).testTag("signup_owner_name_input")
                )

                OutlinedTextField(
                    value = securityPin,
                    onValueChange = {
                        if (it.length <= 4) onPinChange(it.filter { c -> c.isDigit() })
                    },
                    label = { Text("4-Digit PIN") },
                    placeholder = { Text("••••") },
                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = RoyalBluePrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.weight(0.9f).testTag("signup_pin_input")
                )
            }

            // Baseline Milk Rates
            Text("BASELINE DEFAULT MILK RATES (₹ / LITER):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = cowRate,
                    onValueChange = onCowRateChange,
                    label = { Text("🐄 Cow Rate (₹/L)") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = RoyalBluePrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = buffaloRate,
                    onValueChange = onBuffaloRateChange,
                    label = { Text("🐃 Buffalo Rate (₹/L)") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = RoyalBluePrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // App Language Selector
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onLanguageClick)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("App Language: ${getLanguageName(selectedLanguage)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Text("Change", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.8f).height(50.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onLaunch,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.3f).height(50.dp).testTag("signup_complete_btn")
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Complete & Launch", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 1-Screen Streamlined Quick Sign Up Card
 * Frictionless onboarding: Mobile + Dairy Name + Owner Name + Optional PIN with pre-configured defaults.
 */
@Composable
fun QuickOneScreenSignUpCard(
    phoneInput: String,
    onPhoneChange: (String) -> Unit,
    dairyName: String,
    onDairyNameChange: (String) -> Unit,
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    securityPin: String,
    onPinChange: (String) -> Unit,
    customMilkType: String,
    onMilkTypeChange: (String) -> Unit,
    customCowRate: String,
    onCowRateChange: (String) -> Unit,
    customBuffaloRate: String,
    onBuffaloRateChange: (String) -> Unit,
    hasPaneer: Boolean,
    onPaneerChange: (Boolean) -> Unit,
    hasGheeButter: Boolean,
    onGheeButterChange: (Boolean) -> Unit,
    hasCurdChaas: Boolean,
    onCurdChaasChange: (Boolean) -> Unit,
    hasKhoyaSweets: Boolean,
    onKhoyaSweetsChange: (Boolean) -> Unit,
    selectedLanguage: String,
    onLanguageClick: () -> Unit,
    showAdvanced: Boolean,
    onToggleAdvanced: () -> Unit,
    onGoogleSignUp: () -> Unit,
    onLaunch: () -> Unit,
    onSwitchToLogin: () -> Unit,
    onQuickDemo: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("quick_signup_card")
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = RoyalBlueLight,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AddBusiness, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Create Dairy Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "1-Step Setup: Start managing your dairy in seconds",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Quick 1-Tap Google Sign Up
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = RoyalBlueLight.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onGoogleSignUp)
                    .testTag("quick_signup_google_btn")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("G", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sign Up with Google Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = RoyalBluePrimary
                        )
                        Text(
                            text = "Instant 1-tap setup with automatic cloud backup",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = RoyalBluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = " OR ENTER DETAILS ",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            // Mobile Phone
            OutlinedTextField(
                value = phoneInput,
                onValueChange = {
                    if (it.length <= 10) onPhoneChange(it.filter { c -> c.isDigit() })
                },
                label = { Text("10-Digit Mobile Phone *") },
                placeholder = { Text("e.g. 9876543210") },
                leadingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                    ) {
                        Text("+91", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RoyalBluePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("signup_phone_input")
            )

            // Dairy Name
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = dairyName,
                    onValueChange = onDairyNameChange,
                    label = { Text("Dairy / Business Name *") },
                    placeholder = { Text("e.g. Gokul Milk Dairy & Farm") },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = RoyalBluePrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("signup_dairy_name_input")
                )

                // Quick suggestions
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Gokul Dairy", "Kisan Milk", "Shree Krishna", "Green Valley Farm").forEach { suggestion ->
                        AssistChip(
                            onClick = { onDairyNameChange(suggestion) },
                            label = { Text(suggestion, fontSize = 11.sp) },
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }
            }

            // Owner Name & Optional 4-Digit PIN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = onOwnerNameChange,
                    label = { Text("Owner Name") },
                    placeholder = { Text("e.g. Ramesh Patel") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RoyalBluePrimary) },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f).testTag("signup_owner_name_input")
                )

                OutlinedTextField(
                    value = securityPin,
                    onValueChange = {
                        if (it.length <= 4) onPinChange(it.filter { c -> c.isDigit() })
                    },
                    label = { Text("4-Digit PIN") },
                    placeholder = { Text("Optional") },
                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = RoyalBluePrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.weight(0.9f).testTag("signup_pin_input")
                )
            }

            // Smart Defaults Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DairyGreenLight,
                border = BorderStroke(1.dp, DairyGreen.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pre-Configured Dairy Defaults",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "✓ Cattle Herd (Cow & Buffalo)  ✓ Direct Household Delivery  ✓ Daily Reports & P&L",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // Collapsible Advanced Settings
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleAdvanced)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Advanced Rates & Products (Optional)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Icon(
                            imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (showAdvanced) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Milk Type
                        Text("Milk Cattle Handled:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("COW_ONLY", "🐄 Cow", "Cow"),
                                Triple("BUFFALO_ONLY", "🐃 Buffalo", "Buffalo"),
                                Triple("BOTH", "🥛 Both (Cow & Buffalo)", "Both")
                            ).forEach { (code, label, _) ->
                                val isSelected = customMilkType == code
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onMilkTypeChange(code) },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Default Milk Rates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = customCowRate,
                                onValueChange = onCowRateChange,
                                label = { Text("Cow Rate (₹/L)") },
                                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = RoyalBluePrimary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customBuffaloRate,
                                onValueChange = onBuffaloRateChange,
                                label = { Text("Buffalo Rate (₹/L)") },
                                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = RoyalBluePrimary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Byproducts
                        Text("Value-Added Dairy Products:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = hasPaneer,
                                onClick = { onPaneerChange(!hasPaneer) },
                                label = { Text("🧀 Paneer", fontSize = 10.5.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = hasGheeButter,
                                onClick = { onGheeButterChange(!hasGheeButter) },
                                label = { Text("🧈 Ghee", fontSize = 10.5.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = hasCurdChaas,
                                onClick = { onCurdChaasChange(!hasCurdChaas) },
                                label = { Text("🍶 Curd", fontSize = 10.5.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = hasKhoyaSweets,
                                onClick = { onKhoyaSweetsChange(!hasKhoyaSweets) },
                                label = { Text("🍬 Khoya", fontSize = 10.5.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Language Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth().clickable(onClick = onLanguageClick)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("App Language: ${getLanguageName(selectedLanguage)}", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                }
                                Text("Change", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Primary Launch Action
            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("launch_dairy_btn")
            ) {
                Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Launch Dairy Workspace", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // Footer Switch to Login
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already registered? ", fontSize = 13.sp, color = TextSecondary)
                Text(
                    text = "Log In Here",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBluePrimary,
                    modifier = Modifier.clickable(onClick = onSwitchToLogin)
                )
            }
        }
    }
}

/**
 * Visual Stepper for New Dairy Signup
 */
@Composable
fun SignupStepperHeader(currentStep: Int, onStepClick: (Int) -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SignupStepPill(step = 1, title = "1. Identity", active = currentStep == 1, completed = currentStep > 1, onClick = { onStepClick(1) })
                HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), color = if (currentStep > 1) DairyGreen else MaterialTheme.colorScheme.outlineVariant)
                SignupStepPill(step = 2, title = "2. Activities", active = currentStep == 2, completed = currentStep > 2, onClick = { onStepClick(2) })
                HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), color = if (currentStep > 2) DairyGreen else MaterialTheme.colorScheme.outlineVariant)
                SignupStepPill(step = 3, title = "3. Customise", active = currentStep == 3, completed = currentStep > 3, onClick = { onStepClick(3) })
                HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), color = if (currentStep > 3) DairyGreen else MaterialTheme.colorScheme.outlineVariant)
                SignupStepPill(step = 4, title = "4. Launch", active = currentStep == 4, completed = false, onClick = { onStepClick(4) })
            }
        }
    }
}

@Composable
fun SignupStepPill(step: Int, title: String, active: Boolean, completed: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(enabled = completed, onClick = onClick)
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                completed -> DairyGreen
                active -> RoyalBluePrimary
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (completed) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                } else {
                    Text(
                        text = "$step",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) RoyalBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * SIGNUP STEP 1: Phone, Verified OTP & Dairy Profile
 */
@Composable
fun SignupStep1Identity(
    phoneInput: String,
    onPhoneChange: (String) -> Unit,
    otpInput: String,
    onOtpChange: (String) -> Unit,
    generatedOtp: String?,
    isOtpVerified: Boolean,
    timerSeconds: Int,
    isVerifying: Boolean,
    dairyName: String,
    onDairyNameChange: (String) -> Unit,
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    securityPin: String,
    onPinChange: (String) -> Unit,
    enableBiometric: Boolean,
    onBiometricChange: (Boolean) -> Unit,
    onGoogleSignUp: () -> Unit,
    onSendOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    onNext: () -> Unit,
    onQuickDemo: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("signup_step1_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = RoyalBlueLight,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AddBusiness, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Step 1: Dairy & Account Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Set up your registered phone & dairy profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Quick 1-Tap Google Sign Up
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGoogleSignUp() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(28.dp),
                        shadowElevation = 1.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = "Google",
                                tint = RoyalBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sign Up with Google Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Instant 1-Tap Cloud Sync & automatic backup",
                            fontSize = 10.5.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = RoyalBluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "  OR USE MOBILE NUMBER  ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            // 1. Mobile Phone Input & Instant OTP Verify
            Column {
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = {
                        if (it.length <= 10) onPhoneChange(it.filter { c -> c.isDigit() })
                    },
                    label = { Text("10-Digit Mobile Phone *") },
                    placeholder = { Text("e.g. 9876543210") },
                    leadingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                        ) {
                            Text("+91", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RoyalBluePrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.height(20.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        }
                    },
                    trailingIcon = {
                        if (isOtpVerified) {
                            Icon(Icons.Default.Verified, contentDescription = "Verified", tint = DairyGreen)
                        } else if (phoneInput.length == 10) {
                            TextButton(onClick = onSendOtp) {
                                Text("Get OTP", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("signup_phone_input")
                )

                // If OTP was dispatched
                if (generatedOtp != null && !isOtpVerified) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoyalBlueLight.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SMS OTP sent: $generatedOtp", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                                if (timerSeconds > 0) {
                                    Text("${timerSeconds}s", fontSize = 11.sp, color = TextSecondary)
                                } else {
                                    TextButton(onClick = onSendOtp, contentPadding = PaddingValues(0.dp)) {
                                        Text("Resend", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = otpInput,
                                    onValueChange = { if (it.length <= 6) onOtpChange(it.filter { c -> c.isDigit() }) },
                                    label = { Text("6-Digit OTP") },
                                    placeholder = { Text("123456") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = onVerifyOtp,
                                    enabled = otpInput.length == 6,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Text("Verify", fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "💡 Dev Mode: OTP is automatically filled above for instant testing. In production APK, SMS is sent to your mobile carrier.",
                                fontSize = 9.5.sp,
                                color = RoyalBluePrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // 2. Dairy Name & Quick Suggestions
            Column {
                OutlinedTextField(
                    value = dairyName,
                    onValueChange = onDairyNameChange,
                    label = { Text("Dairy / Business Name *") },
                    placeholder = { Text("e.g. Gokul Milk Collection Centre") },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = RoyalBluePrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("signup_dairy_name_input")
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Gokul Dairy", "Kisan Milk", "Shree Krishna", "Green Valley Farm").forEach { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { onDairyNameChange(suggestion) }
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = RoyalBluePrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // 3. Owner Name & Optional Fast 4-Digit PIN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = onOwnerNameChange,
                    label = { Text("Owner / Incharge Name") },
                    placeholder = { Text("Optional") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RoyalBluePrimary) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = securityPin,
                    onValueChange = { if (it.length <= 4) onPinChange(it.filter { c -> c.isDigit() }) },
                    label = { Text("4-Digit PIN") },
                    placeholder = { Text("Optional") },
                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = RoyalBluePrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Biometric Toggle if PIN provided
            if (securityPin.length == 4) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable Fingerprint / Face Unlock", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = enableBiometric,
                            onCheckedChange = onBiometricChange,
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CTA Button to Next Step
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("signup_step1_next_btn")
            ) {
                Text("Next: Customize Based on Your Activity", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * SIGNUP STEP 2: Modular Dairy Operations ("Turn On What Your Dairy Does")
 */
@Composable
fun SignupStep2ActivityCustomization(
    manageHerd: Boolean,
    onManageHerdChange: (Boolean) -> Unit,
    herdSpecies: String,
    onHerdSpeciesChange: (String) -> Unit,
    procureFromFarmers: Boolean,
    onProcureFarmersChange: (Boolean) -> Unit,
    bulkInward: Boolean,
    onBulkInwardChange: (Boolean) -> Unit,
    sellHouseholds: Boolean,
    onSellHouseholdsChange: (Boolean) -> Unit,
    sellRestaurants: Boolean,
    onSellRestaurantsChange: (Boolean) -> Unit,
    sellTraders: Boolean,
    onSellTradersChange: (Boolean) -> Unit,
    sellCollectionCentre: Boolean,
    onSellCollectionCentreChange: (Boolean) -> Unit,
    sellRetailCounter: Boolean,
    onSellRetailCounterChange: (Boolean) -> Unit,
    hasByproducts: Boolean,
    onHasByproductsChange: (Boolean) -> Unit,
    hasCattleFeed: Boolean,
    onHasCattleFeedChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val activeCount = listOf(
        procureFromFarmers,
        manageHerd,
        sellHouseholds,
        sellRestaurants,
        hasByproducts,
        hasCattleFeed || manageHerd
    ).count { it }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("signup_step2_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = FreshGoldLight,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = FreshGold, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Step 2: Choose Dairy Operations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Turn on what your dairy does. All tools stay fully customizable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Text(
                "WHAT DOES YOUR DAIRY DO? (TURN ON WHAT YOU NEED):",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = RoyalBluePrimary
            )

            // Operation 1: Village Farmer Milk Collection Desk
            ActivityOptionCard(
                icon = Icons.Default.WaterDrop,
                title = "🥛 Village Farmer Milk Collection Desk",
                subtitle = "Collect milk from neighbor farmers, test FAT/SNF, generate printed slips & manage farmer accounts",
                isSelected = procureFromFarmers,
                onClick = { onProcureFarmersChange(!procureFromFarmers) }
            )

            // Operation 2: Own Cattle Herd & Milking Records
            ActivityOptionCard(
                icon = Icons.Default.Pets,
                title = "🐄 Own Cattle Herd & Milking Records",
                subtitle = "Track cows/buffaloes, morning & evening milking yield, lactation, AI breeding & health",
                isSelected = manageHerd,
                onClick = { onManageHerdChange(!manageHerd) }
            )

            // If Herd is managed, show species selector
            AnimatedVisibility(visible = manageHerd) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GrassGreen.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, GrassGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Select Cattle Species on Farm:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GrassGreen)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = herdSpecies == "COW",
                                onClick = { onHerdSpeciesChange("COW") },
                                label = { Text("🐄 Cow Only", fontSize = 10.5.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = herdSpecies == "BUFFALO",
                                onClick = { onHerdSpeciesChange("BUFFALO") },
                                label = { Text("🐃 Buffalo Only", fontSize = 10.5.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = herdSpecies == "BOTH",
                                onClick = { onHerdSpeciesChange("BOTH") },
                                label = { Text("🥛 Both (Dual)", fontSize = 10.5.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Operation 3: Doorstep Household Delivery Routes
            ActivityOptionCard(
                icon = Icons.Default.LocalShipping,
                title = "🚚 Doorstep Household Delivery Routes",
                subtitle = "Morning & evening home delivery routes, bottle/packet drops & monthly customer khatas",
                isSelected = sellHouseholds,
                onClick = { onSellHouseholdsChange(!sellHouseholds) }
            )

            // Operation 4: Hotels, Restaurants & B2B Commercial Supply
            ActivityOptionCard(
                icon = Icons.Default.Restaurant,
                title = "🏨 Hotels, Restaurants & B2B Commercial Supply",
                subtitle = "Supply bulk milk & products to hotels, restaurants, cafes & halwais with commercial billing",
                isSelected = sellRestaurants,
                onClick = { onSellRestaurantsChange(!sellRestaurants) }
            )

            // Operation 5: Milk Byproducts & Product Inventory (Paneer, Ghee, Curd, Khoya)
            ActivityOptionCard(
                icon = Icons.Default.Fastfood,
                title = "🧀 Milk Byproducts & Product Inventory",
                subtitle = "Manufacture paneer, ghee, curd, butter & khoya from raw milk, track yield & manage stock",
                isSelected = hasByproducts,
                onClick = { onHasByproductsChange(!hasByproducts) }
            )

            // Operation 6: Cattle Feed & Fodder Management
            ActivityOptionCard(
                icon = Icons.Default.Agriculture,
                title = "🌾 Cattle Feed & Fodder Management",
                subtitle = "Stock mustard cake (khal), cottonseed (binola), churi, pellets, feeding logs & farmer supply",
                isSelected = hasCattleFeed || manageHerd,
                onClick = { onHasCattleFeedChange(!hasCattleFeed) }
            )

            // Dynamic Active Summary Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DairyGreenLight,
                border = BorderStroke(1.dp, DairyGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("✓ $activeCount Modules Active for Your Dairy", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF1B5E20))
                        Text("Navigation tabs will adapt automatically. You can toggle any tool in Settings anytime without resetting data.", fontSize = 10.5.sp, color = TextSecondary)
                    }
                }
            }

            // Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.8f).height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.2f).height(48.dp).testTag("signup_step2_next_btn")
                ) {
                    Text("Next: Rates & Pricing", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun ActivityOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) RoyalBlueLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = if (isSelected) RoyalBluePrimary else TextPrimary)
                Text(subtitle, fontSize = 10.5.sp, color = TextSecondary)
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = RoyalBluePrimary)
            )
        }
    }
}

/**
 * SIGNUP STEP 3: Deep Customisation (Milk Species, Pricing Formula, Baseline Rates, Byproducts & Language)
 */
@Composable
fun SignupStep3CustomiseRules(
    milkType: String,
    onMilkTypeChange: (String) -> Unit,
    pricingMode: String,
    onPricingModeChange: (String) -> Unit,
    cowRate: String,
    onCowRateChange: (String) -> Unit,
    buffaloRate: String,
    onBuffaloRateChange: (String) -> Unit,
    hasPaneer: Boolean,
    onPaneerChange: (Boolean) -> Unit,
    hasGheeButter: Boolean,
    onGheeButterChange: (Boolean) -> Unit,
    hasCurdChaas: Boolean,
    onCurdChaasChange: (Boolean) -> Unit,
    hasKhoyaSweets: Boolean,
    onKhoyaSweetsChange: (Boolean) -> Unit,
    hasCattleFeed: Boolean,
    onCattleFeedChange: (Boolean) -> Unit,
    selectedLanguage: String,
    onLanguageClick: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("signup_step3_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = RoyalBlueLight,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Step 3: Customise Your Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Tailor species, rates, pricing formula, byproducts & modules",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            HorizontalDivider()

            // 1. Supported Milk Species Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("1. Milk Species / Varieties Handled:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("COW_ONLY", "🐄 Cow Only", "Cow milk"),
                        Triple("BUFFALO_ONLY", "🐃 Buffalo Only", "Buffalo"),
                        Triple("BOTH", "🥛 Dual (Cow & Buffalo)", "Both")
                    ).forEach { (code, label, _) ->
                        val isSelected = milkType == code
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) RoyalBlueLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onMilkTypeChange(code) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) RoyalBluePrimary else TextPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 2. Milk Rate & Pricing Method Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("2. Milk Rate & Pricing Calculation Method:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("FAT_SNF", "🧪 FAT & SNF Chart", "Standard Dairy testing formula"),
                        Triple("DIRECT", "🏷️ Fixed Flat Rate/L", "Fixed ₹ per liter for each customer"),
                        Triple("FAT_ONLY", "⚖️ FAT-Only Rate", "Formula on FAT % only")
                    ).forEach { (mode, label, _) ->
                        val isSelected = pricingMode == mode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) DairyGreenLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) DairyGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onPricingModeChange(mode) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF1B5E20) else TextPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 3. Baseline Rates Configuration
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("3. Baseline Default Milk Rates (₹ / Liter):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = cowRate,
                        onValueChange = onCowRateChange,
                        label = { Text("🐄 Cow Rate (₹/L)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = RoyalBluePrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = buffaloRate,
                        onValueChange = onBuffaloRateChange,
                        label = { Text("🐃 Buffalo Rate (₹/L)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = RoyalBluePrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Dairy Byproducts & Value Addition Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("4. Dairy Value-Added Byproducts Handled:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = hasPaneer,
                        onClick = { onPaneerChange(!hasPaneer) },
                        label = { Text("🧀 Paneer", fontSize = 10.5.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = hasGheeButter,
                        onClick = { onGheeButterChange(!hasGheeButter) },
                        label = { Text("🧈 Ghee", fontSize = 10.5.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = hasCurdChaas,
                        onClick = { onCurdChaasChange(!hasCurdChaas) },
                        label = { Text("🍶 Curd", fontSize = 10.5.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = hasKhoyaSweets,
                        onClick = { onKhoyaSweetsChange(!hasKhoyaSweets) },
                        label = { Text("🍬 Khoya", fontSize = 10.5.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Feed Module Status
            if (hasCattleFeed) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DairyGreenLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🌾 Cattle Feed & Consumables module is active for your cattle herd.", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B5E20))
                    }
                }
            }

            // 6. Preferred App Language
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLanguageClick)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Preferred App Language", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Current: ${getLanguageName(selectedLanguage)}", fontSize = 10.5.sp, color = TextSecondary)
                        }
                    }
                    Text("Change", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                }
            }

            // Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.8f).height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.2f).height(48.dp).testTag("signup_step3_next_btn")
                ) {
                    Text("Next: Review & Launch", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * SIGNUP STEP 4: Live Setup Summary & 1-Tap Launch
 */
@Composable
fun SignupStep4SummaryLaunch(
    dairyName: String,
    ownerName: String,
    phone: String,
    procureFromFarmers: Boolean,
    manageHerd: Boolean,
    sellHouseholds: Boolean,
    sellRestaurants: Boolean,
    milkType: String,
    pricingMode: String,
    cowRate: String,
    buffaloRate: String,
    hasPaneer: Boolean,
    hasGheeButter: Boolean,
    hasCurdChaas: Boolean,
    hasKhoyaSweets: Boolean,
    hasCattleFeed: Boolean,
    securityPin: String,
    enableBiometric: Boolean,
    selectedLanguage: String,
    onBack: () -> Unit,
    onLaunch: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("signup_step4_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = DairyGreenLight,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Ready to Launch Your Dairy!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Review your tailored workspace settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            HorizontalDivider()

            // Summary Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = RoyalBlueLight.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("WORKSPACE CONFIGURATION:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Dairy Name:", fontSize = 12.sp, color = TextSecondary)
                        Text(dairyName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Owner / Incharge:", fontSize = 12.sp, color = TextSecondary)
                        Text(ownerName.ifBlank { "Owner" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Mobile Contact:", fontSize = 12.sp, color = TextSecondary)
                        Text("+91 $phone", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    HorizontalDivider(color = RoyalBluePrimary.copy(alpha = 0.2f))

                    Text("ACTIVE DAIRY OPERATIONS & MODULES:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    if (procureFromFarmers) {
                        Text("✓ 🥛 Village Farmer Milk Collection Desk", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B5E20))
                    }
                    if (manageHerd) {
                        Text("✓ 🐄 Own Cattle Herd & Milking Records", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                    }
                    if (sellHouseholds) {
                        Text("✓ 🚚 Doorstep Household Delivery Routes", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE65100))
                    }
                    if (sellRestaurants) {
                        Text("✓ 🏨 Hotels, Restaurants & B2B Commercial Supply", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = RoyalBluePrimary)
                    }
                    if (hasPaneer || hasGheeButter || hasCurdChaas || hasKhoyaSweets) {
                        Text("✓ 🧀 Milk Byproducts (Paneer, Ghee, Curd, Khoya)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B5E20))
                    }
                    if (hasCattleFeed) {
                        Text("✓ 🌾 Cattle Feed & Fodder Management", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF795548))
                    }

                    HorizontalDivider(color = RoyalBluePrimary.copy(alpha = 0.2f))

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Milk Species:", fontSize = 12.sp, color = TextSecondary)
                        Text(when (milkType) {
                            "COW_ONLY" -> "🐄 Cow Only"
                            "BUFFALO_ONLY" -> "🐃 Buffalo Only"
                            else -> "🥛 Cow & Buffalo"
                        }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Rate Calculation:", fontSize = 12.sp, color = TextSecondary)
                        Text(when (pricingMode) {
                            "FAT_SNF" -> "🧪 FAT & SNF Chart"
                            "FAT_ONLY" -> "⚖️ FAT-Only Rate"
                            else -> "🏷️ Fixed Flat Rate"
                        }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Baseline Rates:", fontSize = 12.sp, color = TextSecondary)
                        Text("Cow: ₹$cowRate/L • Buffalo: ₹$buffaloRate/L", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    if (hasCattleFeed) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Active Modules:", fontSize = 12.sp, color = TextSecondary)
                            Text("Cattle Feed & Consumables", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = DairyGreen)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Volume Reconciliation:", fontSize = 12.sp, color = TextSecondary)
                        Text("Active (Wastage & Losses)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Fast Login PIN:", fontSize = 12.sp, color = TextSecondary)
                        Text(if (securityPin.length == 4) "Set (4-Digit PIN)" else "None", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
            }

            // Big Launch CTA
            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("launch_dairy_workspace_btn")
            ) {
                Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("🚀 Launch My Tailored Dairy Workspace", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Back to Edit Customisation", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}




/**
 * Language Selection Dialog for Signup
 */
@Composable
fun SignupLanguageModal(
    currentLang: String,
    onSelectLang: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        Pair("en", "English"),
        Pair("hi", "हिंदी (Hindi)"),
        Pair("mr", "मराठी (Marathi)"),
        Pair("gu", "ગુજરાતી (Gujarati)"),
        Pair("ta", "தமிழ் (Tamil)"),
        Pair("te", "తెలుగు (Telugu)"),
        Pair("kn", "ಕನ್ನಡ (Kannada)"),
        Pair("pa", "ਪੰਜਾਬੀ (Punjabi)"),
        Pair("bn", "বাংলা (Bengali)")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select App Language", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.forEach { (code, name) ->
                    val isSelected = currentLang == code
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) RoyalBlueLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLang(code) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = RoyalBluePrimary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

fun getLanguageName(code: String): String {
    return when (code) {
        "hi" -> "हिंदी (Hindi)"
        "mr" -> "मराठी (Marathi)"
        "gu" -> "ગુજરાતી (Gujarati)"
        "ta" -> "தமிழ் (Tamil)"
        "te" -> "తెలుగు (Telugu)"
        "kn" -> "ಕನ್ನಡ (Kannada)"
        "pa" -> "ਪੰਜਾਬੀ (Punjabi)"
        "bn" -> "বাংলা (Bengali)"
        else -> "English"
    }
}

/**
 * STEP 1 Phone Input Helper
 */
@Composable
fun Step1PhoneInput(
    phoneInput: String,
    onPhoneChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    onSendWhatsAppOtp: () -> Unit = {},
    onSwitchToStaff: () -> Unit,
    onSwitchToPin: () -> Unit,
    onSwitchToAccounts: () -> Unit,
    onQuickDemo: () -> Unit,
    onGoogleSignIn: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("auth_step1_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = RoyalBlueLight,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Enter Mobile Number",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "We will send an instant SMS verification code",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = phoneInput,
                onValueChange = {
                    if (it.length <= 10) {
                        onPhoneChange(it.filter { c -> c.isDigit() })
                    }
                },
                label = { Text("10-Digit Mobile Phone *") },
                placeholder = { Text("e.g. 9876543210") },
                leadingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                    ) {
                        Text("+91", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RoyalBluePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("otp_phone_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSendOtp,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("send_otp_btn")
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SMS OTP", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSendWhatsAppOtp,
                    border = BorderStroke(1.dp, DairyGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("login_whatsapp_otp_btn")
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp OTP", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onGoogleSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("google_signin_btn"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Google", tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign In with Google Account", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Clean, fast switch to 4-Digit PIN or Fingerprint unlock
            OutlinedButton(
                onClick = onSwitchToPin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("switch_to_pin_btn"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(Icons.Default.Pin, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Login with 4-Digit PIN or Fingerprint", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
        }
    }
}

/**
 * STEP 2: Fast OTP Verification Card
 */
@Composable
fun Step2OtpVerification(
    phone: String,
    otpInput: String,
    generatedOtp: String?,
    timerSeconds: Int,
    isVerifying: Boolean,
    onOtpChange: (String) -> Unit,
    onEditPhone: () -> Unit,
    onResendOtp: () -> Unit,
    onVerifyOtp: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("auth_step2_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = DairyGreenLight,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Verify OTP Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Sent to +91 $phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Verification Status Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DairyGreenLight,
                border = BorderStroke(1.dp, DairyGreen.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Verification Code Sent", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DairyGreen)
                        if (timerSeconds > 0) {
                            Text("${timerSeconds}s", fontSize = 11.sp, color = TextSecondary)
                        } else {
                            TextButton(onClick = onResendOtp, contentPadding = PaddingValues(0.dp)) {
                                Text("Resend", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                            }
                        }
                    }
                    Text(
                        text = "Please check your SMS inbox or WhatsApp chat for the 6-digit verification code sent to +91 $phone.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = otpInput,
                onValueChange = {
                    if (it.length <= 6) {
                        onOtpChange(it.filter { c -> c.isDigit() })
                    }
                },
                label = { Text("6-Digit Verification Code *") },
                placeholder = { Text("123456") },
                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = RoyalBluePrimary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("otp_code_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onVerifyOtp,
                enabled = otpInput.length == 6 && !isVerifying,
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("verify_otp_btn")
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Verify & Continue", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEditPhone) {
                    Text("Edit Mobile Number", fontSize = 12.sp)
                }

                if (timerSeconds > 0) {
                    Text("Resend code in ${timerSeconds}s", fontSize = 12.sp, color = TextSecondary)
                } else {
                    TextButton(onClick = onResendOtp) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resend OTP Code", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * PIN & Biometric Fast Login Card
 */
@Composable
fun PinBiometricLoginCard(
    viewModel: MilkMateViewModel,
    onSwitchToOtp: () -> Unit,
    onSwitchToRegister: () -> Unit
) {
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pinText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var isPasswordMode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = FreshGoldLight,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = FreshGold, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Security PIN & Biometric Unlock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    Text("Unlock saved dairy session securely", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Biometric Fingerprint Button
            Button(
                onClick = {
                    if (context is FragmentActivity) {
                        BiometricAuthHelper.authenticate(
                            activity = context,
                            title = "MilkMate Biometric Login",
                            subtitle = "Verify Fingerprint or Device Screen Lock",
                            onSuccess = {
                                viewModel.unlockAppWithBiometrics { }
                            },
                            onError = { err ->
                                errorMessage = err
                            }
                        )
                    } else {
                        errorMessage = "Biometrics requires device support."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Login with Fingerprint / Screen Lock", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(" OR ENTER PIN / PASSWORD ", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (!isPasswordMode) {
                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 4) pinText = it.filter { c -> c.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("4-Digit Security PIN") },
                    placeholder = { Text("••••") },
                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = RoyalBluePrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (pinText.length == 4) {
                            viewModel.loginWithPin(pinText) { ok ->
                                if (!ok) errorMessage = "Invalid PIN. Please try again."
                            }
                        } else {
                            errorMessage = "Please enter full 4-digit PIN"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen)
                ) {
                    Text("Unlock with PIN", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            } else {
                OutlinedTextField(
                    value = passwordText,
                    onValueChange = {
                        passwordText = it
                        errorMessage = null
                    },
                    label = { Text("Master Password") },
                    leadingIcon = { Icon(Icons.Default.Password, contentDescription = null, tint = RoyalBluePrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (passwordText.isNotBlank()) {
                            viewModel.loginWithPassword(passwordText) { ok ->
                                if (!ok) errorMessage = "Invalid password."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen)
                ) {
                    Text("Unlock with Password", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { isPasswordMode = !isPasswordMode }) {
                    Text(if (isPasswordMode) "Use 4-Digit PIN instead" else "Use Password instead", fontSize = 12.sp)
                }

                TextButton(onClick = onSwitchToOtp) {
                    Text("← Login via Mobile OTP", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Staff & Delivery Boy Login Card
 */
@Composable
fun StaffAuthCard(viewModel: MilkMateViewModel) {
    var businessId by remember { mutableStateOf("") }
    var staffId by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val existingBusinesses by viewModel.existingBusinesses.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadExistingBusinesses()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Staff & Delivery Boy Login",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = RoyalBluePrimary
            )
            Text(
                text = "Access your assigned delivery routes and daily customer entries using your credentials.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (errorMsg != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Detected Dairies Quick Picker
            if (existingBusinesses.isNotEmpty()) {
                Text(
                    text = "SELECT SAVED DAIRY (OR ENTER ID BELOW):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    existingBusinesses.forEach { biz ->
                        val isSelected = businessId == biz.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                businessId = biz.id
                                errorMsg = null
                            },
                            label = {
                                Text(
                                    text = "${biz.businessName} (${biz.id.take(8)})",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = businessId,
                onValueChange = {
                    businessId = it
                    errorMsg = null
                },
                label = { Text("Business Code / ID (e.g. BIZ-XXXX)") },
                placeholder = { Text("Provided by dairy owner") },
                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = RoyalBluePrimary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("staff_biz_id_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = staffId,
                onValueChange = {
                    staffId = it
                    errorMsg = null
                },
                label = { Text("Staff ID or Registered Mobile") },
                placeholder = { Text("e.g. STF-101 or 9876543210") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = RoyalBluePrimary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("staff_id_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    pin = it
                    errorMsg = null
                },
                label = { Text("4-Digit Security PIN") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = RoyalBluePrimary) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("staff_pin_input")
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (businessId.isNotBlank() && staffId.isNotBlank() && pin.isNotBlank()) {
                        viewModel.loginStaff(businessId, staffId, pin) { success, err ->
                            if (!success) {
                                errorMsg = err
                            }
                        }
                    } else {
                        errorMsg = "Please fill in Business ID, Staff ID, and PIN"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("login_as_staff_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Login, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Login as Staff", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Saved Dairies Card (Multi-Dairy Switcher)
 */
@Composable
fun OwnerLoginCard(
    viewModel: MilkMateViewModel,
    onSwitchToRegister: () -> Unit
) {
    val existingBusinesses by viewModel.existingBusinesses.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadExistingBusinesses()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Saved Dairy Accounts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = RoyalBluePrimary
            )
            Text(
                text = "Switch between multiple saved dairy workspaces on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (existingBusinesses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No local dairy accounts found.", fontSize = 13.sp, color = TextSecondary)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    existingBusinesses.forEach { biz ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.loginOwnerDirect(biz.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(biz.businessName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${biz.ownerName} • ${biz.phone}", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
