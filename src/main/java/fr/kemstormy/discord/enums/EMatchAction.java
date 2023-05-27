package fr.kemstormy.discord.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.hibernate.Hibernate;
import org.javacord.api.entity.channel.TextChannel;

import fr.kemstormy.discord.model.FootballPlayer;
import fr.kemstormy.discord.model.Team;
import fr.kemstormy.discord.resource.MatchDataResource;

public enum EMatchAction {
    CLEARANCE, SHORT_CLEARANCE, SHORT_BACK_PASS, SHORT_PROGRESSIVE_PASS, LONG_PASS, SHORT_SHOT, LONG_SHOT, PENALTY, DIRECT_FREEKICK, INDIRECT_FREEKICK, CORNER, END_OF_ACTION;

    public static EMatchAction processRandomAction(FootballPlayer playerWithBall, Team homeTeam, Team awayTeam) {
        Random r = new Random();
        List<EMatchAction> allActions = Arrays.asList(values());

        return allActions.get(r.nextInt(allActions.size()));
    }

    public static List<EMatchAction> getPossibleActions(FootballPlayer possessionner, boolean isKickoff, EMatchAction previousAction, FootballPlayer previousPlayer, EMatchEvent event) {
        List<EMatchAction> possibleActions = new ArrayList<>();

        switch(possessionner.getPost()) {
            case GOALKEEPER:
                possibleActions.add(CLEARANCE);
                possibleActions.add(SHORT_CLEARANCE);
                possibleActions.add(END_OF_ACTION);
                break;
            case DEFENDER:
                /***
                 * Un défenseur peut :
                 * - passer en retrait pour le gardien
                 * - passer le ballon à un autre défenseur ou un milieu
                 * - tirer de loin
                 * - tirer un corner
                 * ---- si l'action précédente est un corner réussi
                 * - tirer de près
                 * ---- si l'évènement précédent est un coup franc
                 * - coup franc indirect
                 * - coup franc direct
                 * -------- si la faute est sur un attaquant
                 * - penalty
                 */
                possibleActions.add(SHORT_BACK_PASS);
                //possibleActions.add(LONG_PASS);
                possibleActions.add(SHORT_PROGRESSIVE_PASS);
                possibleActions.add(LONG_SHOT);
                //possibleActions.add(CORNER);
                //possibleActions.add(END_OF_ACTION);

                if (previousPlayer != null && previousPlayer.getClub().equals(possessionner.getClub()) && event.equals(CORNER)) {
                    possibleActions.add(SHORT_SHOT);
                }

                if (event != null && event.equals(EMatchEvent.FOUL)) {
                    possibleActions.add(DIRECT_FREEKICK);
                    possibleActions.add(INDIRECT_FREEKICK);

                    if (previousPlayer != null && previousPlayer.getClub().equals(possessionner.getClub())) {
                        possibleActions.add(PENALTY);
                    }
                }
                break;
            case MIDFIELDER:
                /***
                 * Un milieu peut :
                 * - passer en retrait pour un défenseur
                 * - passer le ballon à un autre milieu ou un attaquant
                 * - tirer de loin
                 * - tirer un corner
                 * ---- si l'action précédente est un corner réussi
                 * - tirer de près
                 * ---- si l'évènement précédent est un coup franc
                 * - coup franc indirect
                 * - coup franc direct
                 * -------- si la faute est sur un attaquant
                 * - penalty
                 */
                possibleActions.add(SHORT_BACK_PASS);
                possibleActions.add(SHORT_PROGRESSIVE_PASS);
                possibleActions.add(LONG_SHOT);
                //possibleActions.add(CORNER);
                //possibleActions.add(END_OF_ACTION);

                if (previousPlayer != null && previousPlayer.getClub().equals(possessionner.getClub()) && event.equals(CORNER)) {
                    //possibleActions.add(SHORT_SHOT);
                }

                if (event != null && event.equals(EMatchEvent.FOUL)) {
                    possibleActions.add(DIRECT_FREEKICK);
                    possibleActions.add(INDIRECT_FREEKICK);

                    if (previousPlayer != null && previousPlayer.getClub().equals(possessionner.getClub())) {
                        possibleActions.add(PENALTY);
                    }
                }
                break;
            case FORWARD:
                /***
                 * Un attaquant peut :
                 * - passer en retrait pour un milieu
                 * - passer le ballon à un autre attaquant
                 * - tirer un corner
                 * ---- si l'action précédente est un corner réussi
                 * - tirer de près
                 * ---- si l'évènement précédent est un coup franc
                 * - coup franc indirect
                 * - coup franc direct
                 * -------- si la faute est sur un attaquant
                 * - penalty
                 * ------------------------------------------
                 * A l'engagement : seulement une short pass
                 */

                if (isKickoff) {
                    possibleActions.add(SHORT_BACK_PASS);
                    possibleActions.add(SHORT_PROGRESSIVE_PASS);
                    break;
                }
                possibleActions.add(SHORT_BACK_PASS);
                possibleActions.add(SHORT_PROGRESSIVE_PASS);
                possibleActions.add(SHORT_SHOT);
                //possibleActions.add(CORNER);
                //possibleActions.add(END_OF_ACTION);

                if (event != null && event.equals(EMatchEvent.FOUL)) {
                    possibleActions.add(DIRECT_FREEKICK);
                    possibleActions.add(INDIRECT_FREEKICK);

                    if (previousPlayer != null && previousPlayer.getClub().equals(possessionner.getClub())) {
                        possibleActions.add(PENALTY);
                    }
                }
                break;
            default:
                break;
        }

        return possibleActions;
    }

