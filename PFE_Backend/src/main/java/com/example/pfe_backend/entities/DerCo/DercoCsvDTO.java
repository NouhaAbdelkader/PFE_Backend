package com.example.pfe_backend.entities.DerCo;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DercoCsvDTO {

    @CsvBindByName(column = "refEquipement")
    private String refEquipement;

    @CsvBindByName(column = "descriptionPanne")
    private String descriptionPanne;

    @CsvBindByName(column = "typePanne")
    private String typePanne;

    @CsvBindByName(column = "dateDetection")
    @CsvDate("yyyy-MM-dd")
    private Date dateDetection;

    @CsvBindByName(column = "servicesImpactes")
    private String servicesImpactes;
}