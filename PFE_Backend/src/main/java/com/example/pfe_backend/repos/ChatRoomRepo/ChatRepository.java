package com.example.pfe_backend.repos.ChatRoomRepo;

import com.example.pfe_backend.entities.chatRoom.ChatRoom;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.HashSet;
import java.util.List;

public interface ChatRepository extends JpaRepository<ChatRoom, Integer> {
    HashSet<ChatRoom> findChatRoomByReceiver(NotifixUser receiver);
    HashSet<ChatRoom> findChatRoomBySender(NotifixUser receiver);
    HashSet<ChatRoom> findBySenderOrReceiver(NotifixUser sender, NotifixUser receiver);

    ChatRoom findChatRoomBySenderAndReceiver(NotifixUser sender,NotifixUser receiver);
    ChatRoom findChatRoomByReceiverAndSender(NotifixUser receiver,NotifixUser sender);
}
