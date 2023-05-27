package fr.kemstormy.discord.model;

import jakarta.annotation.Nullable;
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

    int passes = 0;

    int interceptions = 0;

    int shots = 0;
}
