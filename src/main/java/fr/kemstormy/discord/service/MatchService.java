package fr.kemstormy.discord.service;

import java.awt.Color;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kemstormy.discord.enums.EFootballPlayerPost;
import fr.kemstormy.discord.enums.EMatchAction;
import fr.kemstormy.discord.enums.EMatchStatus;
import fr.kemstormy.discord.enums.ERecordType;
import fr.kemstormy.discord.model.FootballPlayer;
import fr.kemstormy.discord.model.Ladder;
import fr.kemstormy.discord.model.League;
import fr.kemstormy.discord.model.Match;
import fr.kemstormy.discord.model.MatchDecisivePasser;
import fr.kemstormy.discord.model.MatchStriker;
import fr.kemstormy.discord.model.PlayerCharacteristics;
import fr.kemstormy.discord.model.PlayerRecord;
import fr.kemstormy.discord.model.Stadium;
import fr.kemstormy.discord.model.Team;
import fr.kemstormy.discord.model.TeamRecord;
import fr.kemstormy.discord.model.Week;
import fr.kemstormy.discord.repository.FootballPlayerRepository;
import fr.kemstormy.discord.repository.LadderRepository;
import fr.kemstormy.discord.repository.MatchDecisivePasserRepository;
import fr.kemstormy.discord.repository.MatchRepository;
import fr.kemstormy.discord.repository.MatchStrikerRepository;
import fr.kemstormy.discord.repository.PlayerCharacteristicsRepository;
import fr.kemstormy.discord.repository.PlayerRecordRepository;
import fr.kemstormy.discord.repository.StadiumRepository;
import fr.kemstormy.discord.repository.TeamRecordRepository;
import fr.kemstormy.discord.repository.TeamRepository;
import fr.kemstormy.discord.resource.MatchDataResource;
import fr.kemstormy.discord.resource.PasserLadderResource;
import fr.kemstormy.discord.resource.StrikerLadderResource;
import fr.kemstormy.discord.resource.XpData;
import jakarta.persistence.Tuple;

@Service
public class MatchService {
    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private FootballPlayerRepository footballPlayerRepository;

    @Autowired 
    private TeamRepository teamRepository;

    @Autowired
    private LadderRepository ladderRepository;

    @Autowired
    private MatchStrikerRepository matchStrikerRepository;

    @Autowired
    private MatchDecisivePasserRepository matchDecisivePasserRepository;

    @Autowired
    private StadiumRepository stadiumRepository;

    @Autowired
    private PlayerCharacteristicsRepository playerCharacteristicsRepository;

    @Autowired
    private TeamRecordRepository teamRecordRepository;

    @Autowired
    private PlayerRecordRepository playerRecordRepository;

    public Match createMatch(Team home, Team away, League league) {
        Match m = new Match();

        m.setHomeTeam(home);
        m.setAwayTeam(away);
        Instant now = Instant.now();
        m.setDate(now);
        m.setScoreHome(0);
        m.setScoreAway(0);
        m.setCompetition(league);
        m.setStatus(EMatchStatus.COMING);

        return this.matchRepository.save(m);
    }

