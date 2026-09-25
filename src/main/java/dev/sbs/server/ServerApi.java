package dev.sbs.server;

import api.simplified.hypixel.HypixelContract;
import api.simplified.hypixel.exception.HypixelApiException;
import api.simplified.mojang.MojangContract;
import api.simplified.mojang.exception.MojangApiException;
import api.simplified.mojang.request.MojangDomain;
import com.google.gson.Gson;
import dev.sbs.api.SimplifiedContract;
import dev.sbs.api.exception.SimplifiedApiException;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.client.Client;
import dev.simplified.client.ClientConfig;
import dev.simplified.client.Proxy;
import dev.simplified.client.subnet.SubnetRotation;
import dev.simplified.gson.GsonSettings;
import dev.simplified.manager.KeyManager;
import dev.simplified.manager.Manager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Server-local service locator.
 * <p>
 * Owns the {@link Gson} and {@link GsonSettings} used by the server for contract I/O, a
 * {@link KeyManager} that supplies the Hypixel API key header on demand, and the {@link Client}
 * / {@link Proxy} instances for the Hypixel, SBS, and Mojang contracts. The Mojang
 * {@link Proxy} can be rebuilt with IPv6 source-address rotation via
 * {@link #setInet6NetworkPrefix(String)} once the runtime prefix is known. Persistence access
 * flows through {@code api.simplified.skyblock.SkyBlockData} directly - this locator does not
 * own it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServerApi {

    @Getter private static final @NotNull GsonSettings gsonSettings = GsonSettings.defaults();
    @Getter private static final @NotNull Gson gson = gsonSettings.create();

    @Getter private static final @NotNull KeyManager keyManager = new KeyManager(
        (entry, key) -> key.equalsIgnoreCase(entry.getKey()),
        Manager.Mode.UPDATE
    );

    @Getter private static final @NotNull Client<HypixelContract> hypixelClient = Client.create(
        ClientConfig.builder(HypixelContract.class, gsonSettings)
            .withErrorDecoder(HypixelApiException::new)
            .withDynamicHeader("API-Key", keyManager.getSupplier("HYPIXEL_API_KEY"))
            .build()
    );

    @Getter private static final @NotNull Client<SimplifiedContract> sbsClient = Client.create(
        ClientConfig.builder(SimplifiedContract.class, gsonSettings)
            .withErrorDecoder(SimplifiedApiException::new)
            .build()
    );

    /**
     * The rate-limit-relevant subnet size for the Mojang rotation. A {@code /64} is the
     * smallest block an IPv6 host is normally delegated, and the granularity an edge treats
     * as one client, so budgets are tracked per {@code /64} rather than per address.
     */
    private static final int MOJANG_BUCKET_PREFIX_LENGTH = 64;

    private static volatile @Nullable Proxy<MojangContract> mojangProxy = null;

    /**
     * Returns the shared Mojang proxy, which rotates outbound source addresses across the
     * prefix supplied to {@link #setInet6NetworkPrefix(String)}.
     *
     * @return the shared Mojang proxy instance
     * @throws IllegalStateException if no IPv6 prefix has been registered
     */
    public static @NotNull Proxy<MojangContract> getMojangProxy() {
        Proxy<MojangContract> proxy = mojangProxy;

        if (proxy == null)
            throw new IllegalStateException("setInet6NetworkPrefix must be called before the Mojang proxy is used");

        return proxy;
    }

    /**
     * Replaces the registered Mojang {@link Proxy} with one that rotates outbound source
     * addresses across the given IPv6 CIDR prefix.
     * <p>
     * Intended to be called once at application startup once the runtime IPv6 prefix is known.
     *
     * @param cidrPrefix an IPv6 network prefix in CIDR notation (e.g. {@code "2000:444:33ff::/48"})
     */
    public static void setInet6NetworkPrefix(@NotNull String cidrPrefix) {
        mojangProxy = Proxy.builder(
                ClientConfig.builder(MojangContract.class, gsonSettings)
                    .withErrorDecoder(MojangApiException::new)
                    .build()
            )
            .withSubnetRotation(
                SubnetRotation.builder()
                    .sourcePrefix(cidrPrefix)
                    .bucketPrefixLength(MOJANG_BUCKET_PREFIX_LENGTH)
                    .build()
            )
            .withAvailability(client -> !client.isRateLimited(MojangDomain.MINECRAFT_SERVICES))
            .build();
    }

}
