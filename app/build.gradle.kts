import java.time.Instant
import java.time.temporal.ChronoUnit

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
// Build number, used as versionCode. It must only ever go up across every APK a
// tester might install, or Android refuses the update (and uninstalling wipes
// saves). In CI it is 1000 + the workflow's run number, which increases across
// all branches; the offset keeps it above the commit-count codes (22-24) of the
// first playtest builds. Local builds fall back to the commit count of HEAD.
val buildNumber: Int = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()?.let { 1000 + it }
    ?: gitOutput("rev-list", "--count", "HEAD")?.toIntOrNull()
    ?: 1
val buildTimeUtc: String = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
val buildOrigin: String = System.getenv("GITHUB_RUN_NUMBER")?.let { "GitHub Actions run #$it" } ?: "local build"

// ── Release signing ───────────────────────────────────────────────────────────
// The release key never lives in the repo. CI decodes it from a secret and passes
// these four variables (see docs/EXTERNAL_ACTIONS.md). Without all four, release
// builds stay unsigned exactly as before, so local and debug builds never fail.
val releaseKeystorePath: String? = System.getenv("RELEASE_KEYSTORE_PATH")?.ifBlank { null }
val releaseKeystorePassword: String? = System.getenv("RELEASE_KEYSTORE_PASSWORD")?.ifBlank { null }
val releaseKeyAlias: String? = System.getenv("RELEASE_KEY_ALIAS")?.ifBlank { null }
val releaseKeyPassword: String? = System.getenv("RELEASE_KEY_PASSWORD")?.ifBlank { null }
val hasReleaseSigning: Boolean = releaseKeystorePath != null && releaseKeystorePassword != null &&
    releaseKeyAlias != null && releaseKeyPassword != null

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
        versionName = "1.0.$buildNumber"

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
        if (hasReleaseSigning) {
            create("release") {
                storeFile     = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias      = releaseKeyAlias
                keyPassword   = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-auth")
}