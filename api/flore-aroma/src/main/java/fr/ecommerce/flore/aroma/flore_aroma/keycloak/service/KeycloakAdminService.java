package fr.ecommerce.flore.aroma.flore_aroma.keycloak.service;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin-username}")
    private String adminUsername;

    @Value("${keycloak.admin-password}")
    private String adminPassword;

    @Value("${keycloak.client-id:admin-cli}")
    private String clientId;

    @Value("${keycloak.front-client-id:florearoma-front}")
    private String frontClientId;

    private final RestTemplate restTemplate = new RestTemplate();

    private String getAdminAccessToken() {
        String url = serverUrl + "/realms/master/protocol/openid-connect/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String,String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        ResponseEntity<Map> resp = restTemplate.postForEntity(url, new HttpEntity<>(form, headers), Map.class);
        return (String) resp.getBody().get("access_token");
    }

    public String createUserWithEmailRequired(String email, String firstName, String lastName) {
        String token = getAdminAccessToken();

        String url = serverUrl + "/admin/realms/" + realm + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // username = email pour ce flow “email-first”
        Map<String, Object> payload = Map.of(
                "username", email,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "emailVerified", false,
                "requiredActions", List.of("VERIFY_EMAIL")
        );

        ResponseEntity<Void> resp = restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), Void.class);

        // Keycloak renvoie l’URL Location du nouvel utilisateur
        String location = resp.getHeaders().getFirst("Location");
        // /admin/realms/{realm}/users/{id}
        String userId = location.substring(location.lastIndexOf('/') + 1);

        return userId;
    }

    public void sendVerifyEmail(String userId) {
        String token = getAdminAccessToken();

        // PUT /execute-actions-email
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/execute-actions-email?client_id=" + frontClientId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<String> actions = List.of("VERIFY_EMAIL");
        restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(actions, headers), Void.class);
    }

    public void setUserPassword(String userId, String rawPassword, boolean temporary) {
        String token = getAdminAccessToken();

        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> cred = Map.of(
                "type", "password",
                "value", rawPassword,
                "temporary", temporary
        );

        restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(cred, headers), Void.class);
    }

    // KeycloakAdminService.java

    public void addRealmRoleToUser(String userId, String roleName) {
        String token = getAdminAccessToken();

        // GET role
        String roleUrl = serverUrl + "/admin/realms/" + realm + "/roles/" + roleName;
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);

        ResponseEntity<Map> roleResp =
                restTemplate.exchange(roleUrl, HttpMethod.GET, new HttpEntity<>(h), Map.class);

        String roleId = (String) roleResp.getBody().get("id");

        // POST mapping
        String mapUrl = serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";
        Map<String, Object> roleRep = Map.of("id", roleId, "name", roleName);

        h.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity(mapUrl, new HttpEntity<>(List.of(roleRep), h), Void.class);
    }

    public void deleteUser(String userId) {
        String token = getAdminAccessToken();

        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        // (optionnel) invalider les sessions actives avant suppression
        try {
            String logoutUrl = url + "/logout";
            restTemplate.postForEntity(logoutUrl, new HttpEntity<>(headers), Void.class);
        } catch (Exception ignore) {
            // si pas de session, on ignore
        }

        // suppression du user
        restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }

    public void markEmailVerified(String userId, boolean verified) {
        String token = getAdminAccessToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("emailVerified", verified);
        restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, h), Void.class);
    }

    public void enableUser(String userId, boolean enabled) {
        String token = getAdminAccessToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("enabled", enabled);
        restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, h), Void.class);
    }

    public String createUserWithoutRequiredActions(String email, String firstName, String lastName) {
        String token = getAdminAccessToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = Map.of(
                "username", email,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "emailVerified", false // on confirmera plus tard après le code WhatsApp
                // PAS de requiredActions ici
        );

        ResponseEntity<Void> resp = restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), Void.class);
        String location = resp.getHeaders().getFirst("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    public void clearRequiredActions(String userId) {
        String token = getAdminAccessToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("requiredActions", List.of());
        restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, h), Void.class);
    }





}

