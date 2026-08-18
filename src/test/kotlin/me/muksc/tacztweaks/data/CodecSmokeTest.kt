package me.muksc.tacztweaks.data

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import me.muksc.tacztweaks.data.core.ValueRange
import me.muksc.tacztweaks.data.manager.BULLET_INTERACTION_CODEC
import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class CodecSmokeTest {
    @Test
    fun `restored predicate tier burst and pellet fixture decodes`() {
        assertFixtureDecodes(
            "tacz-tweaks-example-pack/data/tacztweaks/bullet_interactions/schema_smoke.json",
            BulletInteraction.CODEC
        )
    }

    @Test
    fun `airspace fixture decodes`() {
        assertFixtureDecodes(
            "tacz-tweaks-example-pack/data/tacztweaks/bullet_sounds/airspace.json",
            BulletSounds.CODEC
        )
    }

    @Test
    fun `legacy v2 bullet interaction decodes and converts`() {
        val text = checkNotNull(javaClass.getResource("/fixtures/bullet_interaction_v2.json")).readText()
        val decoded = BULLET_INTERACTION_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(text))
        assertTrue(decoded.result().isPresent)
        assertTrue(decoded.result().orElseThrow() is BulletInteraction.Block)
    }

    @Test
    fun `value range includes zero and rejects reversed bounds`() {
        assertTrue(0.0 in ValueRange.DEFAULT)
        val reversed = JsonParser.parseString("""{"min":2.0,"max":1.0}""")
        assertFalse(ValueRange.CODEC.parse(JsonOps.INSTANCE, reversed).result().isPresent)
    }

    private fun <T> assertFixtureDecodes(path: String, codec: com.mojang.serialization.Codec<T>) {
        val json = JsonParser.parseString(Files.readString(Path.of(path)))
        val decoded = codec.parse(JsonOps.INSTANCE, json)
        assertTrue(decoded.result().isPresent)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrapMinecraft() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }
}
