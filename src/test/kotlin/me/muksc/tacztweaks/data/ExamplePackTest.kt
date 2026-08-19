package me.muksc.tacztweaks.data

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Verifies that the example pack names a block tag which exists in the 26.1.2 merged jar. */
class ExamplePackTest {
    @Test
    fun `metal tag uses the reloadable 26_1_2 chains tag`() {
        val fixture = checkNotNull(javaClass.getResource("/fixtures/metal_tag.json"))
            .readText()
        val values = JsonParser.parseString(fixture).asJsonObject
            .getAsJsonArray("values")
            .map { it.asString }

        assertTrue("#minecraft:chains" in values)
        assertNotNull(javaClass.getResource("/data/minecraft/tags/block/chains.json"))
    }
}
