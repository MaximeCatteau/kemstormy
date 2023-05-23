package fr.kemstormy.discord.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import fr.kemstormy.discord.enums.EFootballPlayerGenerationType;
import fr.kemstormy.discord.enums.EFootballPlayerPost;
import fr.kemstormy.discord.model.DiscordUser;
import fr.kemstormy.discord.model.FootballPlayer;
import fr.kemstormy.discord.model.Team;
import fr.kemstormy.discord.service.DiscordUserService;
import fr.kemstormy.discord.service.FootballPlayerService;
import fr.kemstormy.discord.service.TeamService;
import lombok.Data;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

@Data
public class DiscordUtils {

    private DiscordUserService discordUserService;
    private FootballPlayerService footballPlayerService;
    private TeamService teamService;

    public DiscordUtils(@Lazy DiscordUserService discordUserService, @Lazy FootballPlayerService footballPlayerService, @Lazy TeamService teamService) {
        this.discordUserService = discordUserService;
        this.footballPlayerService = footballPlayerService;
        this.teamService = teamService;
    }

    public String getCommand(String command, User messageAuthor, MessageCreateEvent event) {
        List<String> commands = this.removeCommandDiscriminator(command);
        String mainCommand = commands.get(0);
        String msg = "";
        TextChannel channel = event.getChannel();

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

                this.footballPlayerService.createOrUpdateFootballPlayer(createdFootballPlayer);

                msg = "Le joueur **" + createdFootballPlayer.getFirstName() + " " + createdFootballPlayer.getLastName() + "** a été créé.";

                break;
            case "teams":
                List<Team> teams = this.teamService.getAllTeams();

                for(Team t : teams) {
                    msg += t.getId() + "# " + t.getName() + "\n";
                }

                break;
            case "team":
                if (commands.size() < 2) {
                    msg = "Mauvaise commande";
                    break;
                }

                List<String> copyCommands = new ArrayList(commands);

                String teamName = copyCommands.get(1);

                if (commands.size() > 2) {
                    copyCommands.remove(0);
                    teamName = copyCommands.stream().collect(Collectors.joining(" "));
                }

                Team t = this.teamService.getTeamByName(teamName);

                if (t == null) {
                    msg = "Aucune équipe trouvée correspondant à *" + commands.get(1) + "*.";
                    break;
                }

                List<FootballPlayer> goalkeepers = this.footballPlayerService.getFootballPlayersByTeamAndPost(t.getId(), EFootballPlayerPost.GOALKEEPER);
                List<FootballPlayer> defenders = this.footballPlayerService.getFootballPlayersByTeamAndPost(t.getId(), EFootballPlayerPost.DEFENDER);
                List<FootballPlayer> midfielders = this.footballPlayerService.getFootballPlayersByTeamAndPost(t.getId(), EFootballPlayerPost.MIDFIELDER);
                List<FootballPlayer> forwards = this.footballPlayerService.getFootballPlayersByTeamAndPost(t.getId(), EFootballPlayerPost.FORWARD);

                EmbedBuilder embed = new EmbedBuilder();

                embed.setTitle(t.getName());
                embed.setThumbnail(t.getLogo());

                String gk = "";
                String df = "";
                String mid = "";
                String atck = "";

                for (FootballPlayer f : goalkeepers) {
                    gk += f.getFirstName() + " " + f.getLastName() + "\n";
                }

                for (FootballPlayer f : defenders) {
                    df += f.getFirstName() + " " + f.getLastName() + "\n";
                }

                for (FootballPlayer f : midfielders) {
                    mid += f.getFirstName() + " " + f.getLastName() + "\n";
                }

                for (FootballPlayer f : forwards) {
                    atck += f.getFirstName() + " " + f.getLastName() + "\n";
                }

                embed.addField("Gardiens de but", gk);
                embed.addField("Défenseurs", df);
                embed.addField("Milieux", mid);
                embed.addField("Attaquants", atck);

                embed.setDescription(":stadium: " + t.getStadium().getName() + "(" + t.getStadium().getCapacity() + ")" );
                embed.setImage(t.getStadium().getPhoto());

                channel.sendMessage(embed);
                msg = "";

                break;
            case "generate":
                if (commands.size() != 2) {
                    msg = "Mauvaise commande";
                    break;
                }

                if (commands.get(1).equals("teams")) {
                    this.teamService.deleteAllTeams();
                    try {
                        Object o = new JSONParser(JSONParser.MODE_JSON_SIMPLE).parse(new FileReader("teams.json"));
                        JSONArray j = (JSONArray) o;

                        List<Team> generatedTeams = this.convertToList(j);

                        this.teamService.generateAllTeams(generatedTeams);

                        msg = "Les équipes ont été générées correctement :white_check_mark:";
                    } catch (FileNotFoundException | ParseException e) {
                        e.printStackTrace();
                        msg = "Erreur lors de la génération des équipes :x:";
                    }
                    break;
                } else if (commands.get(1).equals("players")) {
                    List<FootballPlayer> generatedPlayers = this.footballPlayerService.generateBotFootballPlayers();
                    msg = "Joueurs générés (20) :\n";
                    for (FootballPlayer footballPlayer : generatedPlayers) {
                        msg += footballPlayer.getFirstName() + " " + footballPlayer.getLastName() + " (" + footballPlayer.getPost().name() + ")\n";
                    }
                    break;
                } else {
                    msg = "Choisissez `players` ou `teams`.";
                    break;
                }
            default:
                msg = "Commande inconnue...";
                break;
        }
        return msg;
    }

    private List<String> removeCommandDiscriminator(String command) {
        return Arrays.asList(command.substring(1).split(" "));
    }

    private List<Team> convertToList(JSONArray jsonArray) {
        List<Team> teams = new ArrayList<>();

        for (Object obj : jsonArray) {
            JSONObject rawTeam = (JSONObject) obj;
            Team t = new Team();

            t.setName(rawTeam.get("name").toString());
            t.setLogo(rawTeam.get("logo").toString());
            t.setQuotaGoalKeepers(Integer.parseInt(rawTeam.get("quota_goalkeepers").toString()));
            t.setQuotaDefenders(Integer.parseInt(rawTeam.get("quota_defenders").toString()));
            t.setQuotaMidfielders(Integer.parseInt(rawTeam.get("quota_midfielders").toString()));
            t.setQuotaForwards(Integer.parseInt(rawTeam.get("quota_forwards").toString()));

            teams.add(t);
        }

        return teams;
    }
}