    public void playMatch(TextChannel channel) throws InterruptedException, IOException {
        Match m = this.matchRepository.getNextMatchToPlay();
        Random timeRandomOffset = new Random();
        Random randomPlayer = new Random();

        Team homeTeam = (Team) Hibernate.unproxy(m.getHomeTeam());
        Team awayTeam = (Team) Hibernate.unproxy(m.getAwayTeam());

        List<FootballPlayer> startingHome = this.generateComposition(homeTeam);
        List<FootballPlayer> startingAway = this.generateComposition(awayTeam);
        List<FootballPlayer> scorers = new ArrayList<>();
        List<FootballPlayer> decisivePassers = new ArrayList<>();

        Ladder homeLadder = this.ladderRepository.getByLeagueAndTeam(homeTeam.getLeague().getId(), homeTeam.getId());
        Ladder awayLadder = this.ladderRepository.getByLeagueAndTeam(awayTeam.getLeague().getId(), awayTeam.getId());

        Stadium homeTeamStadium = (Stadium) Hibernate.unproxy(homeTeam.getStadium());

        boolean isLastMatch = this.isLastMatchOfLeague(m.getCompetition());

        m.setStatus(EMatchStatus.IN_PROGRESS);
        this.matchRepository.save(m);

        // Coin toss
        Team possesion = this.coinToss(homeTeam, awayTeam);

        // The football player who has the ball is one of the forwards of possession team
        FootballPlayer possessioner = this.defineKickOffPlayer(possesion.equals(homeTeam) ? startingHome : startingAway);

        // Define the first action to do
        EMatchAction firstAction = EMatchAction.defineAction(possessioner, true);

        List<FootballPlayer> allPlayers = new ArrayList<>();
        allPlayers.addAll(startingHome);
        allPlayers.addAll(startingAway);

        MatchDataResource matchData = new MatchDataResource();

        matchData.setMatch(m);
        matchData.setHomeTeam(homeTeam);
        matchData.setAwayTeam(awayTeam);
        matchData.setScorers(scorers);
        matchData.setPassers(decisivePassers);
        matchData.setMinute(0);
        matchData.setScoreHome(0);
        matchData.setScoreAway(0);
        matchData.setAction(firstAction);
        matchData.setPossessioner(possessioner);
        matchData.setMatchXpTable(this.initializeMatchDataTable(startingHome, startingAway));

        channel.sendMessage("Début du match entre " + homeTeam.getName() + " et " + awayTeam.getName() + " (" + homeTeam.getStadium().getName() + ") !");

        this.sendCompoEmbed(channel, homeTeam, startingHome, true);
        TimeUnit.SECONDS.sleep(2);
        this.sendCompoEmbed(channel, awayTeam, startingAway, false);
        TimeUnit.SECONDS.sleep(2);

        List<FootballPlayer> mates = EMatchAction.getEligibleTeamMates(firstAction, possesion.equals(homeTeam) ? startingHome : startingAway, possessioner);

        channel.sendMessage("0' - " + possessioner.getMatchName() + " donne le coup d'envoi de cette rencontre !");

        matchData = EMatchAction.playAction(channel, matchData, mates, possesion.equals(homeTeam) ? startingHome : startingAway);

        for (int i = 1; i < 90; i+=timeRandomOffset.nextInt(10)+1) {
            FootballPlayer randomPossessionner = allPlayers.get(randomPlayer.nextInt(allPlayers.size()));
            EMatchAction actionToPlay = EMatchAction.defineAction(randomPossessionner, false);
            List<FootballPlayer> teamWithBall = possesion.equals(homeTeam) ? startingHome : startingAway;
            mates = EMatchAction.getEligibleTeamMates(actionToPlay, teamWithBall, possessioner);

            matchData.setPossessioner(randomPossessionner);
            matchData.setAction(actionToPlay);
            matchData.setMinute(i);

            matchData = EMatchAction.playAction(channel, matchData, mates, teamWithBall);

            TimeUnit.SECONDS.sleep(2);
        }

        if (matchData.getScoreHome() > matchData.getScoreAway()) {
            homeLadder.setVictories(homeLadder.getVictories() + 1);
            awayLadder.setLoses(awayLadder.getLoses() + 1);
            channel.sendMessage("Fin du match ! Victoire de **" + homeTeam.getName() + "** (" + matchData.getScoreHome() + " - " + matchData.getScoreAway() + ")");
        } else if (matchData.getScoreHome() < matchData.getScoreAway()) {
            homeLadder.setLoses(homeLadder.getLoses() + 1);
            awayLadder.setVictories(awayLadder.getVictories() + 1);
            channel.sendMessage("Fin du match ! Victoire de **" + awayTeam.getName() + "** (" + matchData.getScoreHome() + " - " + matchData.getScoreAway() + ")");
        } else {
            homeLadder.setDraws(homeLadder.getDraws() + 1);
            awayLadder.setDraws(awayLadder.getDraws() + 1);
            channel.sendMessage("Fin du match ! Match nul... (" + matchData.getScoreHome() + " - " + matchData.getScoreAway() + ")");
        }

        homeLadder.setScoredGoals(homeLadder.getScoredGoals() + matchData.getScoreHome());
        homeLadder.setConcededGoals(homeLadder.getConcededGoals() + matchData.getScoreAway());
        awayLadder.setScoredGoals(awayLadder.getScoredGoals() + matchData.getScoreAway());
        awayLadder.setConcededGoals(awayLadder.getConcededGoals() + matchData.getScoreHome());

        this.ladderRepository.save(homeLadder);
        this.ladderRepository.save(awayLadder);

        m.setStatus(EMatchStatus.COMPLETED);
        m.setScoreHome(matchData.getScoreHome());
        m.setScoreAway(matchData.getScoreAway());

        List<MatchStriker> strikersToAdd = new ArrayList<>();
        List<MatchDecisivePasser> decisivePassersToAdd = new ArrayList<>();

        for (FootballPlayer f : matchData.getScorers()) {
            MatchStriker ms = new MatchStriker();
            ms.setFootballPlayer(f);
            ms.setMatch(m);
            strikersToAdd.add(ms);
        }

        this.matchStrikerRepository.saveAll(strikersToAdd);

        for (FootballPlayer f : matchData.getPassers()) {
            MatchDecisivePasser mdp = new MatchDecisivePasser();
            mdp.setFootballPlayer(f);
            mdp.setMatch(m);
            decisivePassersToAdd.add(mdp);
        }

        this.matchDecisivePasserRepository.saveAll(decisivePassersToAdd);

        System.out.println("Fin du match");

        if (matchData.getScoreHome() > matchData.getScoreAway()) {
            this.processStadiumXp(homeTeamStadium, 3, channel);
            homeTeam.setBudget(homeTeam.getBudget() + 3);
        } else if (matchData.getScoreHome() == matchData.getScoreAway()) {
            this.processStadiumXp(homeTeamStadium, 1, channel);
            homeTeam.setBudget(homeTeam.getBudget() + 1);
            awayTeam.setBudget(homeTeam.getBudget() + 2);
        } else {
            awayTeam.setBudget(homeTeam.getBudget() + 4);
        }

        this.processXpPlayers(matchData.getMatchXpTable(), channel);
        this.teamRepository.save(homeTeam);
        this.teamRepository.save(awayTeam);

        this.matchRepository.save(m);

        if (isLastMatch) {
            this.distributeRewards(channel, m.getCompetition().getId());
        }
    }

