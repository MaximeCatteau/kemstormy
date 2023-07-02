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

    int experience = 0;

    int passes = 1;

    int interceptions = 1;

    int shots = 1;

    int longShots = 1;

    int longPasses = 1;

    int shotStopping = 1;

    int corners = 1;

    int freekicks = 1;

    int dribbles = 1;

    int tackles = 1;

    int penalty = 1;
}
