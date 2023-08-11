package fr.kemstormy.discord.utils;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
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
import fr.kemstormy.discord.model.Ladder;
import fr.kemstormy.discord.model.League;
import fr.kemstormy.discord.model.Nationality;
import fr.kemstormy.discord.model.PlayerCharacteristics;
import fr.kemstormy.discord.model.PlayerRecord;
import fr.kemstormy.discord.model.Team;
import fr.kemstormy.discord.model.TeamRecord;
import fr.kemstormy.discord.resource.PasserLadderResource;
import fr.kemstormy.discord.resource.StrikerLadderResource;
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
    private LeagueService leagueService;
    private MatchService matchService;
    private LadderService ladderService;
    private MatchStrikerService matchStrikerService;
    private MatchDecisivePassersService matchDecisivePassersService;
    private TeamRecordService teamRecordService;
    private PlayerRecordService playerRecordService;

    public DiscordUtils(@Lazy DiscordUserService discordUserService, @Lazy FootballPlayerService footballPlayerService, @Lazy TeamService teamService, @Lazy MatchService matchService, @Lazy LeagueService leagueService, @Lazy LadderService ladderService, @Lazy MatchStrikerService matchStrikerService, @Lazy MatchDecisivePassersService matchDecisivePassersService, @Lazy TeamRecordService teamRecordService, @Lazy PlayerRecordService playerRecordService) {
        this.discordUserService = discordUserService;
        this.footballPlayerService = footballPlayerService;
        this.teamService = teamService;
        this.leagueService = leagueService;
        this.matchService = matchService;
        this.ladderService = ladderService;
        this.matchStrikerService = matchStrikerService;
        this.matchDecisivePassersService = matchDecisivePassersService;
        this.teamRecordService = teamRecordService;
        this.playerRecordService = playerRecordService;
    }

    public String getCommand(String command, User messageAuthor, MessageCreateEvent event) throws InterruptedException, IOException {
        List<String> commands = this.removeCommandDiscriminator(command);
        String mainCommand = commands.get(0);
        String msg = "";
        TextChannel channel = event.getChannel();
        String leagueName = "";
        List<Team> leagueTeams = new ArrayList<>();
        League league;
        EmbedBuilder embed = new EmbedBuilder();

        switch (mainCommand) {
            case "register":
                DiscordUser du = new DiscordUser();
                du.setDiscordId(messageAuthor.getIdAsString());

                this.discordUserService.createOrUpdateDiscordUser(du);

                msg = "Compte enregistré :white_check_mark:";
                break;
            case "players":
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
            case "player":
                List<String> copyPlayerCommands = new ArrayList(commands);

                if (commands.size() < 3) {
                    msg = "Mauvaise commande, utilisez !player prenom nom";
                    break;
                }

                String playerFirstname = commands.get(1);
                String playerLastname = commands.get(2);

                FootballPlayer findingPlayer = this.footballPlayerService.findByFirstNameAndLastName(playerFirstname, playerLastname);

                if (findingPlayer == null) {
                    msg = "Ce joueur n'existe pas.";
                    break;
                }

                Nationality nationality = findingPlayer.getNationality();

                embed.setTitle(findingPlayer.getFirstName() + " " + findingPlayer.getLastName() + " - " + nationality.getFlag() + " - Niveau " + findingPlayer.getLevel());

                if (findingPlayer.getClub() != null) {
                    embed.setDescription(findingPlayer.getClub().getName());
                    embed.setColor(this.convertHexToColor(findingPlayer.getClub().getMainColor()));
                    embed.setThumbnail(findingPlayer.getClub().getLogo());
                } else {
                    embed.setDescription("_Sans club_");
                }

                int playerExp = findingPlayer.getPlayerCharacteristics().getExperience();
                int playerAge = findingPlayer.getAge();
                int playerScoredGoals = this.matchStrikerService.getScoredGoalsForPlayer(findingPlayer.getId());
                int playerAssists = this.matchDecisivePassersService.getAssistsForPlayer(findingPlayer.getId());

                List<PlayerRecord> playerRecords = this.playerRecordService.getPlayerRecordsByPlayer(findingPlayer.getId());

                String strPlayerRecords = "";

                for (PlayerRecord playerRecord : playerRecords) {
                    strPlayerRecords += "- " + playerRecord.getLabel() + "\n";
                }

                embed.addField("Expérience", "" + playerExp);
                embed.addField("Âge", "" + playerAge);
                embed.addField("Buts marqués", "" + playerScoredGoals);
                embed.addField("Passes décisives", "" + playerAssists);

                if (!strPlayerRecords.equals("")) {
                    embed.addField("Palmarès", strPlayerRecords);
                }

                channel.sendMessage(embed);
                break;
            case "create":
                if (commands.size() != 4) {
                    msg = "Commande invalide, veuillez réessayer avec `!create prenom nom post`";
                    break;
                }
                FootballPlayer createdFootballPlayer = new FootballPlayer();
                DiscordUser discordUser = this.discordUserService.getByDiscordId(messageAuthor.getIdAsString());

                FootballPlayer existing = this.footballPlayerService.findByOwnerId(discordUser.getId());

                if (existing != null) {
                    msg = "Vous avez déjà un joueur créé (" + existing.getMatchName() + ").";
                    break;
                }

                createdFootballPlayer.setAge(18);
                createdFootballPlayer.setFirstName(commands.get(1));
                createdFootballPlayer.setLastName(commands.get(2));
                createdFootballPlayer.setOwner(discordUser);
                createdFootballPlayer.setPost(EFootballPlayerPost.valueOf(commands.get(3).toUpperCase()));

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

                embed.setTitle(t.getName());
                embed.setThumbnail(t.getLogo());
                embed.setColor(this.convertHexToColor(t.getMainColor()));

                String gk = "";
                String df = "";
                String mid = "";
                String atck = "";

                for (FootballPlayer f : goalkeepers) {
                    gk += f.getFirstName() + " " + f.getLastName() + " - " + f.getNationality().getFlag() + "\n";
                }

                for (FootballPlayer f : defenders) {
                    df += f.getFirstName() + " " + f.getLastName() + " - " + f.getNationality().getFlag() + "\n";
                }

                for (FootballPlayer f : midfielders) {
                    mid += f.getFirstName() + " " + f.getLastName() + " - " + f.getNationality().getFlag() + "\n";
                }

                for (FootballPlayer f : forwards) {
                    atck += f.getFirstName() + " " + f.getLastName() + " - " + f.getNationality().getFlag() + "\n";
                }

                List<TeamRecord> teamRecords = this.teamRecordService.getTeamRecordByTeam(t.getId());

                String strTeamRecords = "";

                for (TeamRecord tr : teamRecords) {
                    strTeamRecords += "- " + tr.getLabel() + "\n";
                }

                embed.addField("Budget", t.getBudget() + "€");
                embed.addField("Gardiens de but", gk);
                embed.addField("Défenseurs", df);
                embed.addField("Milieux", mid);
                embed.addField("Attaquants", atck);

                if (!strTeamRecords.equals("")) {
                    embed.addField("Palmarès", strTeamRecords);
                }

                embed.setDescription(":stadium: " + t.getStadium().getName() + " (*Niveau : " + t.getStadium().getLevel() + " - " + t.getStadium().getCapacity() + " places*)" );
                embed.setImage(t.getStadium().getPhoto());

                channel.sendMessage(embed);
                msg = "";

                break;

            case "league":
                if (commands.size() < 2) {
                    msg = "Mauvaise commande";
                    break;
                }

                List<String> copyLeagueCommands = new ArrayList(commands);

                leagueName = copyLeagueCommands.get(1);

                if (commands.size() > 2) {
                    copyLeagueCommands.remove(0);
                    leagueName = copyLeagueCommands.stream().collect(Collectors.joining(" "));
                }

                league = this.leagueService.getLeagueByName(leagueName);
                if (league == null) {
                    msg = "Championnat inconnu";
                    break;
                }

                leagueTeams = this.teamService.getLeagueTeams(league.getId());

                EmbedBuilder leagueEmbed = new EmbedBuilder();
                leagueEmbed.setTitle(league.getName());
                leagueEmbed.setThumbnail(league.getLogo());
                for (Team leagueTeam : leagueTeams) {
                    leagueEmbed.addField("" + (leagueTeams.indexOf(leagueTeam)+1), leagueTeam.getName());
                  }
                channel.sendMessage(leagueEmbed);
                break;
            case "generate":
                if (commands.size() < 2) {
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
                    if (!messageAuthor.getIdAsString().equals("185790407156826113")) {
                        msg = "Vous n'avez pas accès à cette commande";
                        break;
                    }

                    List<FootballPlayer> newPlayers = this.footballPlayerService.generateNewFootballPlayers(Long.valueOf(1));

                    msg = newPlayers.size() + " ont été générés correctement !";
                    break;
                } else if (commands.get(1).equals("matchs")) {
                    if (!messageAuthor.getIdAsString().equals("185790407156826113")) {
                        msg = "Vous n'avez pas accès à cette commande";
                        break;
                    }
                    
                    List<String> cmds = new ArrayList(commands);

                    String mainLeagueName = cmds.get(2);

                    if (commands.size() > 3) {
                        cmds.remove(0);
                        cmds.remove(0);
                        mainLeagueName = cmds.stream().collect(Collectors.joining(" "));
                    }

                    League l = this.leagueService.getLeagueByName(mainLeagueName);

                    this.matchService.championshipScheduling(l);
                    msg = ":white_check_mark: Matchs générés pour le championnat : " + l.getName() + " !";
                    break;
                }
                else {
                    msg = "Choisissez `players` ou `teams`.";
                    break;
                }
            case "match":
                List<Team> opponents = this.teamService.composeRandomMatch();
                Team home = opponents.get(0);
                Team away = opponents.get(1);
                League homeLeague = home.getLeague();

                EmbedBuilder matchPreviewEmbed = new EmbedBuilder();
                matchPreviewEmbed.setTitle(home.getName() + " - " + away.getName());

                this.matchService.createMatch(home, away, homeLeague);

                matchPreviewEmbed.setDescription(":stadium: " + home.getStadium().getName());
                matchPreviewEmbed.setImage(home.getStadium().getPhoto());

                channel.sendMessage(matchPreviewEmbed);
                msg = "";
                break;
            case "play":
                try {
                    this.matchService.playMatch(channel);
                } catch(InterruptedException e) {
                    msg = "Match interrompu";
                } catch (FileNotFoundException e) {
                    msg = "Fichier non trouvé";
                }
                break;
            case "ladder":
                List<String> copyLadderCommands = new ArrayList(commands);
                leagueName = copyLadderCommands.get(1);

                if (commands.size() > 2) {
                    copyLadderCommands.remove(0);
                    leagueName = copyLadderCommands.stream().collect(Collectors.joining(" "));
                }

                league = this.leagueService.getLeagueByName(leagueName);
                if (league == null) {
                    msg = "Championnat inconnu";
                    break;
                }

                leagueTeams = this.teamService.getLeagueTeams(league.getId());

                List<Ladder> ladder = this.ladderService.getLaddersByLeague(league.getId());

                channel.sendMessage(this.ladderService.createEmbedLadder(ladder, league.getId()));
                break;
            case "strikers":
                List<String> copyStrikersCommands = new ArrayList(commands);
                leagueName = copyStrikersCommands.get(1);

                if (commands.size() > 2) {
                    copyStrikersCommands.remove(0);
                    leagueName = copyStrikersCommands.stream().collect(Collectors.joining(" "));
                }

                league = this.leagueService.getLeagueByName(leagueName);
                List<StrikerLadderResource> strikers = this.matchStrikerService.getStrikersLadder(league.getId());

                channel.sendMessage(this.matchStrikerService.createEmbedStrikersLadder(strikers, league.getId()));
                break;
            case "passers":
                List<String> copyPassersCommands = new ArrayList(commands);
                leagueName = copyPassersCommands.get(1);

                if (commands.size() > 2) {
                    copyPassersCommands.remove(0);
                    leagueName = copyPassersCommands.stream().collect(Collectors.joining(" "));
                }

                league = this.leagueService.getLeagueByName(leagueName);
                List<PasserLadderResource> passers = this.matchDecisivePassersService.getDecisivePassersLadder(league.getId());

                channel.sendMessage(this.matchDecisivePassersService.createEmbedDecisivePassersLadder(passers, league.getId()));
                break;
            default:
                msg = "Commande inconnue...";
                break;
        }
        return msg;
    }

    public String getPrivateCommand(MessageCreateEvent event, DiscordApi api) throws InterruptedException, ExecutionException {
        String msg = "";
        String content = event.getMessageContent();
        PrivateChannel channel = event.getPrivateChannel().get();

        List<String> commands = this.removeCommandDiscriminator(content);
        String mainCommand = commands.get(0);
        String authorId = event.getMessageAuthor().getIdAsString();
        
        DiscordUser discordUser = this.discordUserService.getByDiscordId(authorId);

        if (discordUser == null) {
            return "Désolé, vous devez vous inscrire sur un des serveurs partenaires pour envoyer des commandes";
        }

        FootballPlayer footballPlayer = this.footballPlayerService.findByOwnerId(discordUser.getId());

        switch (mainCommand) {
            case "upgrade":
                if (footballPlayer == null) {
                    return "Désolé, vous n'avez pas encore créé votre joueur. Contactez votre administrateur pour plus d'informations !";
                }

                if (footballPlayer.getPointsToSet() < 1) {
                    return "Vous n'avez pas assez de points. Gagnez en expérience afin de monter votre niveau et gagner des points de caractéristiques !";
                }

                Message message = channel.sendMessage( "Souhaitez-vous augmenter les capacités de " + footballPlayer.getMatchName() + " ?").get();
                message.addReaction("✅");
				message.addReaction("❌");

                message.addReactionAddListener(react -> {
					User user = null;
                    try {
                        user = api.getUserById(react.getUserIdAsString()).get();
                        if (!user.isBot() && react.getEmoji().equalsEmoji("✅")) {
                            Message whatSkill = channel.sendMessage(
                                "Quelle compétence voulez-vous augmenter ?\n" +
                                "0️⃣ - Finition\n" +
                                "1️⃣ - Passes courtes\n" +
                                "2️⃣ - Tirs de loin\n" +
                                "3️⃣ - Interceptions\n" +
                                "4️⃣ - Tacles\n" +
                                "5️⃣ - Passes longues\n" +
                                "6️⃣ - Corners\n" +
                                "7️⃣ - Dribbles\n" +
                                "8️⃣ - Coups francs\n" +
                                "9️⃣ - Penalty\n" +
                                "🔟 - Réflexes (gardiens)\n" +
                                "🔴 - Arrêts (gardiens)\n"
                                ).get();
                            whatSkill.addReaction("0️⃣");
                            whatSkill.addReaction("1️⃣");
                            whatSkill.addReaction("2️⃣");
                            whatSkill.addReaction("3️⃣");
                            whatSkill.addReaction("4️⃣");
                            whatSkill.addReaction("5️⃣");
                            whatSkill.addReaction("6️⃣");
                            whatSkill.addReaction("7️⃣");
                            whatSkill.addReaction("8️⃣");
                            whatSkill.addReaction("9️⃣");
                            whatSkill.addReaction("🔟");
                            whatSkill.addReaction("🔴");

                            whatSkill.addReactionAddListener(skill -> {
                                User skillUser;
                                try {
                                    skillUser = api.getUserById(skill.getUserIdAsString()).get();
                                    if (!skillUser.isBot()) {
                                        this.handleSkillUpgradeReaction(channel, skill.getEmoji(), footballPlayer);
                                        whatSkill.delete();
                                        message.delete();
                                    }
                                } catch (InterruptedException | ExecutionException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }).removeAfter(2, TimeUnit.MINUTES);
                        } else if (!user.isBot() && react.getEmoji().equalsEmoji("❌")) {
                            channel.sendMessage("Dommage mon frérito !");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
				}).removeAfter(10, TimeUnit.MINUTES);
        }

        return msg;
    }

    private void handleSkillUpgradeReaction(PrivateChannel channel, Emoji emoji, FootballPlayer footballPlayer) {
        PlayerCharacteristics pc = footballPlayer.getPlayerCharacteristics();

        switch (emoji.asUnicodeEmoji().get()) {
            case "0️⃣":
                channel.sendMessage("Vous avez augmenté votre finition de 1 !");
                pc.setShots(pc.getShots() < 99 ? pc.getShots() + 1 : 99);
                break;
            case "1️⃣":
                channel.sendMessage("Vous avez augmenté vos passes de 1 !");
                pc.setPasses(pc.getPasses() < 99 ? pc.getPasses() + 1 : 99);
                break;
            case "2️⃣":
                channel.sendMessage("Vous avez augmenté vos tirs de loin de 1 !");
                pc.setLongShots(pc.getLongShots() < 99 ? pc.getLongShots() + 1 : 99);
                break;
            case "3️⃣":
                channel.sendMessage("Vous avez augmenté vos interceptions de 1 !");
                pc.setInterceptions(pc.getInterceptions() < 99 ? pc.getInterceptions() + 1 : 99);
                break;
            case "4️⃣":
                channel.sendMessage("Vous avez augmenté vos tacles de 1 !");
                pc.setTackles(pc.getTackles() < 99 ? pc.getTackles() + 1 : 99);
                break;
            case "5️⃣":
                channel.sendMessage("Vous avez augmenté vos passes longues de 1 !");
                pc.setLongPasses(pc.getLongPasses() < 99 ? pc.getLongPasses() + 1 : 99);
                break;
            case "6️⃣":
                channel.sendMessage("Vous avez augmenté vos corners de 1 !");
                pc.setCorners(pc.getCorners() < 99 ? pc.getCorners() + 1 : 99);
                break;
            case "7️⃣":
                channel.sendMessage("Vous avez augmenté vos dribbles de 1 !");
                pc.setDribbles(pc.getDribbles() < 99 ? pc.getDribbles() + 1 : 99);
                break;
            case "8️⃣":
                channel.sendMessage("Vous avez augmenté vos coups francs de 1 !");
                pc.setFreekicks(pc.getFreekicks() < 99 ? pc.getFreekicks() + 1 : 99);
                break;
            case "9️⃣":
                channel.sendMessage("Vous avez augmenté vos penaltys de 1 !");
                pc.setPenalty(pc.getPenalty() < 99 ? pc.getPenalty() + 1 : 99);
                break;
            case "🔟":
                channel.sendMessage("Vous avez augmenté vos réflexes de 1 !");
                pc.setReflexes(pc.getReflexes() < 99 ? pc.getReflexes() + 1 : 99);
                break;
            case "🔴":
                channel.sendMessage("Vous avez augmenté vos arrêts de 1 !");
                pc.setShotStopping(pc.getShotStopping() < 99 ? pc.getShotStopping() + 1 : 99);
                break;
            default:
                channel.sendMessage("Emoji non reconnu !");
                break;
        }

        footballPlayer.setPlayerCharacteristics(pc);
        footballPlayer.setPointsToSet(footballPlayer.getPointsToSet() > 0 ? footballPlayer.getPointsToSet() - 1 : 0);

        this.footballPlayerService.createOrUpdateFootballPlayer(footballPlayer);
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

    private Color convertHexToColor(String hex) {
        int r = Integer.valueOf(hex.substring(1, 3), 16);
        int g = Integer.valueOf(hex.substring(3, 5), 16);
        int b = Integer.valueOf(hex.substring(5, 7), 16);
        Color color = new Color(r, g, b);

        return color;
    }
}
