package com.example.pfe_backend.services.ChatRoomService;

import com.example.pfe_backend.entities.chatRoom.Media;
import com.example.pfe_backend.entities.chatRoom.MediaType;
import com.example.pfe_backend.repos.ChatRoomRepo.MediaRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class MediaServiceImpl {

    private final MediaRepository mediaRepository;

    public String uploadMessageMedia(Integer chatId, MultipartFile file, MediaType mediaType, Long userId,
                                     String fileType) throws Exception {

        if (userId == null) {
            throw new Exception("Every media must have a userId");
        }

        if (chatId == null) {
            throw new Exception("Message media must contain a chatId");
        }

        Media media = new Media();
        media.setUserId(userId); // Conversion Long → String
        media.setFileType(fileType);
        media.setPicture(file.getBytes()); // Corrigé ici

        String title = "uploaded-media";
        if (file.getOriginalFilename() != null) {
            title = file.getOriginalFilename().replaceAll(" ", "-");
        }
        media.setTitle(title);
        media.setChatId(chatId);
        media.setMediaType(mediaType);

        Media savedMedia = mediaRepository.save(media);
        String mediaId = savedMedia.getId(); // ID de type String

        if (mediaId == null) {
            throw new Exception("Unable to upload media");
        }

        return mediaId;
    }

    public Media getMedia(Long userId, MediaType mediaType) {
        return mediaRepository.findOneByUserIdAndMediaType(userId, mediaType).orElse(null);
    }

    public Media getMediaById(String mediaId) {
        return mediaRepository.findMediaById(mediaId);
    }
}
