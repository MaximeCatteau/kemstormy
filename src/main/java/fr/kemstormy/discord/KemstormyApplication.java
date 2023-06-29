package fr.kemstormy.discord;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import org.checkerframework.common.reflection.qual.GetClass;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import fr.kemstormy.discord.enums.EFootballPlayerGenerationType;
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

	public static void main(String[] args) throws Exception {
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

		api.addMessageCreateListener(event -> {
			if (!event.getMessageAuthor().isBotUser() && event.getMessageContent() != null && event.getMessageContent().length() > 0 && event.getMessageContent().startsWith("!")) {
				String message = event.getMessageContent();
				String messageToSendBack = "";
				try {
					messageToSendBack = utils.getCommand(message, event.getMessageAuthor().asUser().orElseThrow(), event);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				event.getChannel().sendMessage(messageToSendBack);
			}
		});
	}

	@Scheduled(fixedDelay = 1000 * 60)
	public void sendEveryMinutes() throws InterruptedException, ExecutionException {
		if (applicationContext != null) {
			FootballPlayerService footballPlayerService = applicationContext.getBean(FootballPlayerService.class);
			TeamService teamService = applicationContext.getBean(TeamService.class);

			FootballPlayer random = footballPlayerService.getRandomFootballPlayerWithNoTeam();

			if (random == null) {
				return;
			}

			Team recruiter = teamService.handleRecruitment(random);

			DiscordApi api = new DiscordApiBuilder()
				.setToken(TOKEN)
				.addIntents(Intent.MESSAGE_CONTENT)
				.login()
				.join();

			TextChannel tc = api.getTextChannelById("185791467732729856").orElseThrow();

			if (EFootballPlayerGenerationType.BY_PLAYER.equals(random.getGenerationType())) {
				Message message = tc.sendMessage("<@" + random.getOwner().getDiscordId() + "> PROPOSITION DE TRANSFERT\n" + recruiter.getName() + " propose  à " + random.getMatchName() + " de rejoindre l'équipe, acceptez-vous ?").get();

				message.addReaction("✅");
				message.addReaction("❌");
				
				message.addReactionAddListener(react -> {
					User user = null;
					try {
						user = api.getUserById(react.getUserIdAsString()).get();
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (!user.isBot() && user.getIdAsString().equals(random.getOwner().getDiscordId()) && react.getEmoji().equalsEmoji("✅")) {
						random.setClub(recruiter);
						footballPlayerService.createOrUpdateFootballPlayer(random);
						tc.sendMessage("TRANSFERT - " + random.getFirstName() + " " + random.getLastName() + " a rejoint " + recruiter.getName() + " !");
						react.deleteMessage();
					} else if (!user.isBot() && user.getIdAsString().equals(random.getOwner().getDiscordId()) && react.getEmoji().equalsEmoji("❌")) {
						tc.sendMessage(random.getFirstName() + " " + random.getLastName() + " a refusé l'offre de **" + recruiter.getName() + "**.");
						react.deleteMessage();
					}
				}).removeAfter(10, TimeUnit.MINUTES);
			} else {
				random.setClub(recruiter);
				footballPlayerService.createOrUpdateFootballPlayer(random);

				tc.sendMessage("TRANSFERT - " + random.getFirstName() + " " + random.getLastName() + " a rejoint " + recruiter.getName() + " !");
			}
		}
	}
}
