package me.muksc.tacztweaks.network;

/**
 * Dedicated-safe reflection bridge for client payload work.
 *
 * <p>The class name is a string so neither this bridge nor common payload handlers place
 * {@code net.minecraft.client.*} types in their constant pools. This mirrors TaCZ: Renovated
 * 26.2's {@code com.tacz.guns.network.ClientPacketBridge}.</p>
 */
public final class ClientPacketBridge {
    private static final String HANDLERS =
            "me.muksc.tacztweaks.client.network.ClientPacketHandlers";

    private ClientPacketBridge() {
    }

    public static void invoke(String method, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Class<?> handlers = Class.forName(HANDLERS);
            handlers.getMethod(method, parameterTypes).invoke(null, arguments);
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            // Expected on a dedicated server: client payload handlers are never invoked there.
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to invoke client payload handler " + method, error);
        }
    }
}
