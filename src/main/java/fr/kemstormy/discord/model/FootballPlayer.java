package fr.kemstormy.discord.model;

import fr.kemstormy.discord.enums.EFootballPlayerGenerationType;
import fr.kemstormy.discord.enums.EFootballPlayerPost;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table
public class FootballPlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;

    private String lastName;

    private int age;

    private EFootballPlayerGenerationType generationType;

    @Nullable
    @OneToOne(fetch = FetchType.LAZY)
    private DiscordUser owner;

    private EFootballPlayerPost post;

    @OneToOne(fetch = FetchType.LAZY)
    @Nullable
    private Team club;

    @OneToOne(fetch = FetchType.LAZY)
    private PlayerCharacteristics playerCharacteristics = new PlayerCharacteristics();

    @OneToOne(fetch = FetchType.LAZY)
    private Nationality nationality;

    int level = 1;

    int pointsToSet = 0;

    int shape = 100;

    public String getMatchName() {
        return this.getFirstName().toUpperCase().charAt(0) + ". " + this.getLastName();
    }
}
