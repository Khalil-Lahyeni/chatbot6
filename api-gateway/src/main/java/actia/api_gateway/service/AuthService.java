package actia.api_gateway.service;

import actia.api_gateway.dto.ChangePasswordRequest;
import lombok.RequiredArgsConstructor;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
    private String issuer;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.server-url}")
    private String serverUrl;

    public void changePassword(
            String username,
            ChangePasswordRequest request
    ) {

        // 1. Vérifier ancien password
        boolean valid = verifyOldPassword(
                username,
                request.getOldPassword()
        );

        if (!valid) {
            throw new RuntimeException("Old password incorrect");
        }

        // 2. Trouver user
        UsersResource usersResource =
                keycloak.realm(realm).users();

        List<UserRepresentation> users =
                usersResource.searchByUsername(username, true);

        if (users.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        UserRepresentation user = users.get(0);

        // 3. Nouveau password
        CredentialRepresentation credential =
                new CredentialRepresentation();

        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getNewPassword());

        usersResource.get(user.getId())
                .resetPassword(credential);
    }

    private boolean verifyOldPassword(
            String username,
            String oldPassword
    ) {

        try {

            Keycloak testClient = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .clientId(clientId)
                    .grantType(OAuth2Constants.PASSWORD)
                    .clientSecret(clientSecret)
                    .username(username)
                    .password(oldPassword)
                    .build();

            testClient.tokenManager().getAccessToken();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
