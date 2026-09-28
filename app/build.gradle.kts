plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

// ── Build identification ──────────────────────────────────────────────────────
// Every APK carries the exact source revision it was built from, shown in-app
// (Settings → About and the login screen). CI passes BUILD_SOURCE_SHA so pull-
// request builds report the branch head rather than GitHub's temporary merge commit.
fun gitOutput(vararg args: String): String? = runCatching {
    providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().ifEmpty { null }
}.getOrNull()

val buildSourceSha: String = (System.getenv("BUILD_SOURCE_SHA")?.take(7)
    ?: gitOutput("rev-parse", "--short=7", "HEAD")
    ?: "unknown") +
    (if (!gitOutput("status", "--porcelain", "--untracked-files=no").isNullOrEmpty()) "-dirty" else "")
// Monotonic build number: commits on this history. Needs a full (not shallow) checkout.
val buildNumber: Int = gitOutput("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 1
val buildTimeUtc: String = java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString()
val buildOrigin: String = System.getenv("GITHUB_RUN_NUMBER")?.let { "GitHub Actions run #$it" } ?: "local build"

android {
    namespace = "com.hag.mypersonalcrossword"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.hag.mypersonalcrossword"
        minSdk = 24
        targetSdk = 36
        versionCode = buildNumber
        versionName = "1.0"

        buildConfigField("String", "GIT_SHA", "\"$buildSourceSha\"")
        buildConfigField("String", "BUILD_TIME_UTC", "\"$buildTimeUtc\"")
        buildConfigField("String", "BUILD_ORIGIN", "\"$buildOrigin\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Override the default debug signing config to use our committed keystore.
    // Without this, every CI runner generates its own debug key and Android
    // refuses to install a new APK over the previous one ("App not installed").
    signingConfigs {
        getByName("debug") {
            storeFile     = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias      = "androiddebugkey"
            keyPassword   = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-auth")
}