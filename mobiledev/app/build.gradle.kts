import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Per-developer settings live in mobiledev/local.properties (git-ignored),
// or can be passed with -P on the command line:
//   labalink.apiBaseUrl=http://10.0.2.2:8080/          (debug builds)
//   labalink.apiBaseUrlRelease=https://api.example.com/ (release builds)
//   labalink.googleWebClientId=xxxx.apps.googleusercontent.com
//   labalink.termsUrl=https://.../terms   labalink.privacyUrl=https://.../privacy
val legalDocsUrl =
    "https://github.com/thatguyKire/IT342-Bien-Labalink/blob/main/docs/legal"

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun setting(key: String, default: String): String =
    (project.findProperty(key) as String?)
        ?: localProps.getProperty(key)
        ?: default

fun quoted(value: String) = "\"" + value.replace("\"", "") + "\""

android {
    namespace = "edu.cit.bien.labalink"

    compileSdk = 35

    defaultConfig {
        applicationId = "edu.cit.bien.labalink"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String", "GOOGLE_WEB_CLIENT_ID",
            quoted(setting("labalink.googleWebClientId", "")))
        buildConfigField(
            "String", "TERMS_URL",
            quoted(setting("labalink.termsUrl", "$legalDocsUrl/Terms-of-Service.md")))
        buildConfigField(
            "String", "PRIVACY_URL",
            quoted(setting("labalink.privacyUrl", "$legalDocsUrl/Privacy-Policy.md")))
    }

    buildTypes {
        debug {
            // 10.0.2.2 is the host machine as seen from the Android emulator.
            buildConfigField(
                "String", "API_BASE_URL",
                quoted(setting("labalink.apiBaseUrl", "http://10.0.2.2:8080/")))
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Release builds must use HTTPS (enforced by the network
            // security config and by RetrofitClient).
            buildConfigField(
                "String", "API_BASE_URL",
                quoted(setting(
                    "labalink.apiBaseUrlRelease",
                    "https://api.labalink.example/")))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.cardview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Networking (Retrofit 2.11 ships R8 rules for suspend functions)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.android.material:material:1.11.0")

    // Google Sign-In (the ID token is verified by the backend)
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // QR scanning
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.2")
}
