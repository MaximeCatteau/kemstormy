package fr.kemstormy.discord.model;

import java.time.Instant;

import lombok.Data;

@Data
public class Match {
    Team homeTeam;

    Team awayTeam;

    Instant date;

    public String toString() {
        return this.getHomeTeam().getName() + " vs. " + this.getAwayTeam().getName();
    }
}
