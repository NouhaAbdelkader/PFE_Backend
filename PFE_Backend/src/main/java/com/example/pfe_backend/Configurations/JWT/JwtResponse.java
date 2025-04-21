package com.example.pfe_backend.Configurations.JWT;

import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class JwtResponse {
    private String token;
    private NotifixUser user;
    private  String refreshtoken;

    public JwtResponse(String token, NotifixUser user, String refreshtoken) {
        this.token = token;
        this.user = user;
        this.refreshtoken=refreshtoken;
    }
    public JwtResponse(String token, NotifixUser user) {
        this.token = token;
        this.user = user;
    }
    public String getRefreshtoken() {
        return refreshtoken;
    }

    public void setRefreshtoken(String refreshtoken) {
        this.refreshtoken = refreshtoken;
    }

    // Getters et setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public NotifixUser getUser() {
        return user;
    }

    public void setUser(NotifixUser user) {
        this.user = user;
    }
}
