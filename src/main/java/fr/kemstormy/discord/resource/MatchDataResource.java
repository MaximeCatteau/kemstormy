package fr.kemstormy.discord.resource;

import java.util.ArrayList;
import java.util.List;

import fr.kemstormy.discord.enums.EMatchAction;
import fr.kemstormy.discord.enums.EMatchEvent;
import fr.kemstormy.discord.model.FootballPlayer;
import fr.kemstormy.discord.model.Match;
import fr.kemstormy.discord.model.Team;
import lombok.Data;

@Data
public class MatchDataResource {
    private Match match;

    private List<FootballPlayer> scorers = new ArrayList<>();

    private List<FootballPlayer> passers = new ArrayList<>();

    private FootballPlayer possessioner;

    private FootballPlayer lastPossessioner;

    private Team homeTeam;

    private Team awayTeam;

    private int scoreHome = 0;

    private int scoreAway = 0;

    private int minute = 0;

    private EMatchEvent matchEvent;

    private EMatchAction action;
}
