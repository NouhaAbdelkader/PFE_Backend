package com.example.pfe_backend.entities.notifixUser;

import com.example.pfe_backend.helpers.ValidPassword;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;


@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "notifix_users") // Définit explicitement le nom de la table
public class NotifixUser implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long userId; // Renommé en 'id' (plus standard)

    @NotEmpty(message = "firstName cannot be empty")
    private String nom;
    @NotEmpty(message = "lastName cannot be empty")
    private String prenom;
    @Email(message = "Email is not valid", regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")
    @NotEmpty(message = "Email cannot be empty")
    private String email;
    @NotNull
    @ValidPassword
    @NotEmpty(message = "password cannot be empty")
    private String password;
    @NotBlank(message = "Phone number is mandatory")
    @Pattern(regexp = "\\d{8}", message = "Phone number must be 8 digits")
    private String numero;

    @Enumerated(EnumType.STRING)
    private Role role;
    @Enumerated(EnumType.STRING)

   private Status status;
}
