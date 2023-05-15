package fr.kemstormy.discord.utils;

import java.util.Arrays;
import java.util.List;

import org.javacord.api.entity.user.User;

import fr.kemstormy.discord.model.DiscordUser;
import fr.kemstormy.discord.service.DiscordUserService;
import lombok.Data;

@Data
public class DiscordUtils {

    private DiscordUserService discordUserService;

    public DiscordUtils(DiscordUserService discordUserService) {
        this.discordUserService = discordUserService;
    }

    public String getCommand(String command, User messageAuthor) {
        List<String> commands = this.removeCommandDiscriminator(command);
        String mainCommand = commands.get(0);

        switch (mainCommand) {
            case "register":
                DiscordUser du = new DiscordUser();
                du.setDiscordId(messageAuthor.getIdAsString());

                this.discordUserService.createOrUpdateDiscordUser(du);

                return "Compte enregistré :white_check_mark:";
            default:
                return "Commande inconnue...";
        }
    }

    private List<String> removeCommandDiscriminator(String command) {
        return Arrays.asList(command.substring(1).split(" "));
    }
}
