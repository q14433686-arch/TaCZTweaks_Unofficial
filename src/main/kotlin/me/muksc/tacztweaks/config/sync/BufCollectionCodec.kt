package me.muksc.tacztweaks.config.sync

import net.minecraft.network.FriendlyByteBuf

/**
 * Replacements for `FriendlyByteBuf#readCollection` / `#writeCollection`.
 *
 * ## Why this exists
 *
 * Minecraft 26.3 removed the `readCollection` / `writeCollection` convenience methods from
 * [FriendlyByteBuf] (the `readMap` / `writeMap` pair went in the same batch). Vanilla's
 * replacement is to compose a `StreamCodec`, but this config system synchronises entries
 * through hand-written `(buf, value) -> Unit` encoders, so converting it to `StreamCodec`
 * would be a refactor well beyond a version port.
 *
 * The sibling TaCZ port solved the map half the same way, see
 * `cn.sh1rocu.tacz.util.BufMapCodec`.
 *
 * ## Wire format
 *
 * **Byte-for-byte identical to the 26.2 methods it replaces**: a varint element count
 * followed by that many elements. Nothing about the sync protocol changes, so a 26.2-era
 * capture and a 26.3 capture of the same config decode identically.
 */
object BufCollectionCodec {
    /** Equivalent to 26.2's `buf.writeCollection(values, writer)`. */
    fun <T> writeList(buf: FriendlyByteBuf, values: Collection<T>, writer: (FriendlyByteBuf, T) -> Unit) {
        buf.writeVarInt(values.size)
        for (value in values) writer(buf, value)
    }

    /** Equivalent to 26.2's `buf.readCollection(ArrayList::new, reader)`. */
    fun <T> readList(buf: FriendlyByteBuf, reader: (FriendlyByteBuf) -> T): List<T> {
        val size = buf.readVarInt()
        // The count is attacker-controlled on a malicious server, so do not pre-size the
        // list to it; grow on demand instead. Vanilla's own removed helper capped the
        // allocation for the same reason.
        val values = ArrayList<T>(minOf(size, 64).coerceAtLeast(0))
        repeat(size) { values.add(reader(buf)) }
        return values
    }
}
