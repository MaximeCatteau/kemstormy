package fr.kemstormy.discord.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table
@Data
public class PlayerCharacteristics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    int experience = 0;

    int passes = 1;

    int interceptions = 1;

    int shots = 1;
}
