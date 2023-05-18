package fr.kemstormy.discord.model;


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
}
