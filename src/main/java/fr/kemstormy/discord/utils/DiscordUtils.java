package fr.kemstormy.discord.utils;

import java.util.Arrays;
import java.util.List;

import org.javacord.api.entity.user.User;

import fr.kemstormy.discord.enums.EFootballPlayerGenerationType;
import fr.kemstormy.discord.model.DiscordUser;
import fr.kemstormy.discord.model.FootballPlayer;
import fr.kemstormy.discord.service.DiscordUserService;
import fr.kemstormy.discord.service.FootballPlayerService;
import lombok.Data;

@Data
public class DiscordUtils {

    private DiscordUserService discordUserService;
    private FootballPlayerService footballPlayerService;

    public DiscordUtils(DiscordUserService discordUserService, FootballPlayerService footballPlayerService) {
        this.discordUserService = discordUserService;
        this.footballPlayerService = footballPlayerService;
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
            case "players":
                String msg = "";
                List<FootballPlayer> fp = this.footballPlayerService.getAllFootballPlayers();

                msg += "Il y a actuellement " + fp.size() + " joueurs enregistrés.\n";

                for (FootballPlayer f : fp) {
                    msg += f.getId() + ". " + f.getFirstName() + " " + f.getLastName() + " (";

                    if (f.getGenerationType().equals(EFootballPlayerGenerationType.BY_BOT)) {
                        msg += "bot)\n";
                    } else if (f.getGenerationType().equals(EFootballPlayerGenerationType.BY_PLAYER)) {
                        msg += "joueur)\n";
                    }
                }

                return msg;
            default:
                return "Commande inconnue...";
        }
    }

    private List<String> removeCommandDiscriminator(String command) {
        return Arrays.asList(command.substring(1).split(" "));
    }
}
