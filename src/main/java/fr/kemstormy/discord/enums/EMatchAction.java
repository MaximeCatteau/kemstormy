package fr.kemstormy.discord.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.hibernate.Hibernate;
import org.javacord.api.entity.channel.TextChannel;

import fr.kemstormy.discord.model.FootballPlayer;
import fr.kemstormy.discord.model.PlayerCharacteristics;
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
                possibleActions.add(LONG_PASS);
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
                possibleActions.add(LONG_PASS);
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
        PlayerCharacteristics pc = (PlayerCharacteristics) Hibernate.unproxy(resource.getPossessioner().getPlayerCharacteristics());

        // Definir le joueur destinataire
        if (eligiblePlayers != null && eligiblePlayers.size() > 0) {
            nextFootballPlayer = eligiblePlayers.get(randomPlayer.nextInt(eligiblePlayers.size()));
        } else {
            nextFootballPlayer = null;
        }

        float actionSuccessPercentage = calculateActionSuccessProbability(resource.getPossessioner(), pc, resource.getAction(), nextFootballPlayer.getPost());

        switch(resource.getAction()) {
            case SHORT_PROGRESSIVE_PASS:
                tc.sendMessage(resource.getMinute() + "' - " + resource.getPossessioner().getMatchName() + " effectue une passe vers " + nextFootballPlayer.getMatchName() + ".");
                if (!actionSuccess(actionSuccessPercentage)) {
                    tc.sendMessage(resource.getPossessioner().getMatchName() + " rate sa passe, le ballon est perdu...");
                    matchEvent = EMatchEvent.NOTHING;
                    resource.setMatchEvent(matchEvent);
                    resource.setLastPossessioner(null);
                    break;
                } else {
                    EMatchAction nextAction = defineAction(nextFootballPlayer, false);
                    List<FootballPlayer> refreshedEligiblePlayers = getEligibleTeamMates(nextAction, allTeamPlayers, nextFootballPlayer);

                    processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 1);

                    resource.setAction(nextAction);
                    resource.setLastPossessioner(resource.getPossessioner());
                    resource.setPossessioner(nextFootballPlayer);

                    return playAction(tc, resource, refreshedEligiblePlayers, allTeamPlayers);
                }
            case SHORT_BACK_PASS:
                tc.sendMessage(resource.getMinute() + "' - " + resource.getPossessioner().getMatchName() + " effectue une passe en retrait vers " + nextFootballPlayer.getMatchName() + ".");
                if (!actionSuccess(actionSuccessPercentage)) {
                    tc.sendMessage(resource.getPossessioner().getMatchName() + " rate sa passe, le ballon est perdu...");
                    matchEvent = EMatchEvent.NOTHING;

                    resource.setMatchEvent(matchEvent);
                    resource.setLastPossessioner(null);

                    break;
                } else {
                    EMatchAction nextAction = defineAction(nextFootballPlayer, false);
                    List<FootballPlayer> refreshedEligiblePlayers = getEligibleTeamMates(nextAction, allTeamPlayers, nextFootballPlayer);
                    
                    processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 1);

                    resource.setAction(nextAction);
                    resource.setLastPossessioner(resource.getPossessioner());
                    resource.setPossessioner(nextFootballPlayer);

                    return playAction(tc, resource, refreshedEligiblePlayers, allTeamPlayers);
                }
            case LONG_PASS:
                tc.sendMessage(resource.getMinute() + "' - " + resource.getPossessioner().getMatchName() + " tente une longue passe vers " + nextFootballPlayer.getMatchName() + ".");
                if (!actionSuccess(actionSuccessPercentage)) {
                    tc.sendMessage(resource.getPossessioner().getMatchName() + " rate sa passe, le ballon est perdu...");
                    matchEvent = EMatchEvent.NOTHING;
                    resource.setMatchEvent(matchEvent);
                    resource.setLastPossessioner(null);
                    break;
                } else {
                    EMatchAction nextAction = defineAction(nextFootballPlayer, false);
                    List<FootballPlayer> refreshedEligiblePlayers = getEligibleTeamMates(nextAction, allTeamPlayers, nextFootballPlayer);

                    processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 1);

                    resource.setAction(nextAction);
                    resource.setLastPossessioner(resource.getPossessioner());
                    resource.setPossessioner(nextFootballPlayer);

                    return playAction(tc, resource, refreshedEligiblePlayers, allTeamPlayers);
                }
            case SHORT_SHOT:
                tc.sendMessage(resource.getMinute() + "' - " + resource.getPossessioner().getMatchName() + " est en bonne position pour frapper, il tente sa chance...");
                if (!actionSuccess(actionSuccessPercentage)) {
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
                    
                    processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 2);

                    resource.setLastPossessioner(null);
                    resource.setPossessioner(null);

                    return resource;
                }
            case LONG_SHOT:
                tc.sendMessage(resource.getMinute() + "' - " + resource.getPossessioner().getMatchName() + " tente sa chance de loin...");
                if (!actionSuccess(actionSuccessPercentage)) {
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

                    processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 2);

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
            case LONG_PASS:
                if (possessionner.getPost().equals(EFootballPlayerPost.GOALKEEPER)) {
                    eligible.removeAll(eligible.stream().filter(f -> f.getPost().equals(EFootballPlayerPost.DEFENDER) || f.getPost().equals(EFootballPlayerPost.GOALKEEPER)).toList());
                } else if (possessionner.getPost().equals(EFootballPlayerPost.DEFENDER)) {
                    eligible.removeAll(eligible.stream().filter(f -> !f.getPost().equals(EFootballPlayerPost.FORWARD)).toList());
                }
                break;
            default:
                break;
        }

        return eligible;
    }

    public static float calculateActionSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha, EMatchAction action, EFootballPlayerPost receiverPost) {
        float chances = 0;

        switch(action) {
            case SHORT_BACK_PASS:
            case SHORT_PROGRESSIVE_PASS:
                chances = calculatePassSuccessProbability(fp, cha, receiverPost);
                break;
            case LONG_PASS:
                chances = calculateLongPassSuccessProbability(fp, cha, receiverPost);
                break;
            case SHORT_SHOT:
                chances = calculateShotSuccessProbability(fp, cha);
                break;
            case LONG_SHOT:
                chances = calculateLongShotSuccessProbability(fp, cha);
                break;
            default:
                break;
        }

        return chances;
    }

    public static float calculatePassSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha, EFootballPlayerPost receiverPost) {
        float chances = 0;

        int passes = cha.getPasses();

        if (EFootballPlayerPost.GOALKEEPER.equals(fp.getPost())) {
            chances = (float) Math.min((1/passes)+0.02, 0.25);
        } else if (EFootballPlayerPost.DEFENDER.equals(fp.getPost())) {
            if (EFootballPlayerPost.GOALKEEPER.equals(receiverPost) || EFootballPlayerPost.DEFENDER.equals(receiverPost)) {
                chances = (float) Math.min((1/passes)+0.02, 0.25);
            } else if (EFootballPlayerPost.MIDFIELDER.equals(receiverPost)) {
                chances = (float) Math.min((1/passes)+0.05, 0.30);
            }
        } else if (EFootballPlayerPost.MIDFIELDER.equals(fp.getPost())) {
            if (EFootballPlayerPost.DEFENDER.equals(receiverPost) || EFootballPlayerPost.MIDFIELDER.equals(receiverPost)) {
                chances = (float) Math.min((1/passes)+0.1, 0.25);
            } else if (EFootballPlayerPost.FORWARD.equals(receiverPost)) {
                chances = (float) Math.min((1/passes)+0.25, 0.75);
            }
        } else {
            if (EFootballPlayerPost.MIDFIELDER.equals(receiverPost)) {
                chances = (float) Math.min((1/passes)+0.1, 0.25);
            } else if (EFootballPlayerPost.FORWARD.equals(receiverPost)) {
                chances = (float) Math.min((1/passes)+0.25, 0.75);
            }
        }

        chances *= 100;
        chances = 100 - chances;

        System.out.println(fp.getMatchName() + " a " + chances + "% de chances de réussir sa passe");

        return chances;    
    }

    public static float calculateLongPassSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha, EFootballPlayerPost receiverPost) {
        double chances = 0;

        int longPasses = cha.getLongPasses();
        double threshold = 0.075;
        double basis = 0.75;
        EFootballPlayerPost post = fp.getPost();

        if (EFootballPlayerPost.GOALKEEPER.equals(post)) {
            if (EFootballPlayerPost.MIDFIELDER.equals(receiverPost)) {
                basis = 0.3;
            } else if (EFootballPlayerPost.FORWARD.equals(receiverPost)) {
                basis = 0.4;
            }
        } else if (EFootballPlayerPost.DEFENDER.equals(post)) {
            if (EFootballPlayerPost.FORWARD.equals(receiverPost)) {
                basis = 0.3;
            }
        }

        chances = (1 / (1 + Math.exp(-threshold * longPasses))) - basis;

        chances = chances > 0 ? chances : 0;

        chances *= 100;

        System.out.println(fp.getMatchName() + " a " + chances + "% de chances de réussir sa longue passe");

        return (float) chances;
    }

    public static float calculateShotSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha) {
        double chances = 0;
        int shots = cha.getShots();
        double threshold = 0.075;
        double basis = 0.25;

        chances = (1 / (1 + Math.exp(-threshold * shots))) - basis;

        chances *= 100;

        System.out.println(fp.getMatchName() + " a " + chances + "% de chances de réussir son tir");

        return (float) chances;
    }

    public static float calculateLongShotSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha) {
        double chances = 0;
        int longShots = cha.getLongShots();
        double threshold = 0.075;
        double basis = 0.75;
        EFootballPlayerPost post = fp.getPost();

        if (EFootballPlayerPost.MIDFIELDER.equals(post)) {
            basis = 0.6;
        } else if (EFootballPlayerPost.FORWARD.equals(post)) {
            basis = 0.5;
        }

        chances = (1 / (1 + Math.exp(-threshold * longShots))) - basis;

        chances = chances > 0 ? chances : 0;

        chances *= 100;

        System.out.println(fp.getMatchName() + " a " + chances + "% de chances de réussir son tir de loin");

        return (float) chances;
    }

    public static boolean actionSuccess(float probability) {
        Random p = new Random();

        float rand = p.nextFloat();

        System.out.println("rand: " + rand + "\nproba: " + (probability/100));

        return rand < (probability/100);
    }

    public static void processMatchXpTable(Map<Long, Integer> matchXpTable, Long possessionnerId, int earnedXp) {
        matchXpTable.put(possessionnerId, matchXpTable.get(possessionnerId) + earnedXp);
    }
}
