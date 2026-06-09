package com.paeki.fujirecipes.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseUpdaterTest {
    @Test
    fun `version comparison handles tags and suffixes`() {
        assertTrue(isRemoteVersionNewer("1.0.0", "v1.0.1"))
        assertTrue(isRemoteVersionNewer("1.0.9", "1.1.0"))
        assertFalse(isRemoteVersionNewer("1.0.0-debug", "v1.0.0"))
        assertFalse(isRemoteVersionNewer("1.2.0", "v1.1.9"))
    }
}
