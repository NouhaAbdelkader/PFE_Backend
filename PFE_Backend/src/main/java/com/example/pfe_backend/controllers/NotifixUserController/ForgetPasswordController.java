package com.example.pfe_backend.controllers.NotifixUserController;

import com.example.pfe_backend.entities.notifixUser.ForgetPasswordRequest;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.example.pfe_backend.helpers.EmailService;
import com.example.pfe_backend.repos.NotifixUserRepo.NotifixUserRepo;
import com.example.pfe_backend.services.NotifixUserservice.ForgetPasswordService;
import com.example.pfe_backend.services.NotifixUserservice.NotifixUserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/auth")
public class ForgetPasswordController {


    private NotifixUserService notifixUserService;

    private NotifixUserRepo notifixUserRepo;
    private EmailService emailService;


    private PasswordEncoder passwordEncoder;

    private ForgetPasswordService forgetPasswordService;

    @PostMapping("/forgotPassword")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgetPasswordRequest request) {
        String email = request.getEmail();
        try {
            NotifixUser user = notifixUserRepo.findByEmail(email);
            if (user != null) {
                // step 1: generate password
                String temporaryPassword = forgetPasswordService.generateRandomPassword();
                // Step 2: Encode the temporary password
                String encodedPassword = passwordEncoder.encode(temporaryPassword);
                // Step 3: Update the user entity with the new encoded password
                user.setPassword(encodedPassword);
                // Step 4: Save the updated user entity to the database
                notifixUserRepo.save(user);
                // step 5: send email
                emailService.sendSimpleMessage(email, "Password Reset", "Your new temporary password is: " + temporaryPassword);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Temporary password sent to your email.");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.out.println("Error during password reset: " + e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

}