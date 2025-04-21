package com.example.pfe_backend.controllers.EquipementsController;

import com.example.pfe_backend.entities.Equipements.Equipement;
import com.example.pfe_backend.entities.notifixUser.NotifixUser;
import com.example.pfe_backend.services.EquipementsService.EquipementService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/equipements")

public class EquipementController {

    private final EquipementService equipementService;



    // Endpoint pour ajouter un équipement
    @PostMapping("/add")
    public ResponseEntity<Equipement> addEquipement(@RequestBody Equipement equipement) {
        Equipement createdEquipement = equipementService.addEquipement(equipement);
        return ResponseEntity.ok(createdEquipement);
    }

    // Endpoint pour récupérer la liste des équipements
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
}
