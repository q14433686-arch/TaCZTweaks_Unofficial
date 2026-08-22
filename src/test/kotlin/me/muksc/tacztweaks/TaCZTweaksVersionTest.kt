package me.muksc.tacztweaks

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaCZTweaksVersionTest {
    @Test
    fun acceptsR2AndLaterRevisionsInTheCurrentFamily() {
        listOf(
            "1.1.8+fabric.1.21.11.R2",
            "1.1.8+fabric.1.21.11.R2-hotfix",
            "1.1.8+fabric.1.21.11.R3",
            "1.1.8+fabric.1.21.11.R10",
            "1.1.8+fabric.1.21.11.R10-preview",
        ).forEach { version -> assertTrue(TaCZTweaks.isSupportedTaczVersion(version), version) }
    }

    @Test
    fun rejectsPreR2AndWrongCoreOrReleaseFamily() {
        listOf(
            "1.1.8+fabric.1.21.11.R1",
            "1.1.8+fabric.1.21.11.R1-hotfix",
            "1.1.8",
            "1.1.9+fabric.1.21.11.R2",
            "1.1.8+fabric.26.1.2.R2",
            "1.1.8+fabric.1.21.11.R",
            "1.1.8+fabric.1.21.11.R2/invalid",
            null,
        ).forEach { version -> assertFalse(TaCZTweaks.isSupportedTaczVersion(version), version) }
    }
}
