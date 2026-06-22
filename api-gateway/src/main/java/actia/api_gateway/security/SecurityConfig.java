package actia.api_gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;

import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.*;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;

import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.WebSessionServerLogoutHandler;
import org.springframework.http.ResponseCookie;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${app.logout-url:http://localhost:4200}")
    private String logoutUrl;

    private final ReactiveClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(ReactiveClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http, ReactiveClientRegistrationRepository clientRegistrationRepository) {
        http

                .cors(cors -> {})
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(apiAwareAuthenticationEntryPoint())
                )
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers("/login/oauth2/code/**").permitAll()
                        .pathMatchers("/oauth2/**").permitAll()
                        .pathMatchers("/api/trains/collector/ping").permitAll()
                        .pathMatchers("/logout").permitAll()
                        .anyExchange().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2
                        .authenticationSuccessHandler(
                                new RedirectServerAuthenticationSuccessHandler(logoutUrl)  // ← RedirectServer...
                        )
                        .authenticationFailureHandler(
                                new RedirectServerAuthenticationFailureHandler(logoutUrl)  // ← RedirectServer...
                        )
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
                        .logoutHandler(deleteCookiesLogoutHandler())
                        .requiresLogout(new PathPatternParserServerWebExchangeMatcher("/logout"))  // ← ServerWebExchangeMatcher
                );

        return http.build();
    }

    /**
     * Évite qu'un appel AJAX/fetch vers /api/** se retrouve redirigé (302) vers Keycloak
     * (ce qui casse en CORS côté navigateur, cf. ClientAuthorizationRequiredException
     * levée par tokenRelay() quand le refresh token est lui aussi expiré).
     * Pour /api/**, on répond toujours 401 ; pour le reste (navigation classique), on
     * laisse Spring Security gérer la redirection OAuth2 normale.
     */
    private ServerAuthenticationEntryPoint apiAwareAuthenticationEntryPoint() {
        ServerAuthenticationEntryPoint unauthorizedEntryPoint =
                new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED);
        ServerAuthenticationEntryPoint oauth2RedirectEntryPoint =
                new RedirectServerAuthenticationEntryPoint("/oauth2/authorization/keycloak");

        return (exchange, ex) -> {
            String path = exchange.getRequest().getURI().getPath();
            if (path.startsWith("/api/")) {
                return unauthorizedEntryPoint.commence(exchange, ex);
            }
            return oauth2RedirectEntryPoint.commence(exchange, ex);
        };
    }

    /**
     * Rafraîchit automatiquement l'access token via le refresh token avant qu'il
     * n'expire, plutôt que de forcer une nouvelle authentification OAuth2 complète
     * (utilisé par TokenRelay sur les routes Collector/Monitoring, cf. RouteConfig).
     */
    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {

        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
                ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .build();

        DefaultReactiveOAuth2AuthorizedClientManager authorizedClientManager =
                new DefaultReactiveOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientRepository);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }

    @Bean
    public OidcClientInitiatedServerLogoutSuccessHandler oidcLogoutSuccessHandler(
                                                                                    ReactiveClientRegistrationRepository clientRegistrationRepository
    ) {
        OidcClientInitiatedServerLogoutSuccessHandler handler =
                new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri(logoutUrl);
        return handler;
    }

    @Bean
    public ServerLogoutHandler deleteCookiesLogoutHandler() {
        return new DelegatingServerLogoutHandler(// invalide la session (= invalidateHttpSession)
                new SecurityContextServerLogoutHandler(),  // clear le SecurityContext
                cookiesClearingLogoutHandler("SESSION", "grafana_session", "grafana_session_expiry"),
                new WebSessionServerLogoutHandler()
                );
    }

    private ServerLogoutHandler cookiesClearingLogoutHandler(String... cookieNames) {
        return (exchange, authentication) -> {
            ServerHttpResponse response = exchange.getExchange().getResponse();

            for (String cookieName : cookieNames) {
                ResponseCookie expiredCookie = ResponseCookie.from(cookieName, "")
                        .maxAge(Duration.ZERO)   // expire immédiatement
                        .path("/")
                        .httpOnly(false)
                        .build();
                response.addCookie(expiredCookie);
            }

            return Mono.empty();
        };
    }

}
