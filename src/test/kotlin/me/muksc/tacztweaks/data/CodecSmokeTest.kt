package me.muksc.tacztweaks.data

import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import me.muksc.tacztweaks.data.core.ValueRange
import me.muksc.tacztweaks.data.manager.BULLET_INTERACTION_CODEC
import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodecSmokeTest {
    @BeforeAll
    fun bootstrapMinecraft() {
        SharedConstants.tryDetectVersion()
        Bootstrap.bootStrap()
    }

    @Test
    fun `restored predicate tier burst and pellet fixture decodes`() {
        assertFixtureDecodes("/fixtures/schema_smoke.json", BulletInteraction.CODEC)
    }

    @Test
    fun `airspace fixture decodes`() {
        assertFixtureDecodes("/fixtures/airspace.json", BulletSounds.CODEC)
    }

    @Test
    fun `legacy v2 bullet interaction decodes and converts`() {
        val decoded = BULLET_INTERACTION_CODEC.parse(
            JsonOps.INSTANCE,
            JsonParser.parseString(readFixture("/fixtures/bullet_interaction_v2.json"))
        )
        assertTrue(decoded.result().isPresent)
        assertTrue(decoded.result().orElseThrow() is BulletInteraction.Block)
    }

    @Test
    fun `value range includes zero and rejects reversed bounds`() {
        assertTrue(0.0 in ValueRange.DEFAULT)
        val reversed = JsonParser.parseString("""{"min":2.0,"max":1.0}""")
        assertFalse(ValueRange.CODEC.parse(JsonOps.INSTANCE, reversed).result().isPresent)
    }

    private fun <T> assertFixtureDecodes(resourcePath: String, codec: Codec<T>) {
        val decoded = codec.parse(JsonOps.INSTANCE, JsonParser.parseString(readFixture(resourcePath)))
        assertTrue(decoded.result().isPresent)
    }

    private fun readFixture(resourcePath: String): String =
        checkNotNull(javaClass.getResource(resourcePath)) { "Missing test fixture $resourcePath" }.readText()
}
