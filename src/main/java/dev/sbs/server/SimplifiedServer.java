package dev.sbs.server;

import com.google.gson.Gson;
import dev.simplified.serverapi.config.ServerConfig;
import dev.simplified.serverapi.config.ServerWebConfig;
import dev.simplified.serverapi.security.ApiKeySecurityConfig;
import dev.simplified.serverapi.security.ApiKeyStore;
import dev.simplified.serverapi.security.PermitAllSecurityConfig;
import dev.simplified.util.SystemUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot application entry point for the Simplified Server.
 *
 * <p>Component scanning covers {@code dev.sbs.server} for this server's controllers and
 * configuration, and {@code dev.simplified.serverapi} for the spring-framework configuration -
 * API key security, error handling, API versioning, and the {@link ServerWebConfig} message
 * converters, which serialize with the {@link Gson} bean this class supplies from
 * {@link ServerApi#getGson()}.
 *
 * <p>{@link #main(String[])} registers the {@code HYPIXEL_API_KEY} environment variable, when
 * set, with {@link ServerApi#getKeyManager()}, passes {@code INET6_NETWORK_PREFIX}, when set,
 * to {@link ServerApi#setInet6NetworkPrefix(String)}, and starts the application with the
 * {@link ServerConfig#optimized()} preset as its default properties.
 *
 * <p>That preset sets {@code api.key.authentication.enabled=true}, so
 * {@link ApiKeySecurityConfig} loads and requires an {@link ApiKeyStore} bean - startup fails
 * without one, and no committed source in this repository declares one. A deployment supplies
 * an {@code ApiKeyStore} bean, or sets {@code api.key.authentication.enabled=false} (a
 * command-line argument or the {@code API_KEY_AUTHENTICATION_ENABLED} environment variable
 * outranks the default properties), which loads {@link PermitAllSecurityConfig} instead and
 * leaves every endpoint open.
 */
@SpringBootApplication(scanBasePackages = { "dev.sbs.server", "dev.simplified.serverapi" })
public class SimplifiedServer {

    @Bean
    public @NotNull Gson gson() {
        return ServerApi.getGson();
    }

    public static void main(String[] args) {
        ServerApi.getKeyManager().add("HYPIXEL_API_KEY", SystemUtil.getEnv("HYPIXEL_API_KEY"));
        SystemUtil.getEnv("INET6_NETWORK_PREFIX").ifPresent(ServerApi::setInet6NetworkPrefix);
        SpringApplication application = new SpringApplication(SimplifiedServer.class);
        application.setDefaultProperties(
            ServerConfig.optimized()
                .build()
                .toProperties()
        );
        application.run(args);
    }

}
