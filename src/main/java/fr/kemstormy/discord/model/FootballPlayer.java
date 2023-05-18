package fr.kemstormy.discord.model;


import fr.kemstormy.discord.enums.EFootballPlayerGenerationType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table
public class FootballPlayer {
    @Id
    private Long id;

    private String firstName;

    private String lastName;

    private int age;

    private EFootballPlayerGenerationType generationType;
}