    public static MatchDataResource playAction(TextChannel tc, MatchDataResource resource, List<FootballPlayer> eligiblePlayers, List<FootballPlayer> allTeamPlayers) {
        String msg = "";
        Random randomPlayer = new Random();
        Random actionSuccess = new Random();
        EMatchEvent matchEvent = EMatchEvent.NOTHING;
        FootballPlayer nextFootballPlayer;
        Team playerClub = (Team) Hibernate.unproxy(resource.getPossessioner().getClub());

        // Definir le joueur destinataire
        if (eligiblePlayers != null && eligiblePlayers.size() > 0) {
            nextFootballPlayer = eligiblePlayers.get(randomPlayer.nextInt(eligiblePlayers.size()));
        } else {
            nextFootballPlayer = null;
        }

        switch(resource.getAction()) {
            case SHORT_PROGRESSIVE_PASS:
                tc.sendMessage(resource.getMinute() + "' - " + resource.getPossessioner().getMatchName() + " effectue une passe vers " + nextFootballPlayer.getMatchName() + ".");
                if (!actionSuccess.nextBoolean()) {
                    tc.sendMessage(resource.getPossessioner().getMatchName() + " rate sa passe, le ballon est perdu...");
                    matchEvent = EMatchEvent.NOTHING;
                    resource.setMatchEvent(matchEvent);
                    resource.setLastPossessioner(null);
                    break;
                } else {
                    EMatchAction nextAction = defineAction(nextFootballPlayer, false);
                    List<FootballPlayer> refreshedEligiblePlayers = getEligibleTeamMates(nextAction, allTeamPlayers, nextFootballPlayer);

                    resource.setAction(nextAction);
                    resource.setLastPossessioner(resource.getPossessioner());
                    resource.setPossessioner(nextFootballPlayer);

                    return playAction(tc, resource, refreshedEligiblePlayers, allTeamPlayers);
                }
            case SHORT_BACK_PASS:
                tc.sendMessage(resource.getMinute() + "' - " + resource.getPossessioner().getMatchName() + " effectue une passe en retrait vers " + nextFootballPlayer.getMatchName() + ".");
                if (!actionSuccess.nextBoolean()) {
                    tc.sendMessage(resource.getPossessioner().getMatchName() + " rate sa passe, le ballon est perdu...");
                    matchEvent = EMatchEvent.NOTHING;

                    resource.setMatchEvent(matchEvent);
                    resource.setLastPossessioner(null);

                    break;
                } else {
                    EMatchAction nextAction = defineAction(nextFootballPlayer, false);
                    List<FootballPlayer> refreshedEligiblePlayers = getEligibleTeamMates(nextAction, allTeamPlayers, nextFootballPlayer);
                    
                    resource.setAction(nextAction);
                    resource.setLastPossessioner(resource.getPossessioner());
                    resource.setPossessioner(nextFootballPlayer);

                    return playAction(tc, resource, refreshedEligiblePlayers, allTeamPlayers);
                }
            case SHORT_SHOT:
                tc.sendMessage(resource.getMinute() + "' - " + resource.getPossessioner().getMatchName() + " est en bonne position pour frapper, il tente sa chance...");
                if (!actionSuccess.nextBoolean()) {
                    tc.sendMessage("... et c'est raté, le ballon est perdu... :x:");
                    matchEvent = EMatchEvent.NOTHING;
                    resource.setLastPossessioner(null);
                    resource.setPossessioner(null);
                    break;
                } else {
                    if (playerClub.getName().equals(resource.getHomeTeam().getName())) {
                        resource.setScoreHome(resource.getScoreHome()+1);
                    } else {
                        resource.setScoreAway(resource.getScoreAway()+1);
                    }

                    if (resource.getLastPossessioner() != null) {
                        resource.getPassers().add(resource.getLastPossessioner());
                    }

                    resource.getScorers().add(resource.getPossessioner());

                    tc.sendMessage(":boom: GOAAAAAAAAAAAAAAAAL ! But marqué par " + resource.getPossessioner().getFirstName() + " " + resource.getPossessioner().getLastName() + " (" + resource.getScoreHome() + " - " + resource.getScoreAway() + ")");
                    
                    resource.setLastPossessioner(null);
                    resource.setPossessioner(null);

                    return resource;
                }
            case LONG_SHOT:
                tc.sendMessage(resource.getMinute() + "' - " + resource.getPossessioner().getMatchName() + " tente sa chance de loin...");
                if (!actionSuccess.nextBoolean()) {
                    tc.sendMessage("... et c'est raté. Il n'avait presque aucune chance de marquer à cette distance... :x:");
                    matchEvent = EMatchEvent.NOTHING;
                    resource.setMatchEvent(matchEvent);
                    resource.setLastPossessioner(null);
                    resource.setPossessioner(null);
                    break;
                } else {
                    if (playerClub.getName().equals(resource.getHomeTeam().getName())) {
                        resource.setScoreHome(resource.getScoreHome()+1);
                    } else {
                        resource.setScoreAway(resource.getScoreAway()+1);
                    }

                    if (resource.getLastPossessioner() != null) {
                        resource.getPassers().add(resource.getLastPossessioner());
                    }

                    resource.getScorers().add(resource.getPossessioner());

                    tc.sendMessage(":rocket: OH LE BOMBAZOOOOOOOOOOOOOOOOO ! Quel but splendide marqué par " + resource.getPossessioner().getFirstName() + " " + resource.getPossessioner().getLastName() + " (" + resource.getScoreHome() + " - " + resource.getScoreAway() + ")");
                    
                    resource.setLastPossessioner(null);
                    resource.setPossessioner(null);

                    return resource;
                }
            default:
                resource.setMatchEvent(EMatchEvent.NOTHING);

                resource.setLastPossessioner(null);
                resource.setPossessioner(null); 

                return resource;
        }

        /*switch(action) {
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
        */
        tc.sendMessage(msg);

        return resource;
    }

