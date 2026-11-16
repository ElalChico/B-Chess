import java.util.Properties
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// El plugin de Google Services solo se aplica cuando google-services.json existe
// (no se sube al repo). Sin ese archivo el build sigue compilando; el modo
// Internet (WebRTC + Firebase) fallará en tiempo de ejecución con un mensaje
// claro en vez de romper el arranque.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val bchessVersion = providers.gradleProperty("bchessVersion").orElse("1.0.0").get()
val versionMatch = Regex("(\\d+)\\.(\\d+)\\.(\\d+)").matchEntire(bchessVersion)
    ?: throw GradleException("bchessVersion must use MAJOR.MINOR.PATCH, e.g. 1.0.1")
val versionMajor = versionMatch.groupValues[1].toLong()
val versionMinor = versionMatch.groupValues[2].toLong()
val versionPatch = versionMatch.groupValues[3].toLong()
require(versionMinor < 1_000 && versionPatch < 1_000) { "MINOR and PATCH must be below 1000" }
val bchessVersionCode = versionMajor * 1_000_000L + versionMinor * 1_000L + versionPatch
require(bchessVersionCode <= Int.MAX_VALUE) { "bchessVersion is too large for Android versionCode" }

// Firma de release: se usa keystore.properties si existe (no se sube al repo).
// Sin ese archivo, el release queda sin firmar y el proyecto sigue compilando.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.bchess.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bchess.android"
        minSdk = 26
        targetSdk = 34
        versionCode = bchessVersionCode.toInt()
        versionName = bchessVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Basura de build / tooling que no se usa en runtime: la quitamos del APK.
            excludes += "/DebugProbesKt.bin"
            excludes += "META-INF/*.version"
        }
    }
}

tasks.register("verifyBchessReleaseSigning") {
    doLast {
        if (!keystorePropsFile.exists()) {
            throw GradleException("Create keystore.properties from keystore.properties.example before publishing a B-Chess release")
        }
        val requiredKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
        val missing = requiredKeys.filter { keystoreProps.getProperty(it).isNullOrBlank() }
        if (missing.isNotEmpty()) {
            throw GradleException("Missing release signing values: ${missing.joinToString()}")
        }
    }
}

tasks.configureEach {
    if (name == "preReleaseBuild") dependsOn("verifyBchessReleaseSigning")
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // OkHttp for downloading APK from GitHub releases
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okio:okio:3.9.0")

    // WebRTC (DataChannel P2P) — fork mantenido del original org.webrtc:google-webrtc
    implementation("io.getstream:stream-webrtc-android:1.3.10")

    // Firebase Realtime Database (solo señalización: SDP/ICE; una vez conectado, queda fuera de la partida)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-database-ktx")

    // Local unit tests for the chess rules engine
    testImplementation("junit:junit:4.13.2")

    // JSON via Android built-in org.json - no extra dep needed, zero crash risk
    // Chess engine: custom pure-Kotlin engine compatible with Lichess/UCI/SAN/FEN
    // (chesslib compatible, without external dependency for max stability)

    debugImplementation("androidx.compose.ui:ui-tooling")
}
