package fr.kemstormy.discord.utils;

import java.util.Arrays;
import java.util.List;

import org.javacord.api.entity.user.User;

import fr.kemstormy.discord.enums.EFootballPlayerGenerationType;
import fr.kemstormy.discord.model.DiscordUser;
import fr.kemstormy.discord.model.FootballPlayer;
import fr.kemstormy.discord.model.Team;
import fr.kemstormy.discord.service.DiscordUserService;
import fr.kemstormy.discord.service.FootballPlayerService;
import fr.kemstormy.discord.service.TeamService;
import lombok.Data;

@Data
public class DiscordUtils {

    private DiscordUserService discordUserService;
    private FootballPlayerService footballPlayerService;
    private TeamService teamService;

    public DiscordUtils(DiscordUserService discordUserService, FootballPlayerService footballPlayerService, TeamService teamService) {
        this.discordUserService = discordUserService;
        this.footballPlayerService = footballPlayerService;
        this.teamService = teamService;
    }

    public String getCommand(String command, User messageAuthor) {
        List<String> commands = this.removeCommandDiscriminator(command);
        String mainCommand = commands.get(0);
        String msg = "";

        switch (mainCommand) {
            case "register":
                DiscordUser du = new DiscordUser();
                du.setDiscordId(messageAuthor.getIdAsString());

                this.discordUserService.createOrUpdateDiscordUser(du);

                msg = "Compte enregistré :white_check_mark:";
                break;
            case "players":
                msg = "";
                List<FootballPlayer> fp = this.footballPlayerService.getAllFootballPlayers();

                msg += "Il y a actuellement " + fp.size() + " joueurs enregistrés.\n";

                for (FootballPlayer f : fp) {
                    msg += f.getId() + ". " + f.getFirstName() + " " + f.getLastName() + " (";

                    if (f.getGenerationType().equals(EFootballPlayerGenerationType.BY_BOT)) {
                        msg += "bot)\n";
                    } else if (f.getGenerationType().equals(EFootballPlayerGenerationType.BY_PLAYER)) {
                        msg += "joueur: " + f.getOwner().getDiscordId() + ")\n";
                    }
                }

                break;
            case "create":
                if (commands.size() != 3) {
                    msg = "Commande invalide, veuillez réessayer avec `!create prenom nom`";
                    break;
                }
                FootballPlayer createdFootballPlayer = new FootballPlayer();
                DiscordUser discordUser = this.discordUserService.getByDiscordId(messageAuthor.getIdAsString());

                createdFootballPlayer.setAge(18);
                createdFootballPlayer.setFirstName(commands.get(1));
                createdFootballPlayer.setLastName(commands.get(2));
                createdFootballPlayer.setOwner(discordUser);

                this.footballPlayerService.createOrUpdatFootballPlayer(createdFootballPlayer);

                msg = "Le joueur **" + createdFootballPlayer.getFirstName() + " " + createdFootballPlayer.getLastName() + "** a été créé.";

                break;
            case "teams":
                List<Team> teams = this.teamService.getAllTeams();

                for(Team t : teams) {
                    msg += t.getId() + "# " + t.getName() + "\n";
                }

                break;
            default:
                msg = "Commande inconnue...";
                break;
        }
        return msg;
    }

    private List<String> removeCommandDiscriminator(String command) {
        return Arrays.asList(command.substring(1).split(" "));
    }
}
