package fr.kemstormy.discord;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import fr.kemstormy.discord.service.DiscordUserService;
import fr.kemstormy.discord.utils.DiscordUtils;

@SpringBootApplication
public class KemstormyApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(KemstormyApplication.class, args);

		DiscordUserService discordUserService = applicationContext.getBean(DiscordUserService.class);

		DiscordUtils utils = new DiscordUtils(discordUserService);
		DiscordApi api = new DiscordApiBuilder()
			.setToken("prank")
			.addIntents(Intent.MESSAGE_CONTENT)
			.login()
			.join();

		api.addMessageCreateListener(event -> {
			if (!event.getMessageAuthor().isBotUser() && event.getMessageContent() != null && event.getMessageContent().length() > 0 && event.getMessageContent().startsWith("!")) {
				String message = event.getMessageContent();
				String messageToSendBack = utils.getCommand(message, event.getMessageAuthor().asUser().orElseThrow());
				event.getChannel().sendMessage(messageToSendBack);
			}
		});
	}

}
