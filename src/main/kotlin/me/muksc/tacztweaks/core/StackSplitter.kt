package me.muksc.tacztweaks.core

/** Pure item-count splitting used by the unload packet handler and its unit tests. */
object StackSplitter {
    const val MAX_UNLOAD_AMMO: Int = 32_768
    const val MAX_STACK_SIZE: Int = 1_024

    @JvmStatic
    fun split(count: Int, stackSize: Int): List<Int>? {
        if (count !in 0..MAX_UNLOAD_AMMO || stackSize !in 1..MAX_STACK_SIZE) return null
        if (count == 0) return emptyList()

        val result = ArrayList<Int>((count - 1) / stackSize + 1)
        var remaining = count
        while (remaining > 0) {
            val next = minOf(remaining, stackSize)
            result.add(next)
            remaining -= next
        }
        return result
    }
}
