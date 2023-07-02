package fr.kemstormy.discord.model;

import fr.kemstormy.discord.enums.ERecordType;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table
@Data
public class PlayerRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @Nullable
    League league;

    String label;

    ERecordType recordType;

    @ManyToOne
    @Nullable
    FootballPlayer footballPlayer;
}