    public static EMatchAction defineAction(FootballPlayer possessionner, boolean isKickoff) {
        Random randomAction = new Random();

        List<EMatchAction> possibleActions = getPossibleActions(possessionner, isKickoff, null, null, null);
        return possibleActions.get(randomAction.nextInt(possibleActions.size()));
    }

    public static List<FootballPlayer> getEligibleTeamMates(EMatchAction previousAction, List<FootballPlayer> team, FootballPlayer possessionner) {
        List<FootballPlayer> eligible = new ArrayList(team);

        eligible.remove(possessionner);

        if (previousAction == null) {
            return eligible;
        }

        switch (previousAction) {
            case SHORT_PROGRESSIVE_PASS:
                if (possessionner.getPost().equals(EFootballPlayerPost.FORWARD)) {
                    eligible.removeAll(eligible.stream().filter(f -> !f.getPost().equals(EFootballPlayerPost.FORWARD)).toList());
                } else if (possessionner.getPost().equals(EFootballPlayerPost.MIDFIELDER)) {
                    eligible.removeAll(eligible.stream().filter(f -> f.getPost().equals(EFootballPlayerPost.DEFENDER) || f.getPost().equals(EFootballPlayerPost.GOALKEEPER)).toList());
                } else if (possessionner.getPost().equals(EFootballPlayerPost.DEFENDER)) {
                    eligible.removeAll(eligible.stream().filter(f -> f.getPost().equals(EFootballPlayerPost.FORWARD) || f.getPost().equals(EFootballPlayerPost.GOALKEEPER)).toList());
                } else {
                    eligible.removeAll(eligible.stream().filter(f -> !f.getPost().equals(EFootballPlayerPost.DEFENDER)).toList());
                }
                break;
            case SHORT_BACK_PASS:
                if (possessionner.getPost().equals(EFootballPlayerPost.FORWARD)) {
                    eligible.removeAll(eligible.stream().filter(f -> !f.getPost().equals(EFootballPlayerPost.MIDFIELDER)).toList());
                } else if (possessionner.getPost().equals(EFootballPlayerPost.MIDFIELDER)) {
                    eligible.removeAll(eligible.stream().filter(f -> !f.getPost().equals(EFootballPlayerPost.DEFENDER)).toList());
                } else {
                    eligible.removeAll(eligible.stream().filter(f -> !f.getPost().equals(EFootballPlayerPost.GOALKEEPER)).toList());
                }
                break;
            default:
                break;
        }

        return eligible;
    }
}
