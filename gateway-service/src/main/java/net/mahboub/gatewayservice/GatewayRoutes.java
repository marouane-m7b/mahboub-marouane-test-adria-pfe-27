package net.mahboub.gatewayservice;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class GatewayRoutes {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("accounts", route -> route.path("/accounts", "/accounts/**")
                        .uri("lb://account-service"))
                .route("transactions", route -> route.path("/transactions", "/transactions/**")
                        .uri("lb://transaction-service"))
                .route("account-connectors", route -> route.path("/account/**")
                        .filters(filter -> filter.stripPrefix(1)).uri("lb://account-service"))
                .route("transaction-connectors", route -> route.path("/transaction/**")
                        .filters(filter -> filter.stripPrefix(1)).uri("lb://transaction-service"))
                .route("chat", route -> route.path("/chat", "/chat/**").uri("lb://ebank-bot"))
                .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization", "Accept"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsWebFilter(source);
    }
}
