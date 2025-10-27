package fr.ecommerce.flore.aroma.flore_aroma.keycloak.service;

import fr.ecommerce.flore.aroma.flore_aroma.account.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KeycloakTokenService {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.front-client-id}")
    private String clientId;

    private final RestTemplate restTemplate = new RestTemplate();

    private String tokenEndpoint() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String authenticateUser(LoginRequest loginRequest) {
        RestTemplate restTemplate = new RestTemplate();
        String url = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", "florearoma-app"); // Remplace par ton Client ID
        body.add("client_secret", "ZwIH2oKdLTH72B7mWjqYcVqtSVG5alte"); // Remplace par ton Client Secret
        body.add("grant_type", "password");
        body.add("username", loginRequest.getUsername());
        body.add("password", loginRequest.getPassword());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        } else {
            throw new RuntimeException("Échec de l'authentification !");
        }
    }

    public Map<String, Object> refresh(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String,String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("refresh_token", refreshToken);

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(tokenEndpoint(), new HttpEntity<>(form, headers), Map.class);
            return resp.getBody();
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Refresh token invalid or expired: " + e.getStatusCode());
        }
    }
}

