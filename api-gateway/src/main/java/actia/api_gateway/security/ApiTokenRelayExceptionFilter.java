package actia.api_gateway.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Quand tokenRelay() ne parvient pas à fournir un access token valide (token expiré
 * et refresh impossible/absent), Spring Security lève une
 * ClientAuthorizationRequiredException qui est normalement interceptée par
 * OAuth2AuthorizationRequestRedirectWebFilter et transformée en redirection (302)
 * vers Keycloak. Pour une requête AJAX/fetch vers /api/**, ce 302 est inutile
 * (le navigateur ne peut pas le suivre : la réponse de Keycloak n'a pas les
 * en-têtes CORS attendus) et finit en erreur CORS trompeuse côté client.
 *
 * Ce filtre, exécuté plus "à l'intérieur" que la chaîne de sécurité (donc
 * intercepte l'exception avant OAuth2AuthorizationRequestRedirectWebFilter),
 * répond directement 401 sur /api/** pour laisser le frontend gérer la
 * reconnexion proprement (cf. auth.interceptor.ts).
 */
@Component
public class ApiTokenRelayExceptionFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(ClientAuthorizationRequiredException.class, ex -> {
                    String path = exchange.getRequest().getURI().getPath();
                    if (path.startsWith("/api/")) {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                    return Mono.error(ex);
                });
    }
}
