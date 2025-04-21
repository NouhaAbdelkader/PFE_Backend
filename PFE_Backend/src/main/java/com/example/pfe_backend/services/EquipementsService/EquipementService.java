package com.example.pfe_backend.services.EquipementsService;

import com.example.pfe_backend.entities.Equipements.Equipement;
import com.example.pfe_backend.exceptions.CustomException;
import com.example.pfe_backend.repos.EquipementRepo.EquipementRepo;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class EquipementService implements IEquipementService{

    private EquipementRepo equipementRepository;

    @Override
    public Equipement addEquipement(Equipement equipement) {
        return equipementRepository.save(equipement);

    }

    @Override
    public List<Equipement> getEquipements() {
        return equipementRepository.findAll();
    }

    @Override
    public Equipement UpdateEquipement(Equipement equipement) {
        return equipementRepository.save(equipement);
    }

    @Override
    public void deleteEquipement(Long EquipementId) {
        if (!equipementRepository.existsById(EquipementId) ){
            throw new CustomException("Equipement not found");
        }
        equipementRepository.deleteById(EquipementId);
    }
}
