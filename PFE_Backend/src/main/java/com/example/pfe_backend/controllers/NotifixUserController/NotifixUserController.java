package com.example.pfe_backend.controllers.NotifixUserController;

import com.example.pfe_backend.Configurations.JWT.JwtResponse;
import com.example.pfe_backend.Configurations.JWT.JwtUtils;
import com.example.pfe_backend.Configurations.JWT.LoginRequest;
import com.example.pfe_backend.Configurations.JWT.RefreshTokenRequest;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.example.pfe_backend.exceptions.CustomException;
import com.example.pfe_backend.services.NotifixUserservice.NotifixUserService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class NotifixUserController {

    private final NotifixUserService notifixUserService;
    private final JwtUtils jwtUtils;

    // Login route
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            JwtResponse jwtResponse = notifixUserService.login(loginRequest.getEmail(), loginRequest.getPassword());

            return ResponseEntity.ok(jwtResponse);
        } catch (CustomException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Internal server error"));
        }
    }

    // Refresh JWT using refresh token from cookie
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshJwtToken(@RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshtoken();

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Missing refresh token"));
        }

        try {
            if (!jwtUtils.validateRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Collections.singletonMap("error", "Invalid refresh token"));
            }

            String email = jwtUtils.extractEmail(refreshToken);
            NotifixUser user = notifixUserService.getUserByEmail(email);

            String newJwtToken = jwtUtils.generateJwtToken(user);
            String newRefreshToken = jwtUtils.generateRefreshToken(user);

            return ResponseEntity.ok(new JwtResponse(newJwtToken, user, newRefreshToken));

        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Refresh token expired"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Internal server error"));
        }
    }



    // Create new Notifix user
    @PostMapping("/add")
    public ResponseEntity<NotifixUser> addNotifixUser(@Valid @RequestBody NotifixUser notifixUser) {
        NotifixUser createdUser = notifixUserService.addNotifixUser(notifixUser);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    // Get all Notifix users
    @GetMapping("/all")
    public ResponseEntity<List<NotifixUser>> getNotifixUsers() {
        List<NotifixUser> users = notifixUserService.getUsersNotifix();
        return ResponseEntity.ok(users);
    }
    //update
    @PutMapping("/update")
    public ResponseEntity<NotifixUser> updateNotifixUser(@RequestBody NotifixUser notifixUser) {
        NotifixUser updatedUser = notifixUserService.updateNotifixUser(notifixUser);
        return ResponseEntity.ok(updatedUser);
    }


    // Change password
    @PutMapping("/changePassword")
    public ResponseEntity<NotifixUser> changePassword(@RequestBody NotifixUser user) {
        try {
            NotifixUser updatedUser = notifixUserService.changePassword(user);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Verify current password
    @PostMapping("/verify-password")
    public ResponseEntity<Boolean> verifyPassword(@RequestBody NotifixUser userTripAura, @RequestParam String currentPassword) {
        Boolean isPasswordValid = notifixUserService.verifPassword(userTripAura, currentPassword);
        return ResponseEntity.ok(isPasswordValid);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteNotifixUser(@PathVariable Long id) {
        notifixUserService.deleteNotifixUser(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String email) {
        notifixUserService.logout(email);
        return ResponseEntity.ok("Logout successful and status set to INACTIVE");
    }

}
