package com.example.pfe_backend.controllers.DercoController;

import com.example.pfe_backend.entities.DerCo.Derco;
import com.example.pfe_backend.entities.DerCo.DercoCsvDTO;
import com.example.pfe_backend.services.DerCoService.CsvProcessingService;
import com.example.pfe_backend.services.DerCoService.DerCoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/derco")
@RequiredArgsConstructor
public class DercoController {

    private final CsvProcessingService csvProcessingService;
    private final DerCoService derCoService;
    private static final Logger logger = LoggerFactory.getLogger(DercoController.class);

    @PostMapping("/scan")
    public ResponseEntity<List<Derco>> scanAndCreateDercoFromCsv() {
        try {
            List<DercoCsvDTO> dercoCsvList = csvProcessingService.readAndPreprocessCsv();
            derCoService.addDerCo(dercoCsvList);
            return ResponseEntity.ok().body(null);
        } catch (Exception e) {
            logger.error("Error scanning CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/new")
    public ResponseEntity<List<Derco>> getNewDercos() {
        try {
            List<Derco> newDercos = derCoService.newDercos();
            logger.info("Retrieved {} new Dercos with no interventions", newDercos.size());
            return ResponseEntity.ok(newDercos);
        } catch (Exception e) {
            logger.error("Error retrieving new Dercos: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Derco>> getAllDercos() {
        List<Derco> dercos = derCoService.getAll();
        return ResponseEntity.ok(dercos);
    }



    @PutMapping("/update")
    public ResponseEntity<Derco> updateDerco(@RequestBody Derco derco) {
        Derco updatedDerco = derCoService.updateDerco(derco);
        return ResponseEntity.ok(updatedDerco);
    }

    @PostMapping("/add")
    public ResponseEntity<Derco> addNewDerCo(@Valid @RequestBody Derco derco) {
        try {
            logger.info("Received JSON for new Derco: {}", new ObjectMapper().writeValueAsString(derco));
            logger.info("EquipementImpacte: idEquipement={}, refEquipement={}",
                    derco.getEquipementImpacte() != null ? derco.getEquipementImpacte().getIdEquipement() : "null",
                    derco.getEquipementImpacte() != null ? derco.getEquipementImpacte().getRefEquipement() : "null");
            Derco newDerco = derCoService.newDerCo(derco);
            logger.info("Successfully added Derco with ID: {}", newDerco.getIdDerCo());
            return ResponseEntity.ok(newDerco);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error adding Derco: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            logger.error("Error adding Derco: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteDerco(@PathVariable Long id) {
        derCoService.deleteDerco(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/filter")
    public ResponseEntity<List<Derco>> filterDercos(@Valid @RequestBody Derco filter) {
        try {
            List<Derco> filteredDercos = derCoService.findByFilters(filter);

            logger.info("Filtered {} DerCos", filteredDercos.size());
            return ResponseEntity.ok(filteredDercos);
        } catch (Exception e) {
            logger.error("Error filtering DerCos: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }
}