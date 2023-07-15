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
public class Nationality {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String flag;

    String country;

    String gentilé;
}
