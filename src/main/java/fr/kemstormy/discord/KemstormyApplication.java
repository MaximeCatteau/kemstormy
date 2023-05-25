package fr.kemstormy.discord;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.intent.Intent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import fr.kemstormy.discord.model.FootballPlayer;
import fr.kemstormy.discord.model.Match;
import fr.kemstormy.discord.model.Team;
import fr.kemstormy.discord.model.Week;
import fr.kemstormy.discord.service.DiscordUserService;
import fr.kemstormy.discord.service.FootballPlayerService;
import fr.kemstormy.discord.service.LeagueService;
import fr.kemstormy.discord.service.MatchService;
import fr.kemstormy.discord.service.TeamService;
import fr.kemstormy.discord.utils.DiscordUtils;

@SpringBootApplication
@EnableScheduling
public class KemstormyApplication {

	private static ConfigurableApplicationContext applicationContext;

	private static final String TOKEN = "prank";

	public static void main(String[] args) {
		applicationContext = SpringApplication.run(KemstormyApplication.class, args);

		DiscordUserService discordUserService = applicationContext.getBean(DiscordUserService.class);
		FootballPlayerService footballPlayerService = applicationContext.getBean(FootballPlayerService.class);
		TeamService teamService = applicationContext.getBean(TeamService.class);
		LeagueService leagueService = applicationContext.getBean(LeagueService.class);
		MatchService matchService = applicationContext.getBean(MatchService.class);

		DiscordUtils utils = new DiscordUtils(discordUserService, footballPlayerService, teamService, matchService, leagueService);
		DiscordApi api = new DiscordApiBuilder()
			.setToken(TOKEN)
			.addIntents(Intent.MESSAGE_CONTENT)
			.login()
			.join();

		//championshipScheduling(teamService);

		api.addMessageCreateListener(event -> {
			if (!event.getMessageAuthor().isBotUser() && event.getMessageContent() != null && event.getMessageContent().length() > 0 && event.getMessageContent().startsWith("!")) {
				String message = event.getMessageContent();
				String messageToSendBack = utils.getCommand(message, event.getMessageAuthor().asUser().orElseThrow(), event);
				event.getChannel().sendMessage(messageToSendBack);
			}
		});
	}

	@Scheduled(fixedDelay = 1000 * 60)
	public void sendEveryMinutes() throws InterruptedException {
		if (applicationContext != null) {
			FootballPlayerService footballPlayerService = applicationContext.getBean(FootballPlayerService.class);
			TeamService teamService = applicationContext.getBean(TeamService.class);

			FootballPlayer random = footballPlayerService.getRandomFootballPlayerWithNoTeam();

			if (random == null) {
				return;
			}

			Team recruiter = teamService.handleRecruitment(random);

			random.setClub(recruiter);
			footballPlayerService.createOrUpdateFootballPlayer(random);

			DiscordApi api = new DiscordApiBuilder()
				.setToken(TOKEN)
				.addIntents(Intent.MESSAGE_CONTENT)
				.login()
				.join();

			TextChannel tc = api.getTextChannelById("185791467732729856").orElseThrow();
			tc.sendMessage("TRANSFERT - " + random.getFirstName() + " " + random.getLastName() + " a rejoint " + recruiter.getName() + " !");
		}
	}

	public static void championshipScheduling(TeamService teamService) {
		List<Team> teams = teamService.getAllTeams();

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

			weekMatchs.add(match);

			for (int idx = 1; idx < halfSize; idx++) {
				Match m = new Match();
				int firstTeam = (day + idx) % copyTeams.size();
				int secondTeam = (day + copyTeams.size() - idx) % copyTeams.size();

				m.setHomeTeam(copyTeams.get(firstTeam));
				m.setAwayTeam(copyTeams.get(secondTeam));

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

			weekMatchs.add(match);

			for (int idx = 1; idx < halfSize; idx++) {
				Match m = new Match();
				int firstTeam = (day + copyTeams.size() - idx) % copyTeams.size();
				int secondTeam = (day + idx) % copyTeams.size();

				m.setHomeTeam(copyTeams.get(firstTeam));
				m.setAwayTeam(copyTeams.get(secondTeam));

				weekMatchs.add(m);
			}

			week.setMatchs(weekMatchs);
			weeks.add(week);
		}
	
		
		Collections.shuffle(weeks);
		
		for (Week w : weeks) {
			System.out.println("Semaine " + (weeks.indexOf(w)+1));
			Instant today = Instant.now();

			int minHour = 14;

			Instant matchDay = today.plus(weeks.indexOf(w), ChronoUnit.DAYS);

			for (Match m : w.getMatchs()) {
				DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.of("Europe/Paris"));
				Instant matchTime = matchDay.atZone(ZoneOffset.UTC).withHour((minHour + (w.getMatchs().indexOf(m) * 1))).withMinute(0).withSecond(0).withNano(0).toInstant();
				System.out.println(m.toString() + " " + formatter.format(matchTime));
			}
			System.out.println("----------------------------");
		}
	}
}
