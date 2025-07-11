package com.example.pfe_backend.services.DerCoService;

import com.example.pfe_backend.entities.DerCo.DercoCsvDTO;
import com.example.pfe_backend.entities.Equipements.Equipement;
import com.example.pfe_backend.entities.Equipements.EtatEquipement;
import com.example.pfe_backend.repos.EquipementRepo.EquipementRepo;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class CsvProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(CsvProcessingService.class);

    private final EquipementRepo equipementRepository;
    private final DerCoService dercoService;
    private final String csvFilePath;

    public CsvProcessingService(
            EquipementRepo equipementRepository,
            DerCoService dercoService,
            @Value("${csvFilePath}") String csvFilePath) {
        this.equipementRepository = equipementRepository;
        this.dercoService = dercoService;
        this.csvFilePath = csvFilePath;
    }

    @Scheduled(cron = "0 0 * * * *") // Chaque heure
    public void scheduledScan() {
        try {
            List<DercoCsvDTO> dercoCsvList = readAndPreprocessCsv();
            dercoService.addDerCo(dercoCsvList);
            logger.info("✅ Scan CSV effectué avec succès à {}", new Date());
        } catch (Exception e) {
            logger.error("❌ Erreur lors du scan CSV automatique : {}", e.getMessage(), e);
        }
    }

    public List<DercoCsvDTO> readAndPreprocessCsv() throws Exception {
        // Vérifier si le fichier existe
        File file = new File(csvFilePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("Le fichier CSV n'existe pas : " + csvFilePath);
        }

        // Lire manuellement la première ligne pour vérifier l'en-tête
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new BOMInputStream(new FileInputStream(file)), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("Le fichier CSV est vide.");
            }
            String[] headers = headerLine.split(",");
            logger.info("En-tête du CSV : {}", Arrays.toString(headers));

            // Vérifier si les colonnes attendues sont présentes
            List<String> expectedHeaders = Arrays.asList("refEquipement", "descriptionPanne", "typePanne", "dateDetection", "servicesImpactes");
            for (String expected : expectedHeaders) {
                boolean found = false;
                for (String header : headers) {
                    if (header.trim().equalsIgnoreCase(expected)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    logger.warn("Colonne attendue '{}' non trouvée dans l'en-tête du CSV.", expected);
                }
            }
        }

        // Lire le fichier CSV avec OpenCSV
        List<DercoCsvDTO> dercoCsvList;
        try (Reader reader = new BufferedReader(new InputStreamReader(new BOMInputStream(new FileInputStream(file)), StandardCharsets.UTF_8))) {
            dercoCsvList = new CsvToBeanBuilder<DercoCsvDTO>(reader)
                    .withType(DercoCsvDTO.class)
                    .withSeparator(',') // S'assurer que le séparateur est une virgule
                    .withIgnoreLeadingWhiteSpace(true)
                    .withThrowExceptions(true) // Activer les exceptions pour voir les erreurs
                    .build()
                    .parse();
        } catch (Exception e) {
            logger.error("Erreur lors de la lecture du fichier CSV : {}", e.getMessage(), e);
            throw new IllegalArgumentException("Erreur lors de la lecture du fichier CSV : " + e.getMessage(), e);
        }

        // Vérifier les erreurs de parsing
        if (dercoCsvList == null || dercoCsvList.isEmpty()) {
            throw new IllegalArgumentException("Erreur de parsing du CSV : aucune donnée valide trouvée.");
        }

        // Ajouter des logs pour vérifier les données lues
        for (DercoCsvDTO dto : dercoCsvList) {
            logger.info("Ligne lue : {}", dto.toString());
            // Validation des champs
            if (dto.getRefEquipement() == null || dto.getRefEquipement().trim().isEmpty()) {
                logger.warn("Ligne invalide : refEquipement manquant ou vide - {}", dto);
                continue;
            }
            if (dto.getDescriptionPanne() == null || dto.getDescriptionPanne().trim().isEmpty()) {
                logger.warn("Ligne avec descriptionPanne manquante : {}", dto);
            }
            if (dto.getServicesImpactes() == null || dto.getServicesImpactes().trim().isEmpty()) {
                logger.warn("Ligne avec servicesImpactes manquante : {}", dto);
            }
            if (dto.getDateDetection() == null) {
                logger.warn("Ligne avec dateDetection manquante : {}", dto);
            }
        }

        // Prétraiter les données : Mettre à jour l'état des équipements
        List<DercoCsvDTO> validDercoCsvList = new ArrayList<>();
        int lineNumber = 1; // Compter l'en-tête si présent
        for (DercoCsvDTO dto : dercoCsvList) {
            try {
                // Validation des champs
                if (dto.getRefEquipement() == null || dto.getRefEquipement().trim().isEmpty()) {
                    logger.warn("Ligne {} : refEquipement manquant ou vide", lineNumber);
                    continue;
                }

                Optional<Equipement> equipementOpt = equipementRepository.findByRefEquipement(dto.getRefEquipement());
                if (equipementOpt.isPresent()) {
                    Equipement equipement = equipementOpt.get();
                    // Mettre à jour l'état à NONFONCTIONNEL
                    if (!EtatEquipement.NONFONCTIONNEL.equals(equipement.getEtatEquipement())) {
                        equipement.setEtatEquipement( (EtatEquipement.NONFONCTIONNEL));
                        equipementRepository.save(equipement);
                    }
                    validDercoCsvList.add(dto); // Ajouter le DTO à la liste pour traitement ultérieur
                } else {
                    logger.warn("Équipement avec refEquipement {} introuvable à la ligne {}", dto.getRefEquipement(), lineNumber);
                }
            } catch (Exception e) {
                logger.error("Erreur lors du traitement de la ligne {} : {}", lineNumber, e.getMessage(), e);
            }
            lineNumber++;
        }

        return validDercoCsvList;
    }
}