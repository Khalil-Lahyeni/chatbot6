package actia.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // ── Collector Service - Trains ──
                .route("Collector", route -> route
                        .path("/api/collector/**")
                                        .filters(filter -> filter
                                                        .stripPrefix(2)
                                                        .prefixPath("/actia/collector")
                                                        .tokenRelay())
                                        .uri("http://localhost:8881"))

                        .route("Monitoring", route -> route
                                        .path("/api/monitoring/**")
                                        .filters(filter -> filter
                                                        .stripPrefix(2)
                                                        .prefixPath("/actia")
                                                        .tokenRelay())
                                        .uri("http://localhost:8001"))

                        .build();
}
}