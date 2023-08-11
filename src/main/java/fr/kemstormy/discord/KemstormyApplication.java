package fr.kemstormy.discord;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
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
import fr.kemstormy.discord.service.LadderService;
import fr.kemstormy.discord.service.LeagueService;
import fr.kemstormy.discord.service.MatchDecisivePassersService;
import fr.kemstormy.discord.service.MatchService;
import fr.kemstormy.discord.service.MatchStrikerService;
import fr.kemstormy.discord.service.PlayerRecordService;
import fr.kemstormy.discord.service.TeamRecordService;
import fr.kemstormy.discord.service.TeamService;
import fr.kemstormy.discord.utils.DiscordUtils;

@SpringBootApplication
@EnableScheduling
public class KemstormyApplication {

	private static ConfigurableApplicationContext applicationContext;

	private static final String TOKEN = "prank";

	private static final List<String> CHANNEL_TOKENS = new ArrayList();

	public static void main(String[] args) throws Exception {
		applicationContext = SpringApplication.run(KemstormyApplication.class, args);

		DiscordUserService discordUserService = applicationContext.getBean(DiscordUserService.class);
		FootballPlayerService footballPlayerService = applicationContext.getBean(FootballPlayerService.class);
		TeamService teamService = applicationContext.getBean(TeamService.class);
		LeagueService leagueService = applicationContext.getBean(LeagueService.class);
		MatchService matchService = applicationContext.getBean(MatchService.class);
		LadderService ladderService = applicationContext.getBean(LadderService.class);
		MatchStrikerService matchStrikerService = applicationContext.getBean(MatchStrikerService.class);
		MatchDecisivePassersService matchDecisivePassersService = applicationContext.getBean(MatchDecisivePassersService.class);
		TeamRecordService teamRecordService = applicationContext.getBean(TeamRecordService.class);
		PlayerRecordService playerRecordService = applicationContext.getBean(PlayerRecordService.class);

		// Adding channel token to list
		CHANNEL_TOKENS.add("1138507381546946611");

		DiscordUtils utils = new DiscordUtils(discordUserService, footballPlayerService, teamService, matchService, leagueService, ladderService, matchStrikerService, matchDecisivePassersService, teamRecordService, playerRecordService);
		DiscordApi api = new DiscordApiBuilder()
			.setToken(TOKEN)
			.addIntents(Intent.MESSAGE_CONTENT)
			.login()
			.join();

		
		api.addMessageCreateListener(event -> {
			// commandes exécutables sur serveur
			if (CHANNEL_TOKENS.contains(event.getChannel().getIdAsString())) {
				if (!event.getMessageAuthor().isBotUser() && event.getMessageContent() != null && event.getMessageContent().length() > 0 && event.getMessageContent().startsWith("!")) {
					String message = event.getMessageContent();
					String messageToSendBack = "";
					try {
						messageToSendBack = utils.getCommand(message, event.getMessageAuthor().asUser().orElseThrow(), event);
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					event.getChannel().sendMessage(messageToSendBack);
				}
			}
			// commandes exécutables en MP
			else if (event.getPrivateChannel().isPresent()) {
				if (!event.getMessageAuthor().isBotUser() && event.getMessageContent() != null && event.getMessageContent().length() > 0 && event.getMessageContent().startsWith("!")) {
					String messageToSendBack = "";

					try {
						messageToSendBack = utils.getPrivateCommand(event, api);
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}

					event.getPrivateChannel().get().sendMessage(messageToSendBack);
					
				}
			}
		});

		
	}

	@Scheduled(fixedDelay = 1000 * 60)
	public void sendEveryMinutes() throws InterruptedException, ExecutionException {
		/*if (applicationContext != null) {
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
		}*/
	}
}
