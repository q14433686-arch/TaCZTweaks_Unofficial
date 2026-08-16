package me.muksc.tacztweaks.feature.gameplay.behaviour

object BulletProtectionContext {
    private val evaluatingBulletProtection = ThreadLocal.withInitial { false }

    @JvmStatic
    fun isEvaluatingBulletProtection(): Boolean = evaluatingBulletProtection.get()

    @JvmStatic
    fun <T> evaluate(block: () -> T): T {
        val previous = evaluatingBulletProtection.get()
        evaluatingBulletProtection.set(true)
        return try {
            block()
        } finally {
            evaluatingBulletProtection.set(previous)
        }
    }
}
