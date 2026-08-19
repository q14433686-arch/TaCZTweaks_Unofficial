package me.muksc.tacztweaks.core

/** Deterministic burst/pellet counters shared by the projectile hook and unit tests. */
class ProjectileIndexAllocator {
    var burstIndex: Int = 0
        private set
    private var pelletIndex: Int = 0

    fun resetShot() {
        burstIndex = 0
        pelletIndex = 0
    }

    fun beginCycle() {
        pelletIndex = 0
    }

    fun takePelletIndex(): Int = pelletIndex++

    fun completeCycle(success: Boolean) {
        if (success) burstIndex++
    }
}
