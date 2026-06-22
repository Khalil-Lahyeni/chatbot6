# Gestion de l'authentification

## Vue d'ensemble

L'authentification repose sur **Keycloak** (Identity Provider), **Spring Cloud Gateway** (point d'entrée unique) et **Angular** (frontend). Le flux global suit le protocole **OAuth2 Authorization Code + OIDC**, avec session persistée côté serveur dans **Redis**.

```
Angular (4200) ──► API Gateway (8888) ──► Keycloak (8080)
                         │
                       Redis (session)
                         │
              ──► Monitoring Service (8001)
              ──► Collector Service (8881)
```

---

## 1. Keycloak — Identity Provider

- **Realm** : `fleet-management`
- **Client ID** : `actia-app`
- **Grant type** : `authorization_code` (login utilisateur) + `client_credentials` (admin API)
- **Scopes** : `openid`, `profile`, `email`
- **PKCE** : activé (`code_challenge_method=S256` visible dans les redirections)

La gateway dispose de deux connexions Keycloak :
- **OAuth2 Login** : pour authentifier les utilisateurs via le navigateur (`authorization_code`)
- **Admin client** (`KeycloakConfig`) : connexion `client_credentials` pour les opérations admin (ex : changement de mot de passe via l'API Admin Keycloak)

---

## 2. API Gateway — Spring Cloud Gateway

### Sécurité (`SecurityConfig.java`)

Toutes les requêtes exigent une session authentifiée :

```java
.anyExchange().authenticated()
```

Endpoints publics (exemptés) :
| Path | Raison |
|---|---|
| `OPTIONS` (toutes routes) | Pré-vol CORS |
| `/actuator/health`, `/actuator/info` | Health checks |
| `/login/oauth2/code/**`, `/oauth2/**` | Callback OAuth2 |
| `/api/trains/collector/ping` | Ping technique collector |
| `/logout` | Déclenchement du logout |

En cas de requête non authentifiée : réponse **`401 Unauthorized`** (pas de redirection — la redirection vers Keycloak est à l'initiative du frontend).

### Session Redis (`RedisSessionConfig.java`)

La session est stockée dans **Redis** avec une durée de vie de **1 heure** :

```java
@EnableRedisWebSession(maxInactiveIntervalInSeconds = 3600)
```

Le cookie `SESSION` (httpOnly) est renvoyé au navigateur après login. Toutes les requêtes ultérieures l'utilisent automatiquement (`withCredentials: true` côté Angular).

### Routage avec `tokenRelay()` (`RouteConfig.java`)

Chaque route backend applique `.tokenRelay()` : le filtre extrait l'access token OAuth2 de la session et l'injecte en en-tête `Authorization: Bearer <token>` dans la requête relayée.

```java
.route("Monitoring", route -> route
    .path("/api/monitoring/**")
    .filters(f -> f.stripPrefix(2).tokenRelay())
    .uri("http://localhost:8001/actia"))

.route("Collector", route -> route
    .path("/api/collector/**")
    .filters(f -> f.stripPrefix(2).tokenRelay())
    .uri("http://localhost:8881/actia/collector"))
```

`stripPrefix(2)` supprime les deux premiers segments du path (ex : `/api/monitoring/trains` → `/trains`).

### Endpoints gateway exposés

| Méthode | Path | Rôle |
|---|---|---|
| `GET` | `/api/user` | Retourne les infos de l'utilisateur connecté (username, email, roles) |
| `POST` | `/api/auth/change-password` | Changement de mot de passe via Keycloak Admin API |
| `GET/POST/PUT/DELETE` | `/api/monitoring/**` | Proxy vers le service Monitoring |
| `GET/POST/PUT/DELETE` | `/api/collector/**` | Proxy vers le service Collector |

### Logout (`SecurityConfig.java`)

Le logout est OIDC-aware : la gateway invalide la session Keycloak (backchannel logout), supprime le `SecurityContext`, invalide la session Redis, et efface les cookies `SESSION`, `grafana_session`, `grafana_session_expiry`.

---

## 3. Frontend Angular

### Initialisation (`AuthService`)

Au démarrage de l'app, `checkSession()` interroge `GET /api/user` :
- **Succès** : l'utilisateur est considéré authentifié, ses infos sont stockées dans un `signal` Angular (`currentUser`).
- **Échec (401/403)** : `currentUser` reste `null`, l'`authGuard` déclenche la redirection vers Keycloak.

```typescript
checkSession(): Observable<void> {
  return this.http.get<UserInfo>(`${GATEWAY_URL}/api/user`).pipe(
    tap((user) => this.currentUser.set(user)),
    catchError(() => { this.currentUser.set(null); return of(null); })
  );
}
```

### Login (`AuthService.login`)

La redirection vers Keycloak passe par la gateway :

```typescript
login(returnUrl: string = '/'): void {
  const redirectUri = encodeURIComponent(`${FRONTEND_URL}${returnUrl}`);
  window.location.href =
    `${GATEWAY_URL}/oauth2/authorization/keycloak?redirect_uri=${redirectUri}`;
}
```

L'URL de retour (`returnUrl`) est préservée pour rediriger l'utilisateur vers la page qu'il voulait atteindre avant d'être renvoyé au login.

### Intercepteur HTTP (`authInterceptor`)

Injecté globalement dans `app.config.ts`, il :
1. Ajoute `withCredentials: true` à chaque requête (transmission du cookie `SESSION`)
2. Sur `401`/`403`, déclenche `authService.login()` — sauf sur `/api/user` pour éviter une boucle infinie lors du `checkSession()`

```typescript
const reqWithCredentials = req.clone({ withCredentials: true });
return next(reqWithCredentials).pipe(
  catchError((err) => {
    if (err.status === 401 || err.status === 403) {
      if (!req.url.includes('/api/user')) authService.login(router.url);
      return EMPTY;
    }
    return throwError(() => err);
  })
);
```

### Guard de route (`authGuard`)

Protège toutes les routes de l'app. Attend que le `checkSession()` initial soit terminé (`isLoading = false`) avant de décider :

```typescript
return toObservable(authService.isLoading).pipe(
  filter((loading) => !loading),
  take(1),
  map(() => {
    if (authService.isAuthenticated()) return true;
    authService.login(state.url);
    return false;
  })
);
```

Un flag module-level `redirectingToLogin` empêche les redirections multiples si plusieurs guards s'activent simultanément.

### Logout multi-onglets (`BroadcastChannel`)

Le logout utilise l'API `BroadcastChannel` pour synchroniser tous les onglets ouverts :

```typescript
logout(): void {
  this.channel.postMessage('LOGOUT'); // notifie les autres onglets
  window.location.href = `${GATEWAY_URL}/logout`;
}
```

À la réception du message `LOGOUT`, chaque onglet vide son état local et redirige vers le logout gateway.

---

## 4. Flux complet — premier accès

```
1. Utilisateur accède à http://localhost:4200
2. App Angular démarre → checkSession() → GET /api/user
3. Gateway : pas de session → 401
4. authGuard : non authentifié → authService.login('/dashboard')
5. Redirection → http://localhost:8888/oauth2/authorization/keycloak
6. Gateway démarre le flow Authorization Code → redirect vers Keycloak (8080)
7. Utilisateur saisit ses credentials sur la page Keycloak
8. Keycloak redirige → http://localhost:8888/login/oauth2/code/keycloak?code=...
9. Gateway échange le code contre des tokens (access + refresh + id token)
10. Gateway crée une session Redis, pose le cookie SESSION
11. Redirect vers http://localhost:4200/dashboard
12. checkSession() → GET /api/user → 200 + { username, email, roles }
13. currentUser signal mis à jour → app affichée
```

---

## 5. Configuration — profils

| Paramètre | Local | Docker |
|---|---|---|
| Keycloak URL | `http://localhost:8080` | `http://keycloak:8080` |
| Redis host | `localhost` | `${REDIS_HOST}` |
| Session timeout | 3600s | 3600s |
| Frontend URL | `http://localhost:4200` | `${ANGULAR_APP_URL}` |
