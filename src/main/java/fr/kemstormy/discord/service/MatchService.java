package fr.kemstormy.discord.service;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kemstormy.discord.enums.EFootballPlayerPost;
import fr.kemstormy.discord.enums.EMatchAction;
import fr.kemstormy.discord.enums.EMatchEvent;
import fr.kemstormy.discord.enums.EMatchStatus;
import fr.kemstormy.discord.model.FootballPlayer;
import fr.kemstormy.discord.model.Ladder;
import fr.kemstormy.discord.model.League;
import fr.kemstormy.discord.model.Match;
import fr.kemstormy.discord.model.MatchDecisivePasser;
import fr.kemstormy.discord.model.MatchStriker;
import fr.kemstormy.discord.model.PlayerCharacteristics;
import fr.kemstormy.discord.model.Stadium;
import fr.kemstormy.discord.model.Team;
import fr.kemstormy.discord.model.Week;
import fr.kemstormy.discord.repository.FootballPlayerRepository;
import fr.kemstormy.discord.repository.LadderRepository;
import fr.kemstormy.discord.repository.MatchDecisivePasserRepository;
import fr.kemstormy.discord.repository.MatchRepository;
import fr.kemstormy.discord.repository.MatchStrikerRepository;
import fr.kemstormy.discord.repository.PlayerCharacteristicsRepository;
import fr.kemstormy.discord.repository.StadiumRepository;
import fr.kemstormy.discord.repository.TeamRepository;
import fr.kemstormy.discord.resource.MatchDataResource;
import fr.kemstormy.discord.resource.XpData;

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
        } else if (matchData.getScoreHome() == matchData.getScoreAway()) {
            this.processStadiumXp(homeTeamStadium, 1, channel);
        }

        this.processXpPlayers(matchData.getMatchXpTable(), channel);

        this.matchRepository.save(m);
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
}
