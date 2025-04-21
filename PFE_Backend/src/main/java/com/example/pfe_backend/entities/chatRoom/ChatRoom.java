package com.example.pfe_backend.entities.chatRoom;

import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ChatRoom implements Serializable {

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
    private List<message> messages = new ArrayList<>();
}
