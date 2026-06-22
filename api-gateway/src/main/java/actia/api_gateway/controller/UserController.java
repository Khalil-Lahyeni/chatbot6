package actia.api_gateway.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
public class UserController {

    private static final Logger logger =
            LoggerFactory.getLogger(UserController.class);

    @GetMapping("/api/user")
    public UserInfoResponse currentUser(
            @AuthenticationPrincipal OidcUser oidcUser) {

        if (oidcUser == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "No active session");
        }

        String username = oidcUser.getPreferredUsername();
        if (username == null || username.isBlank()) {
            username = oidcUser.getName();
        }

        @SuppressWarnings("unchecked")
        List<String> roles = List.of();

        Object realmAccess = oidcUser.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> realmAccessMap) {
            Object rolesObj = realmAccessMap.get("roles");
            if (rolesObj instanceof List<?> roleList) {
                roles = roleList.stream()
                        .map(Object::toString)
                        .filter(r ->
                                !r.equals("offline_access")
                                        && !r.equals("uma_authorization")
                                        && !r.equals("default-roles-fleet-management"))
                        .sorted()
                        .toList();
            }
        }

        return new UserInfoResponse(username, oidcUser.getEmail(), roles);
    }

    public record UserInfoResponse(
            String username,
            String email,
            List<String> roles) {
    }
}
