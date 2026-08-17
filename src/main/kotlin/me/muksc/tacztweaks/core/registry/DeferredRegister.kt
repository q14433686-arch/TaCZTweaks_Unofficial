package me.muksc.tacztweaks.core.registry

import me.muksc.tacztweaks.core.Identifier
import net.minecraft.core.Registry
import java.util.function.Supplier

class DeferredRegister<R : Any>(
    private val registry: Registry<R>,
    private val namespace: String
) {
    companion object {
        fun <R : Any> create(registry: Registry<R>, namespace: String): DeferredRegister<R> = DeferredRegister(registry, namespace)
    }

    fun <T : R> register(path: String, supplier: Supplier<T>): DeferredHolder<R, T> {
        val holder = Registry.registerForHolder(registry, Identifier.fromNamespaceAndPath(namespace, path), supplier.get())
        return DeferredHolder(holder)
    }
}
