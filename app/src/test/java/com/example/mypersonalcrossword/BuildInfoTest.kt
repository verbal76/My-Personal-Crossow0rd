package com.hag.mypersonalcrossword

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards build traceability: every build must carry a real, clean source
 * commit and a build number above the old hard-coded 1. (Runs where the
 * Android build runs — CI checks out full history and a clean tree.)
 */
class BuildInfoTest {
    @Test fun buildCarriesACleanSourceCommit() {
        assertTrue("GIT_SHA was '${BuildConfig.GIT_SHA}'", Regex("[0-9a-f]{7}").matches(BuildConfig.GIT_SHA))
    }

    @Test fun buildNumberComesFromHistory() {
        assertTrue("VERSION_CODE was ${BuildConfig.VERSION_CODE}", BuildConfig.VERSION_CODE > 1)
    }

    @Test fun buildTimeIsRecorded() {
        assertTrue(BuildConfig.BUILD_TIME_UTC, Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z").matches(BuildConfig.BUILD_TIME_UTC))
    }
}
