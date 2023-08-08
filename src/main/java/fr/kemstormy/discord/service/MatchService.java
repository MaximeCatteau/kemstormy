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

import fr.kemstormy.discord.enums.EFootballPlayerGenerationType;
import fr.kemstormy.discord.enums.EFootballPlayerPost;
import fr.kemstormy.discord.enums.EMatchAction;
import fr.kemstormy.discord.enums.EMatchEvent;
import fr.kemstormy.discord.enums.EMatchStatus;
import fr.kemstormy.discord.enums.EPlayerCharacteristic;
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

        // Coin toss
        Team possesion = this.coinToss(homeTeam, awayTeam);

        // The football player who has the ball is one of the forwards of possession team
        FootballPlayer possessioner = this.defineKickOffPlayer(possesion.equals(homeTeam) ? startingHome : startingAway);

        m.setStatus(EMatchStatus.IN_PROGRESS);

        // Define the first action to do
        EMatchAction firstAction = this.defineAction(possessioner, true, null);

        List<FootballPlayer> allPlayers = new ArrayList<>();
        allPlayers.addAll(startingHome);
        allPlayers.addAll(startingAway);

        /***
         * Pre-kickoff requirements :
         * - Home team - OK
         * - Away team - OK
         * - Kick off team - OK
         * - Possessionner - OK
         * - Stadium - OK
         * - Starting XI home/away - OK
         * - The match - OK
         * - The first action of the match
         */

        // Initialize match data
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

        channel.sendMessage("0' - " + possessioner.getMatchName() + " " + possessioner.getNationality().getFlag() + " donne le coup d'envoi de cette rencontre !");

        matchData = this.playAction(channel, matchData, possesion.equals(homeTeam) ? startingHome : startingAway, !possesion.equals(homeTeam) ? startingHome : startingAway);

        for (int i = 1; i < 90; i+=timeRandomOffset.nextInt(10)+1) {
            FootballPlayer randomPossessionner = allPlayers.get(randomPlayer.nextInt(allPlayers.size()));
            EMatchAction actionToPlay = this.defineAction(randomPossessionner, false, null);
            List<FootballPlayer> teamWithBall = possesion.equals(homeTeam) ? startingHome : startingAway;
            List<FootballPlayer> teamWithNoBall = !possesion.equals(homeTeam) ? startingHome : startingAway;

            matchData.setPossessioner(randomPossessionner);
            matchData.setAction(actionToPlay);
            matchData.setMinute(i);

            matchData = this.playAction(channel, matchData, teamWithBall, teamWithNoBall);

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
        compo.addAll(this.footballPlayerRepository.getRandomDefenders(team.getId(), team.getQuotaDefenders()));
        compo.addAll(this.footballPlayerRepository.getRandomMidfielders(team.getId(), team.getQuotaMidfielders()));
        compo.addAll(this.footballPlayerRepository.getRandomAttackers(team.getId(), team.getQuotaForwards()));

        return compo;
    }

    public void sendCompoEmbed(TextChannel tc, Team team, List<FootballPlayer> compo, boolean isHomeTeam) {
        EmbedBuilder embed = new EmbedBuilder();

        List<FootballPlayer> goalkeepers = compo.stream().filter(fp -> fp.getPost().equals(EFootballPlayerPost.GOALKEEPER)).toList();
        List<FootballPlayer> defenders = compo.stream().filter(fp -> fp.getPost().equals(EFootballPlayerPost.DEFENDER)).toList();
        List<FootballPlayer> midfielders = compo.stream().filter(fp -> fp.getPost().equals(EFootballPlayerPost.MIDFIELDER)).toList();
        List<FootballPlayer> forwards = compo.stream().filter(fp -> fp.getPost().equals(EFootballPlayerPost.FORWARD)).toList();

        embed.setTitle("Composition de départ - " + team.getName());

        embed.addField("Gardien de but", goalkeepers.get(0).getFirstName() + " " + goalkeepers.get(0).getLastName() + " " + goalkeepers.get(0).getNationality().getFlag());
        embed.addField("Défenseurs", defenders.stream().map(fp -> fp.getFirstName() + " " + fp.getLastName() + " " + fp.getNationality().getFlag()).collect(Collectors.joining("\n")));
        embed.addField("Milieux", midfielders.stream().map(fp -> fp.getFirstName() + " " + fp.getLastName() + " " + fp.getNationality().getFlag()).collect(Collectors.joining("\n")));
        embed.addField("Attaquants", forwards.stream().map(fp -> fp.getFirstName() + " " + fp.getLastName() + " " + fp.getNationality().getFlag()).collect(Collectors.joining("\n")));

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

        int idx = 1;
		
		for (Week w : weeks) {
			Instant today = Instant.now();

			int minHour = 10;

			Instant matchDay = today.plus(weeks.indexOf(w), ChronoUnit.DAYS);

			for (Match m : w.getMatchs()) {
				Instant matchTime = matchDay.atZone(ZoneOffset.UTC).withHour((minHour + (w.getMatchs().indexOf(m) * 1))).withMinute(0).withSecond(0).withNano(0).toInstant();
                m.setDate(matchTime);
                m.setDay(idx);
                this.matchRepository.save(m);
			}

            idx++;
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
                PlayerCharacteristics pc = (PlayerCharacteristics) Hibernate.unproxy(fp.getPlayerCharacteristics());

                int currentLvl = fp.getLevel();
                int earnedXp = entry.getValue();

                try {
                    if (xpData.willPlayerUp(fp.getLevel(), pc.getExperience(), earnedXp)) {
                        channel.sendMessage("**" + fp.getFirstName() + " " + fp.getLastName() + "** passe au niveau " + (currentLvl + 1));
                        fp.setPointsToSet(fp.getPointsToSet() + 5);
                        fp.setLevel(currentLvl + 1);

                        if (EFootballPlayerGenerationType.BY_BOT.equals(fp.getGenerationType())) {
                            pc = this.processAutoUpgrade(fp, pc);
                            fp.setPointsToSet(0);
                        }
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                pc.setExperience(pc.getExperience() + earnedXp);

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

    private EMatchAction defineAction(FootballPlayer possessionner, boolean isKickoff, EMatchAction previousAction) {
        Random randomAction = new Random();

        List<EMatchAction> possibleActions = this.getPossibleActions(possessionner, isKickoff, previousAction, null, null);
        return possibleActions.get(randomAction.nextInt(possibleActions.size()));
    }

    private List<EMatchAction> getPossibleActions(FootballPlayer possessionner, boolean isKickoff, EMatchAction previousAction, FootballPlayer previousPlayer, EMatchEvent event) {
        List<EMatchAction> possibleActions = new ArrayList<>();
        Team playerTeam = (Team) Hibernate.unproxy(possessionner.getClub());

        switch(possessionner.getPost()) {
            case GOALKEEPER:
                possibleActions.add(EMatchAction.CLEARANCE);
                possibleActions.add(EMatchAction.SHORT_PROGRESSIVE_PASS);
                possibleActions.add(EMatchAction.LONG_PASS);
                break;
            case DEFENDER:
                possibleActions.add(EMatchAction.SHORT_BACK_PASS);
                possibleActions.add(EMatchAction.LONG_PASS);
                possibleActions.add(EMatchAction.SHORT_PROGRESSIVE_PASS);
                possibleActions.add(EMatchAction.LONG_SHOT);
                possibleActions.add(EMatchAction.DRIBBLE);

                break;
            case MIDFIELDER:
                possibleActions.add(EMatchAction.SHORT_BACK_PASS);
                possibleActions.add(EMatchAction.SHORT_PROGRESSIVE_PASS);
                possibleActions.add(EMatchAction.LONG_SHOT);
                possibleActions.add(EMatchAction.DRIBBLE);
                break;
            case FORWARD:
                if (playerTeam.getQuotaForwards() > 1) {
                    possibleActions.add(EMatchAction.SHORT_PROGRESSIVE_PASS);
                }

                possibleActions.add(EMatchAction.SHORT_BACK_PASS);
                possibleActions.add(EMatchAction.DRIBBLE);

                if (isKickoff) {
                    break;
                }

                possibleActions.add(EMatchAction.SHORT_SHOT);

                break;
            default:
                break;
        }

        if (EMatchAction.DRIBBLE.equals(previousAction)) {
            possibleActions.remove(EMatchAction.DRIBBLE);
        }

        return possibleActions;
    }

    private List<FootballPlayer> getEligibleTeamMates(EMatchAction previousAction, List<FootballPlayer> team, FootballPlayer possessionner) {
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
            case CORNER:
                eligible.removeAll(eligible.stream().filter(f -> f.getPost().equals(EFootballPlayerPost.GOALKEEPER)).toList());
            default:
                break;
        }

        return eligible;
    }

    private MatchDataResource playAction(TextChannel tc, MatchDataResource resource, List<FootballPlayer> allTeamPlayers, List<FootballPlayer> opponentPlayers) {
        String msg = "";
        Random randomPlayer = new Random();
        EMatchEvent matchEvent = EMatchEvent.NOTHING;
        FootballPlayer nextFootballPlayer;
        Team playerClub = (Team) Hibernate.unproxy(resource.getPossessioner().getClub());
        PlayerCharacteristics pc = (PlayerCharacteristics) Hibernate.unproxy(resource.getPossessioner().getPlayerCharacteristics());
        List<FootballPlayer> eligiblePlayers = this.getEligibleTeamMates(resource.getAction(), allTeamPlayers, resource.getPossessioner());

        // Definir le joueur destinataire
        if (eligiblePlayers != null && eligiblePlayers.size() > 0) {
            nextFootballPlayer = eligiblePlayers.get(randomPlayer.nextInt(eligiblePlayers.size()));
        } else {
            nextFootballPlayer = null;
        }

        float actionSuccessPercentage = this.calculateActionSuccessProbability(resource.getPossessioner(), pc, resource.getAction(), nextFootballPlayer);

        switch(resource.getAction()) {
            case SHORT_PROGRESSIVE_PASS:
                tc.sendMessage(resource.getMinute() + "' - " + resource.getPossessioner().getMatchName() + " " + resource.getPossessioner().getNationality().getFlag() + " effectue une passe vers " + nextFootballPlayer.getMatchName() + " " + nextFootballPlayer.getNationality().getFlag());
                if (!this.actionSuccess(actionSuccessPercentage, resource.getPercentMultiplicator())) {
                    // Intercepteurs potentiels
                    List<FootballPlayer> potentialInterceptors = new ArrayList<>();
                    FootballPlayer interceptor;

                    potentialInterceptors = this.getPotentialInterceptors(opponentPlayers, resource.getPossessioner().getPost(), null);
                    interceptor = potentialInterceptors.get(randomPlayer.nextInt(potentialInterceptors.size()));
                    this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 1);

                    // Ballon non intercepté
                    if (!this.actionSuccess(this.calculateInterceptionSuccessProbability(interceptor, interceptor.getPlayerCharacteristics()), 0)) {
                        tc.sendMessage("Le " + resource.getPossessioner().getNationality().getGentilé() + " " + resource.getPossessioner().getNationality().getFlag() + " rate sa passe, le ballon est perdu...");
                        matchEvent = EMatchEvent.NOTHING;
                        resource.setMatchEvent(matchEvent);
                        resource.setLastPossessioner(null);
                        resource.setPercentMultiplicator(0);
                        break;
                    }
                    // Ballon intercepté
                    tc.sendMessage(interceptor.getMatchName() + " " + interceptor.getNationality().getFlag() + " intercepte le ballon !");
                    EMatchAction nextAction = this.defineAction(interceptor, false, resource.getAction());
                    
                    this.processMatchXpTable(resource.getMatchXpTable(), interceptor.getId(), 2);

                    resource.setAction(nextAction);
                    resource.setLastPossessioner(resource.getPossessioner());
                    resource.setPossessioner(interceptor);
                    resource.setPercentMultiplicator(0);

                    return this.playAction(tc, resource, opponentPlayers, allTeamPlayers);
                } else {
                    EMatchAction nextAction = this.defineAction(nextFootballPlayer, false, resource.getAction());

                    this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 3);

                    resource.setAction(nextAction);
                    resource.setLastPossessioner(resource.getPossessioner());
                    resource.setPossessioner(nextFootballPlayer);
                    resource.setPercentMultiplicator(0);

                    return this.playAction(tc, resource, allTeamPlayers, opponentPlayers);
                }
            case SHORT_BACK_PASS:
                tc.sendMessage(resource.getMinute() + "' - " + resource.getPossessioner().getMatchName() + " " + resource.getPossessioner().getNationality().getFlag() + " effectue une passe en retrait vers " + nextFootballPlayer.getMatchName() + ".");
                if (!this.actionSuccess(actionSuccessPercentage, resource.getPercentMultiplicator())) {
                    // Intercepteurs potentiels
                    List<FootballPlayer> potentialInterceptors = new ArrayList<>();
                    FootballPlayer interceptor;

                    potentialInterceptors = this.getPotentialInterceptors(opponentPlayers, resource.getPossessioner().getPost(), null);
                    interceptor = potentialInterceptors.get(randomPlayer.nextInt(potentialInterceptors.size()));
                    this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 1);

                    // Ballon non intercepté
                    if (!this.actionSuccess(this.calculateInterceptionSuccessProbability(interceptor, interceptor.getPlayerCharacteristics()), 0)) {
                        tc.sendMessage("Le " + resource.getPossessioner().getNationality().getGentilé() + " " + resource.getPossessioner().getNationality().getFlag() + " rate sa passe, le ballon est perdu...");
                        matchEvent = EMatchEvent.NOTHING;
                        resource.setMatchEvent(matchEvent);
                        resource.setLastPossessioner(null);
                        resource.setPercentMultiplicator(0);
                        break;
                    }
                    // Ballon intercepté
                    tc.sendMessage(interceptor.getMatchName() + " " + interceptor.getNationality().getFlag() + " intercepte le ballon !");
                    EMatchAction nextAction = this.defineAction(interceptor, false, resource.getAction());
                    
                    this.processMatchXpTable(resource.getMatchXpTable(), interceptor.getId(), 2);

                    resource.setAction(nextAction);
                    resource.setLastPossessioner(resource.getPossessioner());
                    resource.setPossessioner(interceptor);
                    resource.setPercentMultiplicator(0);

                    return this.playAction(tc, resource, opponentPlayers, allTeamPlayers);
                } else {
                    EMatchAction nextAction = this.defineAction(nextFootballPlayer, false, resource.getAction());
                    
                    this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 2);

                    resource.setAction(nextAction);
                    resource.setLastPossessioner(resource.getPossessioner());
                    resource.setPossessioner(nextFootballPlayer);
                    resource.setPercentMultiplicator(0);

                    return this.playAction(tc, resource, allTeamPlayers, opponentPlayers);
                }
            case LONG_PASS:
                tc.sendMessage(resource.getMinute() + "' - " + resource.getPossessioner().getMatchName() + " " + resource.getPossessioner().getNationality().getFlag() + " tente une longue passe vers " + nextFootballPlayer.getMatchName() + ".");
                if (!this.actionSuccess(actionSuccessPercentage, resource.getPercentMultiplicator())) {
                    // Intercepteurs potentiels
                    List<FootballPlayer> potentialInterceptors = new ArrayList<>();
                    FootballPlayer interceptor;

                    potentialInterceptors = this.getPotentialInterceptors(opponentPlayers, resource.getPossessioner().getPost(), null);
                    interceptor = potentialInterceptors.get(randomPlayer.nextInt(potentialInterceptors.size()));
                    this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 1);

                    // Ballon non intercepté
                    if (!this.actionSuccess(this.calculateInterceptionSuccessProbability(interceptor, interceptor.getPlayerCharacteristics()), 0)) {
                        tc.sendMessage("Le joueur " + resource.getPossessioner().getNationality().getGentilé() + " " + resource.getPossessioner().getNationality().getFlag() + " rate sa passe, le ballon est perdu...");
                        matchEvent = EMatchEvent.NOTHING;
                        resource.setMatchEvent(matchEvent);
                        resource.setLastPossessioner(null);
                        resource.setPercentMultiplicator(0);
                        break;
                    }
                    // Ballon intercepté
                    tc.sendMessage(interceptor.getMatchName() + " intercepte le ballon !");
                    EMatchAction nextAction = this.defineAction(interceptor, false, resource.getAction());
                    
                    this.processMatchXpTable(resource.getMatchXpTable(), interceptor.getId(), 2);

                    resource.setAction(nextAction);
                    resource.setLastPossessioner(resource.getPossessioner());
                    resource.setPossessioner(interceptor);
                    resource.setPercentMultiplicator(0);

                    return this.playAction(tc, resource, opponentPlayers, allTeamPlayers);
                } else {
                    EMatchAction nextAction = defineAction(nextFootballPlayer, false, resource.getAction());

                    this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 5);

                    resource.setAction(nextAction);
                    resource.setLastPossessioner(resource.getPossessioner());
                    resource.setPossessioner(nextFootballPlayer);

                    return this.playAction(tc, resource, allTeamPlayers, opponentPlayers);
                }
            case SHORT_SHOT:
                tc.sendMessage(resource.getMinute() + "' - " + resource.getPossessioner().getMatchName() + " est en bonne position pour frapper, il tente sa chance...");
                if (!this.actionSuccess(actionSuccessPercentage, resource.getPercentMultiplicator())) {
                    tc.sendMessage("... et c'est raté, le ballon est perdu... :x:");
                    matchEvent = EMatchEvent.NOTHING;
                    this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 1);
                    resource.setLastPossessioner(null);
                    resource.setPossessioner(null);
                    resource.setPercentMultiplicator(0);
                    break;
                } else {
                    // Définir le gardien adverse
                    FootballPlayer opponentGoalkeeper = this.getGoalkeeper(opponentPlayers);

                    // Définir si le gardien fait 1 arrêt
                    if (!actionSuccess(this.calculateSaveSuccessProbability(opponentGoalkeeper, opponentGoalkeeper.getPlayerCharacteristics()), 0)) {
                         if (playerClub.getName().equals(resource.getHomeTeam().getName())) {
                            resource.setScoreHome(resource.getScoreHome()+1);
                        } else {
                            resource.setScoreAway(resource.getScoreAway()+1);
                        }

                        if (resource.getLastPossessioner() != null) {
                            resource.getPassers().add(resource.getLastPossessioner());
                        }

                        resource.getScorers().add(resource.getPossessioner());

                        tc.sendMessage(":boom: GOAAAAAAAAAAAAAAAAL ! But marqué par " + resource.getPossessioner().getFirstName() + " " + resource.getPossessioner().getLastName() + " " + resource.getPossessioner().getNationality().getFlag() + " (" + resource.getScoreHome() + " - " + resource.getScoreAway() + ")");
                        
                        this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 5);

                        resource.setLastPossessioner(null);
                        resource.setPossessioner(null);
                        resource.setPercentMultiplicator(0);

                        return resource;
                    } else {
                        if (!actionSuccess(this.calculateShotStoppingSuccessProbability(opponentGoalkeeper, opponentGoalkeeper.getPlayerCharacteristics()), 0)) {
                            this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 1);
                            // Shot détourné
                            // Si détourné : corner
                            tc.sendMessage("... mais le tir est détourné en corner par le gardien adverse ! :x:");
                            this.processMatchXpTable(resource.getMatchXpTable(), opponentGoalkeeper.getId(), 2);

                            resource.setAction(EMatchAction.CORNER);
                            resource.setLastPossessioner(resource.getPossessioner());
                            resource.setPossessioner(this.determineBestCornerKicker(allTeamPlayers));
                            resource.setPercentMultiplicator(0);

                            return this.playAction(tc, resource, allTeamPlayers, opponentPlayers);
                        } else {
                            // Shot stoppé
                            // Si stoppé : récupération pour l'équipe adverse
                            tc.sendMessage("C'était cadré mais " + opponentGoalkeeper.getMatchName() + " " + opponentGoalkeeper.getNationality().getFlag() + " était sur la trajectoire et capte le ballon :x:");
                            this.processMatchXpTable(resource.getMatchXpTable(), opponentGoalkeeper.getId(), 5);
                            
                            resource.setAction(this.defineAction(opponentGoalkeeper, false, resource.getAction()));
                            resource.setLastPossessioner(resource.getPossessioner());
                            resource.setPossessioner(opponentGoalkeeper);
                            resource.setPercentMultiplicator(0);

                            return this.playAction(tc, resource, opponentPlayers, allTeamPlayers);
                        }
                    }
                }
            case LONG_SHOT:
                tc.sendMessage(resource.getMinute() + "' - " + resource.getPossessioner().getMatchName() + " " + resource.getPossessioner().getNationality().getFlag() + " tente sa chance de loin...");
                if (!this.actionSuccess(actionSuccessPercentage, resource.getPercentMultiplicator())) {
                    tc.sendMessage("... et c'est raté. Il n'avait presque aucune chance de marquer à cette distance... :x:");
                    this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 1);
                    matchEvent = EMatchEvent.NOTHING;
                    resource.setMatchEvent(matchEvent);
                    resource.setLastPossessioner(null);
                    resource.setPossessioner(null);
                    resource.setPercentMultiplicator(0);
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

                    this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 10);

                    tc.sendMessage(":rocket: OH LE BOMBAZOOOOOOOOOOOOOOOOO ! Quel but splendide marqué par " + resource.getPossessioner().getFirstName() + " " + resource.getPossessioner().getNationality().getFlag() + " " + resource.getPossessioner().getLastName() + " (" + resource.getScoreHome() + " - " + resource.getScoreAway() + ")");
                    
                    resource.setLastPossessioner(null);
                    resource.setPossessioner(null);
                    resource.setPercentMultiplicator(0);

                    return resource;
                }
            case CORNER:
                tc.sendMessage(resource.getMinute() + "' - Corner pour " + resource.getPossessioner().getClub().getName() + " frappé par " + resource.getPossessioner().getMatchName() + " " + resource.getPossessioner().getNationality().getFlag() + "...");

                if (!this.actionSuccess(this.calculateCornerSuccessProbability(resource.getPossessioner(), resource.getPossessioner().getPlayerCharacteristics()), resource.getPercentMultiplicator())) {
                    // interception par n'importe qui si raté
                    // Intercepteurs potentiels
                    List<FootballPlayer> potentialInterceptors = new ArrayList<>();
                    FootballPlayer interceptor;

                    potentialInterceptors = this.getPotentialInterceptors(opponentPlayers, resource.getPossessioner().getPost(), EMatchAction.CORNER);
                    interceptor = potentialInterceptors.get(randomPlayer.nextInt(potentialInterceptors.size()));

                    if (!this.actionSuccess(this.calculateInterceptionSuccessProbability(interceptor, interceptor.getPlayerCharacteristics()), resource.getPercentMultiplicator())) {
                        tc.sendMessage("Le corner est raté...");
                        this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 1);
                        matchEvent = EMatchEvent.NOTHING;
                        resource.setMatchEvent(matchEvent);
                        resource.setLastPossessioner(null);
                        resource.setPercentMultiplicator(0);
                        break;
                    } else {
                        // Ballon intercepté
                        tc.sendMessage(interceptor.getMatchName() + " " + interceptor.getNationality().getFlag() + " intercepte le ballon !");
                        EMatchAction nextAction = this.defineAction(interceptor, false, resource.getAction());
                        this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 2);
                        this.processMatchXpTable(resource.getMatchXpTable(), interceptor.getId(), 2);

                        resource.setAction(nextAction);
                        resource.setLastPossessioner(resource.getPossessioner());
                        resource.setPossessioner(interceptor);

                         return this.playAction(tc, resource, opponentPlayers, allTeamPlayers);
                    }
                } 

                // Corner réussi : frappe courte
                tc.sendMessage("... mais le tir est détourné en corner par le gardien adverse ! :x:");
                this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 5);

                resource.setAction(EMatchAction.SHORT_SHOT);
                resource.setLastPossessioner(resource.getPossessioner());
                resource.setPossessioner(nextFootballPlayer);
                resource.setPercentMultiplicator(0);

                return this.playAction(tc, resource, allTeamPlayers, opponentPlayers);
            case DRIBBLE:
                tc.sendMessage("[ DEBUG ] Dribble tenté par : " + resource.getPossessioner().getMatchName() + " (" + pc.getDribbles() +" en dribbles)");
                
                if (!this.actionSuccess(actionSuccessPercentage, resource.getPercentMultiplicator())) {
                    tc.sendMessage("[ DEBUG ] Dribble raté");
                    this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 1);
                    matchEvent = EMatchEvent.NOTHING;
                    resource.setAction(EMatchAction.END_OF_ACTION);
                    resource.setMatchEvent(matchEvent);
                    resource.setLastPossessioner(null);
                    resource.setPossessioner(null);
                    resource.setPercentMultiplicator(0);

                    return resource;
                } else {
                    tc.sendMessage("[ DEBUG ] Dribble réussi");

                    // Intercepteurs potentiels
                    List<FootballPlayer> potentialTacklers = new ArrayList<>();
                    FootballPlayer potentialTackler;

                    potentialTacklers = this.getPotentialTacklers(opponentPlayers, resource.getPossessioner().getPost(), null);


                    EMatchAction nextAction = this.defineAction(resource.getPossessioner(), false, resource.getAction());

                    potentialTackler = potentialTacklers.get(randomPlayer.nextInt(potentialTacklers.size()));
                    PlayerCharacteristics tacklerCharacteristics = (PlayerCharacteristics) Hibernate.unproxy(potentialTackler.getPlayerCharacteristics());

                    // Tacle raté
                    if (!this.actionSuccess(this.calculateTackleSuccessProbability(potentialTackler, tacklerCharacteristics), 0)) {
                        // Faute ou coup critique
                        if (!this.actionSuccess(this.calculateFoulProbability(1.0f, 2.0f), 0)) {
                            // Coup critique
                            System.out.println("[ DEBUG ] Coup critique dribble");
                            this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 5);
                            resource.setPercentMultiplicator(15);

                            resource.setAction(nextAction);
                            resource.setLastPossessioner(resource.getPossessioner());

                            return this.playAction(tc, resource, allTeamPlayers, opponentPlayers);
                        } else {
                            System.out.println("[ DEBUG ] Faute");
                            resource.setMatchEvent(EMatchEvent.FOUL);

                            // Action suivante : coup franc direct ou indirect
                            nextAction = this.determineDirectOrIndirectFreekick();

                            System.out.println("[ DEBUG ] Définition du meilleur tireur de coup franc");
                            // Meilleur tireur de coup franc
                            nextFootballPlayer = this.determineBestFreekicker(allTeamPlayers);

                            resource.setLastPossessioner(null);
                            resource.setPossessioner(nextFootballPlayer);
                            resource.setAction(nextAction);

                            return this.playAction(tc, resource, allTeamPlayers, opponentPlayers);
                        }
                    } else {
                        // tacle réussi
                        tc.sendMessage(potentialTackler.getMatchName() + " " + potentialTackler.getNationality().getFlag() + " réalise un tacle sublime dans les pieds de l'adversaire !");
                        nextAction = this.defineAction(potentialTackler, false, resource.getAction());
                        
                        this.processMatchXpTable(resource.getMatchXpTable(), potentialTackler.getId(), 2);

                        resource.setAction(nextAction);
                        resource.setLastPossessioner(resource.getPossessioner());
                        resource.setPossessioner(potentialTackler);
                        resource.setPercentMultiplicator(0);

                        return this.playAction(tc, resource, opponentPlayers, allTeamPlayers);
                    }
                }
            case DIRECT_FREEKICK:
                System.out.println("[ DEBUG ] Coup franc direct tiré par " + resource.getPossessioner().getMatchName());
                tc.sendMessage(resource.getMinute() + "' - Coup franc pour " + resource.getHomeTeam().getName() + " tiré par " + resource.getPossessioner().getMatchName() + ". Il s'élance pour frapper au but...");

                // calcul
                if (!this.actionSuccess(actionSuccessPercentage, 0)) {
                    // raté
                    tc.sendMessage("... et ça passera à côté, c'est très mal tiré !");
                    this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 1);
                    matchEvent = EMatchEvent.NOTHING;
                    resource.setMatchEvent(matchEvent);
                    resource.setAction(EMatchAction.END_OF_ACTION);
                    resource.setLastPossessioner(null);
                    resource.setPossessioner(null);
                    resource.setPercentMultiplicator(0);

                    return resource;
                } else {
                    // réussi
                    // Définir le gardien adverse
                    FootballPlayer opponentGoalkeeper = this.getGoalkeeper(opponentPlayers);

                    // Définir si le gardien fait 1 arrêt
                    if (!actionSuccess(this.calculateSaveSuccessProbability(opponentGoalkeeper, opponentGoalkeeper.getPlayerCharacteristics()), 0)) {
                         if (playerClub.getName().equals(resource.getHomeTeam().getName())) {
                            resource.setScoreHome(resource.getScoreHome()+1);
                        } else {
                            resource.setScoreAway(resource.getScoreAway()+1);
                        }

                        if (resource.getLastPossessioner() != null) {
                            resource.getPassers().add(resource.getLastPossessioner());
                        }

                        resource.getScorers().add(resource.getPossessioner());

                        tc.sendMessage(":boom: GOAAAAAAAAAAAAAAAAL ! Quel but marqué par " + resource.getPossessioner().getFirstName() + " " + resource.getPossessioner().getLastName() + " " + resource.getPossessioner().getNationality().getFlag() + " (" + resource.getScoreHome() + " - " + resource.getScoreAway() + ")");
                        
                        this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 5);

                        resource.setLastPossessioner(null);
                        resource.setPossessioner(null);
                        resource.setPercentMultiplicator(0);

                        return resource;
                    } else {
                        if (!actionSuccess(this.calculateShotStoppingSuccessProbability(opponentGoalkeeper, opponentGoalkeeper.getPlayerCharacteristics()), 0)) {
                            this.processMatchXpTable(resource.getMatchXpTable(), resource.getPossessioner().getId(), 1);
                            // Shot détourné
                            // Si détourné : corner
                            tc.sendMessage("... OH C'était cadré ! Mais le gardien détourne la frappe en corner ! :x:");
                            this.processMatchXpTable(resource.getMatchXpTable(), opponentGoalkeeper.getId(), 2);

                            resource.setAction(EMatchAction.CORNER);
                            resource.setLastPossessioner(resource.getPossessioner());
                            resource.setPossessioner(this.determineBestCornerKicker(allTeamPlayers));
                            resource.setPercentMultiplicator(0);

                            return this.playAction(tc, resource, allTeamPlayers, opponentPlayers);
                        } else {
                            // Shot stoppé
                            // Si stoppé : récupération pour l'équipe adverse
                            tc.sendMessage("... mais c'est trop moi pour inquiéter " + opponentGoalkeeper.getMatchName() + " " + opponentGoalkeeper.getNationality().getFlag() + " était sur la trajectoire et peut se saisir tranquillement du ballon :x:");
                            this.processMatchXpTable(resource.getMatchXpTable(), opponentGoalkeeper.getId(), 5);
                            
                            resource.setAction(this.defineAction(opponentGoalkeeper, false, resource.getAction()));
                            resource.setLastPossessioner(resource.getPossessioner());
                            resource.setPossessioner(opponentGoalkeeper);
                            resource.setPercentMultiplicator(0);

                            return this.playAction(tc, resource, opponentPlayers, allTeamPlayers);
                        }
                    }
                }
            case INDIRECT_FREEKICK:
                System.out.println("[ DEBUG ] Coup franc indirect tiré par " + resource.getPossessioner().getMatchName());
                resource.setMatchEvent(EMatchEvent.NOTHING);
                resource.setAction(EMatchAction.END_OF_ACTION);

                resource.setLastPossessioner(null);
                resource.setPossessioner(null); 

                return resource;
            default:
                resource.setMatchEvent(EMatchEvent.NOTHING);

                resource.setLastPossessioner(null);
                resource.setPossessioner(null); 

                return resource;
        }

        tc.sendMessage(msg);

        return resource;
    }

    private float calculateActionSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha, EMatchAction action, FootballPlayer receiver) {
        float chances = 0;

        switch(action) {
            case SHORT_BACK_PASS:
            case SHORT_PROGRESSIVE_PASS:
                if (receiver == null) {
                    break;
                }
                chances = this.calculatePassSuccessProbability(fp, cha, receiver.getPost());
                break;
            case LONG_PASS:
            if (receiver == null) {
                    break;
                }
                chances = this.calculateLongPassSuccessProbability(fp, cha, receiver.getPost());
                break;
            case SHORT_SHOT:
                chances = this.calculateShotSuccessProbability(fp, cha);
                break;
            case LONG_SHOT:
                chances = this.calculateLongShotSuccessProbability(fp, cha);
                break;
            case DRIBBLE:
                chances = this.calculateDribbleSuccessProbability(fp, cha);
                break;
            default:
                break;
        }

        return chances;
    }

    private float calculatePassSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha, EFootballPlayerPost receiverPost) {
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

        return chances;    
    }

    private float calculateLongPassSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha, EFootballPlayerPost receiverPost) {
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

        return (float) chances;
    }

    private float calculateShotSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha) {
        double chances = 0;
        int shots = cha.getShots();
        double threshold = 0.075;
        double basis = 0.25;

        chances = (1 / (1 + Math.exp(-threshold * shots))) - basis;

        chances *= 100;

        return (float) chances;
    }

    private float calculateLongShotSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha) {
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

        return (float) chances;
    }

    private float calculateDribbleSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha) {
        double chances = 0;
        int dribble = cha.getDribbles();
        double threshold = 0.075;
        double basis = 0.75;

        basis = 0.3;

        chances = (1 / (1 + Math.exp(-threshold * dribble))) - basis;

        chances = chances > 0 ? chances : 0;

        chances *= 100;

        return (float) chances;
    }

    private float calculateInterceptionSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha) {
        double chances = 0;

        int interceptions = cha.getInterceptions();
        double threshold = 0.075;
        double basis = 0.75;
        EFootballPlayerPost post = fp.getPost();

        if (EFootballPlayerPost.DEFENDER.equals(post) || EFootballPlayerPost.MIDFIELDER.equals(post)) {
            basis = 0.4;
        } else if (EFootballPlayerPost.FORWARD.equals(post)) {
            basis = 0.3;
        }

        chances = (1 / (1 + Math.exp(-threshold * interceptions))) - basis;

        chances = chances > 0 ? chances : 0;

        chances *= 100;

        return (float) chances;
    }

    private float calculateSaveSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha) {
        float chances = 0;

        int reflexes = cha.getReflexes();

        chances = (float) Math.min((1/reflexes)+0.1, 0.25);

        chances *= 100;
        chances = 100 - chances;

        return chances; 
    }

    private float calculateShotStoppingSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha) {
        float chances = 0;

        int shotStopping = cha.getShotStopping();

        chances = (float) Math.min((1/shotStopping)+0.1, 0.25);

        chances *= 100;
        chances = 100 - chances;

        return chances; 
    }

    private float calculateCornerSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha) {
        float chances = 0;

        int corners = cha.getCorners();

        chances = (float) Math.min((1/corners)+0.1, 0.30);

        chances *= 100;
        chances = 100 - chances;

        return chances; 
    }

    private float calculateTackleSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha) {
        double chances = 0;
        int tackle = cha.getTackles();
        double threshold = 0.075;
        double basis = 0.75;

        basis = 0.3;

        chances = (1 / (1 + Math.exp(-threshold * tackle))) - basis;

        chances = chances > 0 ? chances : 0;

        chances *= 100;

        return (float) chances;
    }

    private float calculateFoulProbability(float divider, float denominator) {
        float ratio = divider/denominator;
        
        double chances = ratio * 100;

        return (float) chances;
    }

    private float calculateFreekickSuccessProbability(FootballPlayer fp, PlayerCharacteristics cha) {
        double chances = 0;
        int freekicks = cha.getFreekicks();
        double threshold = 0.075;
        double basis = 0.75;

        chances = (1 / (1 + Math.exp(-threshold * freekicks))) - basis;

        chances = chances > 0 ? chances : 0;

        chances *= 100;

        return (float) chances;
    }

    private boolean actionSuccess(float probability, int boostPercentage) {
        Random p = new Random();
        float rand = p.nextFloat();

        float extendedProbability = probability + (float) boostPercentage;

        return rand < (extendedProbability/100);
    }

    private void processMatchXpTable(Map<Long, Integer> matchXpTable, Long possessionnerId, int earnedXp) {
        matchXpTable.put(possessionnerId, matchXpTable.get(possessionnerId) + earnedXp);
    }

    private FootballPlayer getGoalkeeper(List<FootballPlayer> teamPlayers) {
        return teamPlayers.stream().filter(fp -> EFootballPlayerPost.GOALKEEPER.equals(fp.getPost())).findAny().orElseThrow();
    }

    private List<FootballPlayer> getPotentialInterceptors(List<FootballPlayer> team, EFootballPlayerPost opponentReceiverPost, EMatchAction lastAction) {
        List<FootballPlayer> potentials = new ArrayList<>();

        if (EMatchAction.CORNER.equals(lastAction)) {
            return team;
        }

        switch (opponentReceiverPost) {
            case GOALKEEPER:
            case DEFENDER:
                potentials = team.stream().filter(fp -> EFootballPlayerPost.FORWARD.equals(fp.getPost())).toList();
                break;
            case MIDFIELDER:
                potentials = team.stream().filter(fp -> EFootballPlayerPost.MIDFIELDER.equals(fp.getPost())).toList();
                break;
            case FORWARD:
                potentials = team.stream().filter(fp -> EFootballPlayerPost.DEFENDER.equals(fp.getPost())).toList();
                break;
        }

        return potentials;
    }

    private List<FootballPlayer> getPotentialTacklers(List<FootballPlayer> team, EFootballPlayerPost opponentReceiverPost, EMatchAction lastAction) {
        List<FootballPlayer> potentials = new ArrayList<>();

        if (EMatchAction.CORNER.equals(lastAction)) {
            return team;
        }

        switch (opponentReceiverPost) {
            case DEFENDER:
                potentials = team.stream().filter(fp -> EFootballPlayerPost.FORWARD.equals(fp.getPost())).toList();
                break;
            case MIDFIELDER:
                potentials = team.stream().filter(fp -> EFootballPlayerPost.MIDFIELDER.equals(fp.getPost())).toList();
                break;
            case FORWARD:
                potentials = team.stream().filter(fp -> EFootballPlayerPost.DEFENDER.equals(fp.getPost())).toList();
                break;
        }

        return potentials;
    }

    private FootballPlayer determineBestCornerKicker(List<FootballPlayer> teamPlayers) {
        Collections.shuffle(teamPlayers);

        FootballPlayer bestCornerKicker = teamPlayers.get(0);

        for (FootballPlayer fp : teamPlayers) {
            int bestCornerKickerStat = bestCornerKicker.getPlayerCharacteristics().getCorners();
            int fpCornerStats = fp.getPlayerCharacteristics().getCorners();

            if (fpCornerStats > bestCornerKickerStat) {
                bestCornerKicker = fp;
            }
        }

        return bestCornerKicker;
    }

    private FootballPlayer determineBestFreekicker(List<FootballPlayer> teamPlayers) {
        Collections.shuffle(teamPlayers);

        FootballPlayer bestFreekicker = teamPlayers.get(0);

        for (FootballPlayer fp : teamPlayers) {
            PlayerCharacteristics pcBest = (PlayerCharacteristics) Hibernate.unproxy(bestFreekicker.getPlayerCharacteristics());
            PlayerCharacteristics currentPc = (PlayerCharacteristics) Hibernate.unproxy(fp.getPlayerCharacteristics());
            int bestFreekickerStats = pcBest.getFreekicks();
            int fpFreekickStats = currentPc.getFreekicks();

            if (fpFreekickStats > bestFreekickerStats) {
                bestFreekicker = fp;
            }
        }

        return bestFreekicker;
    }

    private PlayerCharacteristics processAutoUpgrade(FootballPlayer fp, PlayerCharacteristics pc) {
        for (int i = 0; i < fp.getPointsToSet(); i++) {
            EPlayerCharacteristic random = EPlayerCharacteristic.randomPlayerCharacteristic();

            switch (random) {
                case PASS:
                    pc.setPasses(pc.getPasses()+1);
                    break;
                case LONG_PASS:
                    pc.setLongPasses(pc.getLongPasses()+1);
                    break;
                case SHOT:
                    pc.setShots(pc.getShots()+1);
                    break;
                case LONG_SHOT:
                    pc.setLongShots(pc.getLongShots()+1);
                    break;
                case SHOT_STOPPING:
                    pc.setShotStopping(pc.getShotStopping()+1);
                    break;
                case REFLEXES:
                    pc.setReflexes(pc.getReflexes()+1);
                    break;
                case PENALTY:
                    pc.setPenalty(pc.getPenalty()+1);
                    break;
                case FREEKICK:
                    pc.setFreekicks(pc.getFreekicks()+1);
                    break;
                case CORNERS:
                    pc.setCorners(pc.getCorners()+1);
                    break;
                case DRIBBLE:
                    pc.setDribbles(pc.getDribbles()+1);
                    break;
                case INTERCEPTION:
                    pc.setInterceptions(pc.getInterceptions()+1);
                    break;
                case TACKLE:
                    pc.setTackles(pc.getTackles()+1);
                    break;
            }
        }


        return pc;
    }

    private EMatchAction determineDirectOrIndirectFreekick() {
        Random r = new Random();

        return r.nextBoolean() ? EMatchAction.DIRECT_FREEKICK : EMatchAction.INDIRECT_FREEKICK;
    }
}
