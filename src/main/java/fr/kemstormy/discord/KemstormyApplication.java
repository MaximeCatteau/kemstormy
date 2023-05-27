package fr.kemstormy.discord;

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
import fr.kemstormy.discord.model.Team;
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
				String messageToSendBack = "";
				try {
					messageToSendBack = utils.getCommand(message, event.getMessageAuthor().asUser().orElseThrow(), event);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
}