    public List<FootballPlayer> generateComposition(Team team) {
        List<FootballPlayer> compo = new ArrayList<>();

        compo.add(this.footballPlayerRepository.getRandomGoalkeeper(team.getId()));
        compo.addAll(this.footballPlayerRepository.getRandomDefenders(team.getId(), 4));
        compo.addAll(this.footballPlayerRepository.getRandomMidfielders(team.getId(), 4));
        compo.addAll(this.footballPlayerRepository.getRandomAttackers(team.getId(), 2));

        return compo;
    }

    public void sendCompoEmbed(TextChannel tc, Team team, List<FootballPlayer> compo, boolean isHomeTeam) {
        EmbedBuilder embed = new EmbedBuilder();

        List<FootballPlayer> goalkeepers = compo.stream().filter(fp -> fp.getPost().equals(EFootballPlayerPost.GOALKEEPER)).toList();
        List<FootballPlayer> defenders = compo.stream().filter(fp -> fp.getPost().equals(EFootballPlayerPost.DEFENDER)).toList();
        List<FootballPlayer> midfielders = compo.stream().filter(fp -> fp.getPost().equals(EFootballPlayerPost.MIDFIELDER)).toList();
        List<FootballPlayer> forwards = compo.stream().filter(fp -> fp.getPost().equals(EFootballPlayerPost.FORWARD)).toList();

        embed.setTitle("Composition de départ - " + team.getName());

        embed.addField("Gardien de but", goalkeepers.get(0).getFirstName() + " " + goalkeepers.get(0).getLastName());
        embed.addField("Défenseurs", defenders.stream().map(fp -> fp.getFirstName() + " " + fp.getLastName()).collect(Collectors.joining("\n")));
        embed.addField("Milieux", midfielders.stream().map(fp -> fp.getFirstName() + " " + fp.getLastName()).collect(Collectors.joining("\n")));
        embed.addField("Attaquants", forwards.stream().map(fp -> fp.getFirstName() + " " + fp.getLastName()).collect(Collectors.joining("\n")));

        embed.setThumbnail(team.getLogo());

        if (isHomeTeam) {
            embed.setThumbnail(team.getHomeJersey());
            embed.setColor(this.convertHexToColor(team.getMainColor()));
        } else {
            embed.setThumbnail(team.getAwayJersey());
            embed.setColor(this.convertHexToColor(team.getSecondaryColor()));
        }

        tc.sendMessage(embed);
    }

    private Team coinToss(Team home, Team away) {
        Random r = new Random();
        return r.nextBoolean() ? home : away;
    }

