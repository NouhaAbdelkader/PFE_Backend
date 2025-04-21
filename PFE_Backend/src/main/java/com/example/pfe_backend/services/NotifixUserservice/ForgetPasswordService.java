package com.example.pfe_backend.services.NotifixUserservice;


import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Random;
@Service
@AllArgsConstructor
public class ForgetPasswordService {

    public String generateRandomPassword() {
        int length = 10;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }
}