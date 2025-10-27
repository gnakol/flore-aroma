package fr.ecommerce.flore.aroma.flore_aroma.whatsapp.controller;

import fr.ecommerce.flore.aroma.flore_aroma.whatsapp.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("phone-verification")
public class PhoneVerificationController {

    private final WhatsAppService whatsAppService;

    @DeleteMapping("/remove-phone-verification/{id}")
    public ResponseEntity<String> remove(@Validated @PathVariable Long id)
    {
        this.whatsAppService.remove(id);

        return ResponseEntity.status(202).body("Phone verification with ID : "+id+" was found successfully remove");
    }
}
