package fr.ecommerce.flore.aroma.flore_aroma.account.dto;


import lombok.Data;

@Data
public class RegisterRequest {
    private String countryCode;   // ex: "FR"
    private String firstName;
    private String lastName;
    private String email;
    private String phoneE164;
    private String password;
}

