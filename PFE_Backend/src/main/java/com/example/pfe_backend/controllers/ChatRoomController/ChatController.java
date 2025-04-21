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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashSet;

@RestController
@AllArgsConstructor
@RequestMapping("/Chat")
public class ChatController {
    private ChatServiceImpl chatService;
    private MediaServiceImpl mediaService ;
    private SimpMessagingTemplate messagingTemplate;
    private NotifixUserRepo notifixUserRepo;
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);
    @PostMapping("/createChat/{senderId}/{receiverId}")
    public ChatRoom createChat(@RequestBody ChatRoom c, @PathVariable Long senderId, @PathVariable Long receiverId) {
        return chatService.addChat(c, senderId, receiverId);

    }
    @GetMapping("/sentiment/{text}")
    public void processText(String text, NotifixUser user){
        //sentimentAnalysisService.analyzeSentiment(text ,user);
        // Autres opérations à effectuer après l'analyse de sentiment, si nécessaire
    }

    @GetMapping("/{id}")
    public ChatRoom getChatById(@PathVariable int id) {
        return chatService.getChatById(id);
    }

    @DeleteMapping("/{id}")
    public Void deleteChatById(@PathVariable int id) {
        chatService.deleteChatById(id);

        return null;
    }

    //For teacher
    @GetMapping("/getAllChatsByUser/{idUser}")
    public HashSet<ChatRoom> getAllChatBySender(@PathVariable Long idUser) throws ChatNotFoundException {
        return chatService.getChatsByUser(idUser);

    }
    @GetMapping("/getAllChatsBySenderOrReciver/{idUser}/{idUser2}")
    public HashSet<ChatRoom> getAllChatBySenderOrRciver(@PathVariable Long idUser,@PathVariable Long idUser2) throws ChatNotFoundException {
        return chatService.getChatsByUSenderIOrrReciver(idUser,idUser2);

    }
    @GetMapping("/getChatBySenderAndReceiver/{idSender}/{idReceiver}")
    public ResponseEntity<?> getChatBySenderAndReceiver(@PathVariable Long idSender, @PathVariable Long idReceiver) {
        try {
            ChatRoom chat = chatService.getChatBySenderAndReceiver(idSender, idReceiver);

            if (chat != null) {
                return ResponseEntity.ok(chat);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No chat found.");
            }
        } catch (Exception e) {
            LOGGER.error("Exception while fetching chat: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error while fetching chat.");
        }
    }



    @GetMapping("get/getChatBySenderAndReceiverOpp/{idReceiver}/{idSender}")
    public ChatRoom getChatBySenderAndRceiver2(@PathVariable Long idReceiver,@PathVariable Long idSender ) {
        return chatService.getChatBySenderAndReceiver2(idReceiver,idSender);
    }

    @MessageMapping("/message")
    @PutMapping("/message/{chatId}/{senderId}")
    public ResponseEntity<ChatRoom> addMessage(@RequestBody message add, @PathVariable int chatId, @PathVariable Long senderId)
            throws ChatNotFoundException, CustomException, InterruptedException {
        Thread.sleep(1000);
        ChatRoom updatedChatRoom = chatService.addMessage(add, chatId, senderId);
        LOGGER.info("controllleeeeeeeer notif222222222222 "+ add.getReplymessage());


        NotifixUser u = notifixUserRepo.findNotifixUserByUserId(senderId);

        // Vous pouvez également envoyer un message supplémentaire avec les détails du nouveau message ajouté
        String notificationMessage = "NNew message send To you By " + u.getNom()+ " " + u.getPrenom() ;

        // Envoyer le message aux abonnés du sujet WebSocket
        messagingTemplate.convertAndSend("/topic/greetings", notificationMessage);



        // Envoyer le message aux abonnés du sujet WebSocket
        return new ResponseEntity<>(updatedChatRoom, HttpStatus.OK);
    }

    @PostMapping("/media/message")
    public ResponseEntity<ChatRoom> addMediaMessage(@RequestParam int chatId,
                                                    @RequestParam MultipartFile image, @RequestParam String fileType, @RequestParam Long userid)
            throws ChatNotFoundException, Exception, CustomException {
    NotifixUser user = notifixUserRepo.findNotifixUserByUserId(userid);

        message message = new message();
        message.setSendermessage(user.getNom() + " " + user.getPrenom());
        return new ResponseEntity<ChatRoom>(
                chatService.addMediaMessage(message, chatId, image, fileType, userid, MediaType.SHARED_MEDIA),
                org.springframework.http.HttpStatus.OK);
    }
    @GetMapping("media/{id}")
    public Media getMediaById(@PathVariable String  id) {
        return mediaService.getMediaById(id);
    }


}