    private FootballPlayer defineKickOffPlayer(List<FootballPlayer> teamPlayers) {
        Random r = new Random();
        List<FootballPlayer> forwards = teamPlayers.stream().filter(fp -> fp.getPost().equals(EFootballPlayerPost.FORWARD)).toList();

        return forwards.get(r.nextInt(forwards.size()));
    }

    public void championshipScheduling(League league) throws InterruptedException {
		List<Team> teams = this.teamRepository.getLeagueTeams(league.getId());

		int totalDays = teams.size() - 1;
		int halfSize = teams.size() /2;

		List<Team> copyTeams = new ArrayList(teams);
		copyTeams.remove(teams.get(0));

		List<Week> weeks = new ArrayList<>();

		for (int day = 0; day < totalDays; day++) {
			Week week = new Week();
			List<Match> weekMatchs = new ArrayList<>();
			week.setNumber(day + 1);

			int teamIdx = day % copyTeams.size();

			Match match = new Match();
			match.setHomeTeam(copyTeams.get(teamIdx));
			match.setAwayTeam(teams.get(0));
            match.setScoreHome(0);
            match.setScoreAway(0);
            match.setCompetition(league);
            match.setStatus(EMatchStatus.COMING);

			weekMatchs.add(match);

			for (int idx = 1; idx < halfSize; idx++) {
				Match m = new Match();
				int firstTeam = (day + idx) % copyTeams.size();
				int secondTeam = (day + copyTeams.size() - idx) % copyTeams.size();

				m.setHomeTeam(copyTeams.get(firstTeam));
				m.setAwayTeam(copyTeams.get(secondTeam));
                m.setScoreHome(0);
                m.setScoreAway(0);
                m.setCompetition(league);
                m.setStatus(EMatchStatus.COMING);

				weekMatchs.add(m);
			}

			week.setMatchs(weekMatchs);
			weeks.add(week);
		}

		for (int day = 0; day < totalDays; day++) {
			Week week = new Week();
			List<Match> weekMatchs = new ArrayList<>();
			week.setNumber(copyTeams.size() +day + 1);

			int teamIdx = day % copyTeams.size();

			Match match = new Match();
			match.setHomeTeam(teams.get(0));
			match.setAwayTeam(copyTeams.get(teamIdx));
            match.setScoreHome(0);
            match.setScoreAway(0);
            match.setCompetition(league);
            match.setStatus(EMatchStatus.COMING);

			weekMatchs.add(match);

			for (int idx = 1; idx < halfSize; idx++) {
				Match m = new Match();
				int firstTeam = (day + copyTeams.size() - idx) % copyTeams.size();
				int secondTeam = (day + idx) % copyTeams.size();

				m.setHomeTeam(copyTeams.get(firstTeam));
				m.setAwayTeam(copyTeams.get(secondTeam));
                m.setScoreHome(0);
                m.setScoreAway(0);
                m.setCompetition(league);
                m.setStatus(EMatchStatus.COMING);

				weekMatchs.add(m);
			}

			week.setMatchs(weekMatchs);
			weeks.add(week);
		}
	
		
		Collections.shuffle(weeks);
		
		for (Week w : weeks) {
			Instant today = Instant.now();

			int minHour = 14;

			Instant matchDay = today.plus(weeks.indexOf(w), ChronoUnit.DAYS);

			for (Match m : w.getMatchs()) {
				Instant matchTime = matchDay.atZone(ZoneOffset.UTC).withHour((minHour + (w.getMatchs().indexOf(m) * 1))).withMinute(0).withSecond(0).withNano(0).toInstant();
                m.setDate(matchTime);
                this.matchRepository.save(m);
			}
		}
	}

    private void processStadiumXp(Stadium stadium, int experienceEarned, TextChannel tc) throws IOException {
        int currentLvl = stadium.getLevel();
        int experience = stadium.getExperience();
        XpData xpData = new XpData();

        if (xpData.willStadiumUp(stadium, experienceEarned)) {
            tc.sendMessage("Le stade **" + stadium.getName() + "** passe au niveau " + (currentLvl+1) + ".");

            stadium.setLevel(currentLvl + 1);
            stadium.setCapacity(stadium.getCapacity() + 1000);
        }

        experience += experienceEarned;

        stadium.setExperience(experience);

        this.stadiumRepository.save(stadium);
    }

