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
import dev.simplified.util.SystemUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Server-local service locator.
 * <p>
 * Owns the {@link Gson} and {@link GsonSettings} used by the server for contract I/O, a
 * {@link KeyManager} that supplies the Hypixel API key header on demand, and the {@link Client}
 * / {@link Proxy} instances for the Hypixel, SBS, and Mojang contracts. The Mojang {@link Proxy}
 * rotates IPv6 source addresses across the prefix {@code INET6_NETWORK_PREFIX} names, and sends
 * from the host's default address when the variable is unset or blank. Persistence access flows
 * through {@code api.simplified.skyblock.SkyBlockData} directly - this locator does not own it.
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
     * The rate-limit-relevant subnet size for the Mojang rotation. Mojang buckets its per-IP
     * limits by {@code /56} subnet for IPv6, so budgets are tracked per {@code /56}: addresses
     * inside one {@code /56} share one budget, and rotation relieves the limit only by moving
     * between {@code /56} subnets.
     */
    private static final int MOJANG_BUCKET_PREFIX_LENGTH = 56;

    /**
     * The shared Mojang proxy, rotating outbound source addresses across the IPv6 prefix the
     * {@code INET6_NETWORK_PREFIX} environment variable names, and sending from the host's default
     * source address when the variable is unset or blank.
     */
    @Getter private static final @NotNull Proxy<MojangContract> mojangProxy = Proxy.builder(
            ClientConfig.builder(MojangContract.class, gsonSettings)
                .withErrorDecoder(MojangApiException::new)
                .build()
        )
        .withSubnetRotation(
            SystemUtil.getEnv("INET6_NETWORK_PREFIX")
                .filter(cidr -> !cidr.isBlank())
                .map(cidr -> SubnetRotation.builder()
                    .sourcePrefix(cidr)
                    .bucketPrefixLength(MOJANG_BUCKET_PREFIX_LENGTH)
                    .build()
                )
        )
        // The proxy serves the services lookups and the session server's profile reads,
        // whose limits are separate buckets; a client is available while neither is spent.
        .withAvailability(client -> !client.isRateLimited(MojangDomain.MINECRAFT_SERVICES)
            && !client.isRateLimited(MojangDomain.MOJANG_SESSIONSERVER))
        .build();

}
