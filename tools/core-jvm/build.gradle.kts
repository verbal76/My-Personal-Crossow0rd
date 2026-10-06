// Compiles and tests the pure-Kotlin `core` package on a plain JVM, with no Android SDK.
// Run via tools/validate_local.sh. Mirrors the core tests CI runs, so most logic changes
// can be proven locally before any GitHub Actions minutes are spent.
plugins { kotlin("jvm") version "2.3.21" }
repositories { mavenCentral() }
val app = "${rootDir}/../../app/src"
sourceSets {
    main { kotlin.srcDir("$app/main/java/com/example/mypersonalcrossword/core") }
    test {
        kotlin.srcDir("$app/test/java/com/example/mypersonalcrossword/core")
        resources.srcDir("$app/main/assets")
    }
}
dependencies { testImplementation("junit:junit:4.13.2") }
kotlin { jvmToolchain(21) }
tasks.test {
    systemProperty("crossword.assets", "$app/main/assets")
    maxHeapSize = "2g"
    testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
    afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ d, r ->
        if (d.parent == null) println("CORE TESTS: ${r.resultType} total=${r.testCount} passed=${r.successfulTestCount} failed=${r.failedTestCount}")
    }))
}
