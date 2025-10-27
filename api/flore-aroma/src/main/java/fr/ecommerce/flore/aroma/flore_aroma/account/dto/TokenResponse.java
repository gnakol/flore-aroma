package fr.ecommerce.flore.aroma.flore_aroma.account.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;

@Data
@AllArgsConstructor
public class TokenResponse {
    private Map<String, Object> tokens;
}

