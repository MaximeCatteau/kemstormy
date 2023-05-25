package fr.kemstormy.discord.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import fr.kemstormy.discord.enums.EMatchStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @OneToOne
    Team homeTeam;

    @OneToOne
    Team awayTeam;

    Instant date;

    EMatchStatus status;

    @OneToMany
    List<FootballPlayer> scorers = new ArrayList<>();

    @OneToMany
    List<FootballPlayer> decisivePassers = new ArrayList<>();

    int scoreHome = 0;

    int scoreAway = 0;

    @OneToOne
    League competition;

    public String toString() {
        return this.getHomeTeam().getName() + " vs. " + this.getAwayTeam().getName();
    }
}
