package fr.kemstormy.discord.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kemstormy.discord.enums.EFootballPlayerPost;
import fr.kemstormy.discord.enums.EMatchAction;
import fr.kemstormy.discord.enums.EMatchEvent;
import fr.kemstormy.discord.enums.EMatchStatus;
import fr.kemstormy.discord.model.FootballPlayer;
import fr.kemstormy.discord.model.League;
import fr.kemstormy.discord.model.Match;
import fr.kemstormy.discord.model.Team;
import fr.kemstormy.discord.repository.FootballPlayerRepository;
import fr.kemstormy.discord.repository.MatchRepository;

@Service
public class MatchService {
    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private FootballPlayerRepository footballPlayerRepository;

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

    public void playMatch(TextChannel channel) throws InterruptedException {
        Match m = this.matchRepository.getRandomMatch();
        Random timeRandomOffset = new Random();

        Team homeTeam = m.getHomeTeam();
        Team awayTeam = m.getAwayTeam();

        List<FootballPlayer> startingHome = this.generateComposition(homeTeam);
        List<FootballPlayer> startingAway = this.generateComposition(awayTeam);

        List<FootballPlayer> allPlayers = new ArrayList<>();
        allPlayers.addAll(startingHome);
        allPlayers.addAll(startingAway);

        channel.sendMessage("Début du match entre " + homeTeam.getName() + " et " + awayTeam.getName() + " (" + homeTeam.getStadium().getName() + ") !");
        int scoreHome = 0;
        int scoreAway = 0;

        this.sendCompoEmbed(channel, homeTeam, startingHome);
        TimeUnit.SECONDS.sleep(2);
        this.sendCompoEmbed(channel, awayTeam, startingAway);
        TimeUnit.SECONDS.sleep(2);

        for (int i = 1; i < 95; i+=timeRandomOffset.nextInt(10)+1) {
            Random randomPlayer = new Random();
            FootballPlayer actionPlayer = allPlayers.get(randomPlayer.nextInt(allPlayers.size()));

            EMatchAction matchAction = EMatchAction.processRandomAction(actionPlayer, homeTeam, awayTeam);
            EMatchEvent matchEvent = EMatchAction.playAction(channel, matchAction, actionPlayer, homeTeam, startingHome, awayTeam, startingAway, i, scoreHome, scoreAway);

            switch (matchEvent) {
                case GOAL_HOME:
                    scoreHome++;
                    channel.sendMessage(i + "' - But pour " + homeTeam.getName() + " (" + scoreHome + " - " + scoreAway + ") !");
                    break;
                case GOAL_AWAY:
                    scoreAway++;
                    channel.sendMessage(i + "' - But pour " + awayTeam.getName() + " (" + scoreHome + " - " + scoreAway + ") !");
                default:
                    break;
            }

            TimeUnit.SECONDS.sleep(3);
        }

        if (scoreHome > scoreAway) {
            channel.sendMessage("Fin du match ! Victoire de **" + homeTeam.getName() + "** (" + scoreHome + " - " + scoreAway + ")");
        } else if (scoreHome < scoreAway) {
            channel.sendMessage("Fin du match ! Victoire de **" + awayTeam.getName() + "** (" + scoreHome + " - " + scoreAway + ")");
        } else {
            channel.sendMessage("Fin du match ! Match nul... (" + scoreHome + " - " + scoreAway + ")");
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

    public void sendCompoEmbed(TextChannel tc, Team team, List<FootballPlayer> compo) {
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

        tc.sendMessage(embed);
    }
}
