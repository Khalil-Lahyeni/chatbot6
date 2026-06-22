package actia.api_gateway.controller;

import actia.api_gateway.dto.ChangePasswordRequest;
import actia.api_gateway.service.AuthService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            OAuth2AuthenticationToken authentication,
            @RequestBody ChangePasswordRequest request
    ) {

        String username =
                authentication
                        .getPrincipal()
                        .getAttribute("preferred_username");

        authService.changePassword(
                username,
                request
        );


        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Password changed successfully"
                )
        );

    }
}