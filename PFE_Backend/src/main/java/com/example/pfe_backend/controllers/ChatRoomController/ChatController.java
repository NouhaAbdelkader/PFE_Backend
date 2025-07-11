package com.example.pfe_backend.controllers.ChatRoomController;

import com.example.pfe_backend.entities.chatRoom.ChatRoom;
import com.example.pfe_backend.entities.chatRoom.Media;
import com.example.pfe_backend.entities.chatRoom.MediaType;
import com.example.pfe_backend.entities.chatRoom.message;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.example.pfe_backend.exceptions.CustomException;
import com.example.pfe_backend.repos.NotifixUserRepo.NotifixUserRepo;
import com.example.pfe_backend.services.ChatRoomService.ChatNotFoundException;
import com.example.pfe_backend.services.ChatRoomService.ChatServiceImpl;
import com.example.pfe_backend.services.ChatRoomService.MediaServiceImpl;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/Chat")
public class ChatController {
    private ChatServiceImpl chatService;
    private MediaServiceImpl mediaService;
    private SimpMessagingTemplate messagingTemplate;
    private NotifixUserRepo notifixUserRepo;
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);

    @PostMapping("/createChat/{senderId}/{receiverId}")
    public ResponseEntity<ChatRoom> createChat(@RequestBody ChatRoom chat, @PathVariable Long senderId, @PathVariable Long receiverId) {
        try {
            LOGGER.info("Creating chat for senderId: {}, receiverId: {}", senderId, receiverId);
            ChatRoom createdChat = chatService.addChat(chat, senderId, receiverId);
            LOGGER.info("Chat created successfully: {}", createdChat.getId());
            return ResponseEntity.ok(createdChat);
        } catch (Exception e) {
            LOGGER.error("Error creating chat for senderId: {}, receiverId: {}. Error: {}", senderId, receiverId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatRoom> getChatById(@PathVariable int id) {
        try {
            ChatRoom chat = chatService.getChatById(id);
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            LOGGER.error("Error fetching chat by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChatById(@PathVariable int id) {
        try {
            chatService.deleteChatById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LOGGER.error("Error deleting chat by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/getAllChatsByUser/{idUser}")
    public ResponseEntity<HashSet<ChatRoom>> getAllChatsByUser(@PathVariable Long idUser) {
        try {
            HashSet<ChatRoom> chats = chatService.getChatsByUser(idUser);
            return ResponseEntity.ok(chats);
        } catch (ChatNotFoundException e) {
            LOGGER.warn("No chats found for user {}: {}", idUser, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new HashSet<>());
        }
    }
    @GetMapping("/getChatBetweenUsers/{userId1}/{userId2}")
    public ResponseEntity<?> getChatBetweenUsers(@PathVariable Long userId1, @PathVariable Long userId2) {
        try {
            ChatRoom chat = chatService.getChatBetweenUsers(userId1, userId2);
            if (chat != null) {
                return ResponseEntity.ok(chat);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No chat found.");
            }
        } catch (Exception e) {
            LOGGER.error("Error fetching chat: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error.");
        }
    }


    @MessageMapping("/message")
    public void handleWebSocketMessage(@Payload message message) throws ChatNotFoundException {
        try {
            LOGGER.info("Received WebSocket message for chatRoom {}: {}", message.getChatRoom().getId(), message.getSendermessage());
            ChatRoom updatedChatRoom = chatService.addMessage(message, message.getChatRoom().getId(), null); // senderId non utilisé
            messagingTemplate.convertAndSend("/topic/chat/" + message.getChatRoom().getId(), message);
            LOGGER.info("Message sent to /topic/chat/{}: {}", message.getChatRoom().getId(), message.getSendermessage());
        } catch (Exception e) {
            LOGGER.error("Error processing WebSocket message: {}", e.getMessage(), e);
        }
    }

    @PostMapping("/message/{chatId}")
    public ResponseEntity<ChatRoom> addMessage(@RequestBody message message, @PathVariable int chatId) {
        try {
            ChatRoom updatedChatRoom = chatService.addMessage(message, chatId, null);
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, message);
            LOGGER.info("Message sent to /topic/chat/{}: {}", chatId, message.getSendermessage());
            return ResponseEntity.ok(updatedChatRoom);
        } catch (ChatNotFoundException e) {
            LOGGER.error("Chat not found: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            LOGGER.error("Error adding message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/media/message")
    public ResponseEntity<ChatRoom> addMediaMessage(
            @RequestParam int chatId,
            @RequestParam MultipartFile image,
            @RequestParam String fileType,
            @RequestParam Long userid) {
        try {
            NotifixUser user = notifixUserRepo.findNotifixUserByUserId(userid);
            if (user == null) {
                LOGGER.error("User not found: {}", userid);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            message message = new message();
            message.setSendermessage(user.getNom() + " " + user.getPrenom());
            message.setTime(new Date());
            ChatRoom chatRoom = chatService.getChatById(chatId);
            message.setChatRoom(chatRoom);
            ChatRoom updatedChatRoom = chatService.addMediaMessage(message, chatId, image, fileType, userid, MediaType.SHARED_MEDIA);
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, message);
            LOGGER.info("Media message sent to /topic/chat/{}: {}", chatId, message.getSendermessage());
            return ResponseEntity.ok(updatedChatRoom);
        } catch (Exception e) {
            LOGGER.error("Error adding media message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (ChatNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/media/{id}")
    public ResponseEntity<Media> getMediaById(@PathVariable String id) {
        try {
            Media media = mediaService.getMediaById(id);
            return ResponseEntity.ok(media);
        } catch (Exception e) {
            LOGGER.error("Error fetching media by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // Endpoint de test pour déboguer WebSocket
    @PostMapping("/test/sendMessage/{chatRoomId}")
    public ResponseEntity<String> sendTestMessage(@PathVariable Long chatRoomId, @RequestBody message message) {
        try {
            messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, message);
            LOGGER.info("Test message sent to /topic/chat/{}: {}", chatRoomId, message.getSendermessage());
            return ResponseEntity.ok("Test message sent.");
        } catch (Exception e) {
            LOGGER.error("Error sending test message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error sending test message.");
        }
    }

     ///
     @PostMapping("/markAsSeen/{chatId}/{userId}")
     public ResponseEntity<List<message>> markMessagesAsSeen(@PathVariable int chatId, @PathVariable Long userId) {
         try {
             List<message> markedMessages = chatService.markMessagesAsSeen(chatId, userId);
             // Envoyer une notification WebSocket pour informer les autres utilisateurs
             messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/seen", markedMessages);
             LOGGER.info("Messages marked as seen for chatId: {}, userId: {}", chatId, userId);
             return ResponseEntity.ok(markedMessages);
         } catch (CustomException e) {
             LOGGER.error("Error marking messages as seen: {}", e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
         } catch (Exception e) {
             LOGGER.error("Server error marking messages as seen: {}", e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
         }
     }

}