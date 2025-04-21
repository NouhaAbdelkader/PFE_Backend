package com.example.pfe_backend.Configurations.JWT;


import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.example.pfe_backend.exceptions.CustomException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    @Value("${security.jwt.secret-key}") // Clé secrète pour le JWT, tu peux l'ajouter dans un fichier de configuration
    private String jwtSecret ;  // Utiliser une clé secrète dans un fichier de configuration

    // Générer un token JWT
    public String generateJwtToken(NotifixUser user) {
             if (user == null) {
                 throw new CustomException("User not found");
             }
                return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + 2592000000L))  // Expiration dans 1 jour
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // Générer un refresh token JWT
    public String generateRefreshToken(NotifixUser user) {
        if (user == null) {
            throw new CustomException("User not found");
        }
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + 2592000000L))  // Expiration dans 30 jours
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }



    public String extractEmail(String token) {
        return Jwts.parser()
                .setSigningKey(jwtSecret.getBytes())
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // Vérifier si le token est valide
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser()
                    .setSigningKey(jwtSecret.getBytes())  // Correction ici
                    .parseClaimsJws(authToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Vérifier si le refresh token est valide
    public boolean validateRefreshToken(String refreshToken) {
        try {
            Jwts.parser()
                    .setSigningKey(jwtSecret.getBytes())  // Clé secrète pour valider le refresh token
                    .parseClaimsJws(refreshToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
