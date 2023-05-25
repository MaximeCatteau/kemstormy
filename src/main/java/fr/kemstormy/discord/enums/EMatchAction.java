package fr.kemstormy.discord.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.javacord.api.entity.channel.TextChannel;

import fr.kemstormy.discord.model.FootballPlayer;
import fr.kemstormy.discord.model.Team;

public enum EMatchAction {
    CLEARANCE, SHORT_CLEARANCE, SHORT_BACK_PASS, SHORT_PROGRESSIVE_PASS, SHORT_SHOT, LONG_SHOT, PENALTY, DIRECT_FREEKICK, INDIRECT_FREEKICK;

    public static EMatchAction processRandomAction(FootballPlayer playerWithBall, Team homeTeam, Team awayTeam) {
        Random r = new Random();
        List<EMatchAction> allActions = Arrays.asList(values());

        return allActions.get(r.nextInt(allActions.size()));
    }

    public static EMatchEvent playAction(TextChannel tc, EMatchAction action, FootballPlayer player, Team homeTeam, List<FootballPlayer> homePlayers, Team awayTeam, List<FootballPlayer> awayPlayers, int minute, int scoreHome, int scoreAway) {
        String msg = "";
        Random randomPlayer = new Random();
        Random actionSuccess = new Random();
        EMatchEvent matchEvent = EMatchEvent.NOTHING;

        FootballPlayer passReceiver = homePlayers.get(randomPlayer.nextInt(homePlayers.size()));

        switch(action) {
            case CLEARANCE:
                msg = minute + "' - " + player.getFirstName().toUpperCase().charAt(0) + ". " + player.getLastName() + " dégage le ballon loin devant.";
                matchEvent = EMatchEvent.NOTHING;
                break;
            case SHORT_CLEARANCE:
                msg = minute + "' - " + player.getFirstName().toUpperCase().charAt(0) + ". " + player.getLastName() + " relance court.";
                matchEvent = EMatchEvent.NOTHING;
                break;
            case SHORT_BACK_PASS:
                passReceiver = homePlayers.get(randomPlayer.nextInt(homePlayers.size()));
                msg = minute + "' - " + player.getFirstName().toUpperCase().charAt(0) + ". " + player.getLastName() + " effectue une passe courte vers " + passReceiver.getFirstName().toUpperCase().charAt(0) +". " + passReceiver.getLastName() + ".";
                matchEvent = EMatchEvent.NOTHING;
                break;
            case SHORT_PROGRESSIVE_PASS:
                passReceiver = homePlayers.get(randomPlayer.nextInt(homePlayers.size()));
                msg = minute + "' - " + player.getFirstName().toUpperCase().charAt(0) + ". " + player.getLastName() + " effectue une passe courte vers " + passReceiver.getFirstName().toUpperCase().charAt(0) +". " + passReceiver.getLastName() + ".";
                matchEvent = EMatchEvent.NOTHING;
                break;
            case SHORT_SHOT:
                if (actionSuccess.nextBoolean()) {
                    msg = minute + "' - " + player.getFirstName().toUpperCase().charAt(0) + ". " + player.getLastName() + " pénètre dans la surface, frappe et marque !";
                    if (player.getClub().getId() == homeTeam.getId()) {
                        matchEvent = EMatchEvent.GOAL_HOME;
                    } else {
                        matchEvent = EMatchEvent.GOAL_AWAY;
                    }
                    break;
                } else {
                    msg = minute + "' - " + player.getFirstName().toUpperCase().charAt(0) + ". " + player.getLastName() + " pénètre dans la surface, frappe mais le gardien adverse stoppe sa tentative !";
                    break;
                }
            case LONG_SHOT:
                if (actionSuccess.nextBoolean()) {
                    msg = minute + "' - " + player.getFirstName().toUpperCase().charAt(0) + ". " + player.getLastName() + " prend sa chance de loin et marque ! BOMBAZOOOOO ! :rocket:";
                    if (player.getClub().getId() == homeTeam.getId()) {
                        matchEvent = EMatchEvent.GOAL_HOME;
                    } else {
                        matchEvent = EMatchEvent.GOAL_AWAY;
                    }
                    break;
                } else {
                    msg = minute + "' - " + player.getFirstName().toUpperCase().charAt(0) + ". " + player.getLastName() + " tente sa chance de loin ! C'est raté...";
                    break;
                }
            case PENALTY:
                Team team = player.getClub();
                msg = minute + "' - Penalty pour " + team.getName() + " !\n";
                if (actionSuccess.nextBoolean()) {
                    msg += player.getFirstName().toUpperCase().charAt(0) + ". " + player.getLastName() + " s'élance et trompe le gardien !!!";
                    if (player.getClub().getId() == homeTeam.getId()) {
                        matchEvent = EMatchEvent.GOAL_HOME;
                    } else {
                        matchEvent = EMatchEvent.GOAL_AWAY;
                    }
                    break;
                } else {
                    msg += player.getFirstName().toUpperCase().charAt(0) + ". " + player.getLastName() + " s'élance mais manque complètement son geste !";
                    break;
                }
            case DIRECT_FREEKICK:
            case INDIRECT_FREEKICK:
                msg = minute + "' - Coup franc en faveur de " + homeTeam.getName();
                break;
            default:
                msg = minute + "' - L'action se poursuit.";
                break;
        }

        tc.sendMessage(msg);

        return matchEvent;
    }
}
