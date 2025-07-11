package com.example.pfe_backend.entities.chatRoom;

import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.fasterxml.jackson.annotation.JsonManagedReference; // Changer ici
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private NotifixUser sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private NotifixUser receiver;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // Changer ici
    private List<message> messages = new ArrayList<>();
}