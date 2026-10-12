// Desktop (Windows + Linux/Steam Deck) build of My Personal Crossword.
//
// A separate Gradle build on purpose: the Android build (../app) is untouched. This build
// compiles the SAME pure-Kotlin `core` sources the Android app uses (srcDir below, no copy)
// and puts a Java Swing/Java2D front end on top. Only the JDK + Maven Central are needed,
// so it builds in the same sandboxes where the Android SDK cannot be downloaded.
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm") version "2.3.21"
    application
}

repositories { mavenCentral() }

val appSrc = "${rootDir}/../app/src"
val versionText = file("${rootDir}/../VERSION").readText().trim()
group = "com.hag.mypersonalcrossword"
version = versionText

sourceSets {
    main {
        // Shared game logic: the identical files Android compiles.
        kotlin.srcDir("$appSrc/main/java/com/example/mypersonalcrossword/core")
    }
    test {
        // The shared core tests also run against the desktop build.
        kotlin.srcDir("$appSrc/test/java/com/example/mypersonalcrossword/core")
    }
}

dependencies { testImplementation("junit:junit:4.13.2") }

kotlin { jvmToolchain(21) }

// Shared assets only: the word list and branding art (never the Android-only webp/mp3).
val sharedResources by tasks.registering(Copy::class) {
    into(layout.buildDirectory.dir("generated/sharedResources"))
    from("$appSrc/main/assets") { include("test.csv") }
    from("${rootDir}/../branding") {
        include("Hot_Attic_Games_Master_Logo.png", "my_personal_crossword_splash_1080x2280.png")
        into("branding")
    }
    from("packaging") { include("icon.png"); into("branding") }
    // Build identity, read by About/Smoke and packaged next to the jar.
    doLast {
        val d = layout.buildDirectory.dir("generated/sharedResources").get().asFile
        File(d, "build-info.properties").writeText(
            "version=v$versionText\n" +
            "commit=${providers.environmentVariable("GITHUB_SHA").orElse("local").get()}\n")
    }
}
sourceSets.main { resources.srcDir(sharedResources) }

application {
    mainClass.set("com.hag.mypersonalcrossword.desktop.MainKt")
    applicationName = "MyPersonalCrossword"
    applicationDefaultJvmArgs = listOf("-Xmx512m", "-Dsun.java2d.opengl=false")
}

// Runnable fat jar (what jpackage wraps, and `java -jar` runs).
tasks.jar {
    archiveFileName.set("MyPersonalCrossword.jar")
    manifest { attributes("Main-Class" to "com.hag.mypersonalcrossword.desktop.MainKt",
                          "Implementation-Version" to versionText) }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({ configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) } })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.test {
    systemProperty("crossword.assets", "$appSrc/main/assets")
    systemProperty("java.awt.headless", "true")
    maxHeapSize = "2g"
    testLogging { events("failed"); exceptionFormat = TestExceptionFormat.FULL }
    afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ d, r ->
        if (d.parent == null) println("DESKTOP TESTS: ${r.resultType} total=${r.testCount} passed=${r.successfulTestCount} failed=${r.failedTestCount}")
    }))
}
