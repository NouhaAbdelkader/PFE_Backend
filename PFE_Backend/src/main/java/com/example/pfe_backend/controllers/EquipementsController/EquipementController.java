package com.example.pfe_backend.controllers.EquipementsController;

import com.example.pfe_backend.entities.Equipements.Equipement;
import com.example.pfe_backend.services.EquipementsService.EquipementService;
import com.example.pfe_backend.services.EquipementsService.GeoUtils;
import lombok.AllArgsConstructor;
 import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/equipements")
@CrossOrigin(origins = "http://localhost:4200")
public class EquipementController {

    private final EquipementService equipementService;

    @PostMapping("/add")
    public ResponseEntity<Equipement> addEquipement(@RequestBody Equipement equipement) {
        Equipement createdEquipement = equipementService.addEquipement(equipement);
        return ResponseEntity.ok(createdEquipement);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Equipement>> getEquipements() {
        List<Equipement> equipements = equipementService.getEquipements();
        return ResponseEntity.ok(equipements);
    }

    @PutMapping("/update")
    public ResponseEntity<Equipement> updateEquipment(@RequestBody Equipement equipement) {
        Equipement updatedEquipement = equipementService.UpdateEquipement(equipement);
        return ResponseEntity.ok(updatedEquipement);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        equipementService.deleteEquipement(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/city")
    public ResponseEntity<String> getCityFromCoordinates(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon) {
        String city = GeoUtils.getCityFromCoordinates(lat, lon);
        if (city.startsWith("Geo error") || city.startsWith("Unknown city")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(city);
        }
        return ResponseEntity.ok(city);
    }

    @GetMapping("/address")
    public ResponseEntity<String> getAddressFromCoordinates(
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude) {
        String address = GeoUtils.getAddressFromCoordinates(latitude, longitude);
        if (address.startsWith("Geo error") || address.startsWith("Unknown address")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(address);
        }
        return ResponseEntity.ok(address);
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Equipement>> filterEquipements(
            @RequestParam(required = false) String dateInstallation, // Expected format: YYYY-MM
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String adresse,
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) String etat,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        try {
            Date parsedDate = null;
            if (dateInstallation != null && !dateInstallation.trim().isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
                parsedDate = sdf.parse(dateInstallation);
            }
            List<Equipement> equipements = equipementService.fetchFilteredEquipements(
                    parsedDate, ville, adresse, categorie, etat, page, size);
            return ResponseEntity.ok(equipements);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/filter/count")
    public ResponseEntity<Long> countFilteredEquipements(
            @RequestParam(required = false) String dateInstallation,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String adresse,
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) String etat
    ) {
        try {
            Date parsedDate = null;
            if (dateInstallation != null && !dateInstallation.trim().isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
                parsedDate = sdf.parse(dateInstallation);
            }
            long count = equipementService.countFilteredEquipements(parsedDate, ville, adresse, categorie, etat);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


}