    private void processXpPlayers(Map<Long, Integer> xpDataTable, TextChannel channel) {
        List<FootballPlayer> players = new ArrayList<>();
        XpData xpData = new XpData();

        for (var entry : xpDataTable.entrySet()) {
            if (entry.getValue() > 0) {
                FootballPlayer fp = this.footballPlayerRepository.findById(entry.getKey()).orElseThrow();
                PlayerCharacteristics pc = fp.getPlayerCharacteristics();

                int currentLvl = fp.getLevel();
                int experience = pc.getExperience();

                try {
                    if (xpData.willPlayerUp(fp, experience)) {
                        channel.sendMessage("**" + fp.getFirstName() + " " + fp.getLastName() + "** passe au niveau " + (currentLvl + 1));
                        fp.setPointsToSet(fp.getPointsToSet() + 1);
                        fp.setLevel(currentLvl + 1);
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                experience += entry.getValue();

                pc.setExperience(experience);

                this.playerCharacteristicsRepository.save(pc);

                players.add(fp);
            }
        }

        this.footballPlayerRepository.saveAll(players);
    }

    private Map<Long, Integer> initializeMatchDataTable(List<FootballPlayer> startingHome, List<FootballPlayer> startingAway) {
        Map<Long, Integer> map = new HashMap<>();

        for (FootballPlayer hFP : startingHome) {
            map.put(hFP.getId(), 0);
        }

        for (FootballPlayer aFP : startingAway) {
            map.put(aFP.getId(), 0);
        }

        return map;
    }

    private Color convertHexToColor(String hex) {
        int r = Integer.valueOf(hex.substring(1, 3), 16);
        int g = Integer.valueOf(hex.substring(3, 5), 16);
        int b = Integer.valueOf(hex.substring(5, 7), 16);
        Color color = new Color(r, g, b);

        return color;
    }

    private boolean isLastMatchOfLeague(League league) {
        int remainingMatches = this.matchRepository.countRemainingLeagueMatchs(league.getId());

        return remainingMatches == 1;
    }

    private void distributeRewards(TextChannel channel, Long leagueId) {
        this.distributeTeamRewards(channel, leagueId);

        this.distributePlayersRewards(channel, leagueId);
    }

    private void distributeTeamRewards(TextChannel channel, Long leagueId) {
        Ladder ladder = this.ladderRepository.getChampionOfLeague(leagueId);
        Ladder ladderLast = this.ladderRepository.getLastOfLeague(leagueId);

        Team t = ladder.getTeam();
        Team lastTeam = ladderLast.getTeam();

        League league = t.getLeague();

        // Distribute for team
        TeamRecord teamRecord = new TeamRecord();

        teamRecord.setLeague(league);
        teamRecord.setTeam(t);
        teamRecord.setLabel("Champion de " + league.getName());

        this.teamRecordRepository.save(teamRecord);

        // Distribute for each team player
        List<PlayerRecord> playerRecords = new ArrayList<>();
        List<FootballPlayer> teamPlayers = this.footballPlayerRepository.findByTeam(t.getId());

        for (FootballPlayer fp : teamPlayers) {
            PlayerRecord pr = new PlayerRecord();

            pr.setFootballPlayer(fp);
            pr.setLabel("Champion de " + league.getName());
            pr.setLeague(league);
            pr.setRecordType(ERecordType.TEAM);

            playerRecords.add(pr);
        }

        this.playerRecordRepository.saveAll(playerRecords);

        channel.sendMessage("Le club **" + t.getName() + "** est sacré champion de **" + league.getName() + "** ! Félicitations !");
        channel.sendMessage("Le club **" + lastTeam.getName() + "** est malheureusement relégué en **" + league.getLowerLeague().getName() + "** ! Plus de chance la saison prochaine !");
    }

    private void distributePlayersRewards(TextChannel channel, Long leagueId) {
        // Best striker
        this.distributeBestStrikerReward(channel, leagueId);

        // Best passer
        this.distributeBestPasserReward(channel, leagueId);

        // Best goalkeeper
        this.distributeBestGoalkeeperReward(channel, leagueId);
    }

    private void distributeBestStrikerReward(TextChannel channel, Long leagueId) {
        Tuple strikerTuple = this.matchStrikerRepository.getBestStriker(leagueId);
        Set<Entry<String, Object>> entry = strikerTuple.getElements().stream().map(e -> new HashMap.SimpleEntry<String, Object>(e.getAlias(), strikerTuple.get(e))).collect(Collectors.toSet());
        StrikerLadderResource res = new StrikerLadderResource();

        Iterator it = entry.iterator();
        while (it.hasNext()) {
            Map.Entry ent = (Map.Entry) it.next();

            if (((String) ent.getKey()).equals("scored_goals")) {
                res.setScoredGoals((Long) ent.getValue());
            } else if (((String) ent.getKey()).equals("first_name")) {
                res.setFirstName((String) ent.getValue());
            } else if (((String) ent.getKey()).equals("last_name")) {
                res.setLastName((String) ent.getValue());
            } else if (((String) ent.getKey()).equals("team_name")) {
                res.setTeamName((String) ent.getValue());
            }
        }

        FootballPlayer footballPlayer = this.footballPlayerRepository.findByFirstNameAndLastName(res.getFirstName(), res.getLastName());
        League league = footballPlayer.getClub().getLeague();

        PlayerRecord pr = new PlayerRecord();

        pr.setFootballPlayer(footballPlayer);
        pr.setLeague(league);
        pr.setRecordType(ERecordType.SOLO);
        pr.setLabel("Meilleur buteur de " + league.getName());

        this.playerRecordRepository.save(pr);

        channel.sendMessage("**" + footballPlayer.getFirstName() + " " + footballPlayer.getLastName() + "** est sacré meilleur buteur de **" + league.getName() + "** avec **" + res.getScoredGoals() + " réalisations** ! Félicitations !");
    }

    private void distributeBestPasserReward(TextChannel channel, Long leagueId) {
        Tuple passerTuple = this.matchDecisivePasserRepository.getBestPasserOfLeague(leagueId);
        Set<Entry<String, Object>> entry = passerTuple.getElements().stream().map(e -> new HashMap.SimpleEntry<String, Object>(e.getAlias(), passerTuple.get(e))).collect(Collectors.toSet());
        PasserLadderResource res = new PasserLadderResource();

        Iterator it = entry.iterator();
        while (it.hasNext()) {
            Map.Entry ent = (Map.Entry) it.next();

            if (((String) ent.getKey()).equals("assists")) {
                res.setAssists((Long) ent.getValue());
            } else if (((String) ent.getKey()).equals("first_name")) {
                res.setFirstName((String) ent.getValue());
            } else if (((String) ent.getKey()).equals("last_name")) {
                res.setLastName((String) ent.getValue());
            } else if (((String) ent.getKey()).equals("team_name")) {
                res.setTeamName((String) ent.getValue());
            }
        }

        FootballPlayer footballPlayer = this.footballPlayerRepository.findByFirstNameAndLastName(res.getFirstName(), res.getLastName());
        League league = footballPlayer.getClub().getLeague();

        PlayerRecord pr = new PlayerRecord();

        pr.setFootballPlayer(footballPlayer);
        pr.setLeague(league);
        pr.setRecordType(ERecordType.SOLO);
        pr.setLabel("Meilleur passeur de " + league.getName());

        this.playerRecordRepository.save(pr);

        channel.sendMessage("**" + footballPlayer.getFirstName() + " " + footballPlayer.getLastName() + "** est sacré meilleur passeur de **" + league.getName() + "** avec **" + res.getAssists() + " passes décisives** ! Félicitations !");
    }

    private void distributeBestGoalkeeperReward(TextChannel channel, Long leagueId) {
        Ladder bestDefence = this.ladderRepository.getBestDefence(leagueId);
        Team team = bestDefence.getTeam();
        League league = team.getLeague();

        List<FootballPlayer> players = this.footballPlayerRepository.getFootballPlayersByTeamAndPost(team.getId(), EFootballPlayerPost.GOALKEEPER.ordinal());

        FootballPlayer p = players.get(0);

        PlayerRecord pr = new PlayerRecord();

        pr.setFootballPlayer(p);
        pr.setLeague(league);
        pr.setRecordType(ERecordType.SOLO);
        pr.setLabel("Meilleur gardien de " + league.getName());

        this.playerRecordRepository.save(pr);

        channel.sendMessage("**" + p.getFirstName() + " " + p.getLastName() + "** est sacré meilleur gardien de **" + league.getName() + "** avec seulement **" + bestDefence.getConcededGoals() + " buts encaissés** ! Félicitations !");
    }
}
