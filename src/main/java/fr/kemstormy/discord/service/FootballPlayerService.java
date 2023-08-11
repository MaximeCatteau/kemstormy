package fr.kemstormy.discord.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kemstormy.discord.enums.EFootballPlayerGenerationType;
import fr.kemstormy.discord.enums.EFootballPlayerPost;
import fr.kemstormy.discord.model.FootballPlayer;
import fr.kemstormy.discord.model.Nationality;
import fr.kemstormy.discord.model.PlayerCharacteristics;
import fr.kemstormy.discord.repository.FirstNameRepository;
import fr.kemstormy.discord.repository.FootballPlayerRepository;
import fr.kemstormy.discord.repository.LastNameRepository;
import fr.kemstormy.discord.repository.NationalityRepository;
import fr.kemstormy.discord.repository.PlayerCharacteristicsRepository;
import fr.kemstormy.discord.repository.TeamRepository;

@Service
public class FootballPlayerService {
    @Autowired
    private FootballPlayerRepository footballPlayerRepository;
    
    @Autowired
    private PlayerCharacteristicsRepository playerCharacteristicsRepository;

    @Autowired
    private FirstNameRepository firstNameRepository;

    @Autowired
    private LastNameRepository lastNameRepository;

    @Autowired
    private NationalityRepository nationalityRepository;

    @Autowired
    private TeamRepository teamRepository;

    public List<FootballPlayer> getAllFootballPlayers() {
        return this.footballPlayerRepository.findAll();
    }

    public FootballPlayer createOrUpdateFootballPlayer(FootballPlayer footballPlayer) {
        if(footballPlayer.getId() == null) { 
            footballPlayer.setGenerationType(EFootballPlayerGenerationType.BY_PLAYER);
        } else {
            this.playerCharacteristicsRepository.save(footballPlayer.getPlayerCharacteristics());
            return this.footballPlayerRepository.save(footballPlayer);
        }

        PlayerCharacteristics playerCharacteristics = new PlayerCharacteristics();
        playerCharacteristics = this.playerCharacteristicsRepository.save(playerCharacteristics);
        
        footballPlayer.setPlayerCharacteristics(playerCharacteristics);

        return this.footballPlayerRepository.save(footballPlayer);
    }

    public void deleteAllBotPlayers() {
        this.footballPlayerRepository.deleteAllBotPlayers();
    }

    public FootballPlayer getRandomFootballPlayerWithNoTeam() {
        return this.footballPlayerRepository.getFootballPlayerWithNoClub();
    }

    public List<FootballPlayer> getFootballPlayersByTeamAndPost(Long clubId, EFootballPlayerPost post) {
        return this.footballPlayerRepository.getFootballPlayersByTeamAndPost(clubId, post.ordinal());
    }

    public FootballPlayer findByOwnerId(Long ownerId) {
        return this.footballPlayerRepository.findByOwnerId(ownerId);
    }

    public FootballPlayer findByFirstNameAndLastName(String firstName, String lastName) {
        return this.footballPlayerRepository.findByFirstNameAndLastName(firstName, lastName);
    }

    public List<FootballPlayer> generateNewFootballPlayers(Long leagueId) {
        List<FootballPlayer> newFootballPlayers = new ArrayList<>();

        newFootballPlayers.addAll(this.generateByCountry("Italie", 2));
        newFootballPlayers.addAll(this.generateByCountry("Brésil", 8));
        newFootballPlayers.addAll(this.generateByCountry("Espagne", 5));
        newFootballPlayers.addAll(this.generateByCountry("Portugal", 5));
        newFootballPlayers.addAll(this.generateByCountry("Maroc", 5));
        newFootballPlayers.addAll(this.generateByCountry("Argentine", 3));
        newFootballPlayers.addAll(this.generateByCountry("France", 83));
        newFootballPlayers.addAll(this.generateByCountry("Autriche", 2));
        newFootballPlayers.addAll(this.generateByCountry("Ghana", 2));
        newFootballPlayers.addAll(this.generateByCountry("Côte d'Ivoire", 7));
        newFootballPlayers.addAll(this.generateByCountry("Colombie", 1));
        newFootballPlayers.addAll(this.generateByCountry("Belgique", 8));
        newFootballPlayers.addAll(this.generateByCountry("République démocratique du Congo", 3));
        newFootballPlayers.addAll(this.generateByCountry("Bosnie-Herzégovine", 1));
        newFootballPlayers.addAll(this.generateByCountry("Turquie", 1));
        newFootballPlayers.addAll(this.generateByCountry("Ukraine", 1));
        newFootballPlayers.addAll(this.generateByCountry("Chili", 2));
        newFootballPlayers.addAll(this.generateByCountry("Mali", 6));
        newFootballPlayers.addAll(this.generateByCountry("Croatie", 1));
        newFootballPlayers.addAll(this.generateByCountry("Cameroun", 6));
        newFootballPlayers.addAll(this.generateByCountry("États-Unis", 2));
        newFootballPlayers.addAll(this.generateByCountry("Canada", 1));
        newFootballPlayers.addAll(this.generateByCountry("Guinée", 2));
        newFootballPlayers.addAll(this.generateByCountry("Allemagne", 1));
        newFootballPlayers.addAll(this.generateByCountry("Japon", 3));
        newFootballPlayers.addAll(this.generateByCountry("Russie", 1));
        newFootballPlayers.addAll(this.generateByCountry("Sénégal", 5));
        newFootballPlayers.addAll(this.generateByCountry("Pays-Bas", 3));
        newFootballPlayers.addAll(this.generateByCountry("Pologne", 1));
        newFootballPlayers.addAll(this.generateByCountry("Algérie", 7));
        newFootballPlayers.addAll(this.generateByCountry("Kosovo", 1));
        newFootballPlayers.addAll(this.generateByCountry("Danemark", 1));
        newFootballPlayers.addAll(this.generateByCountry("Suisse", 2));
        newFootballPlayers.addAll(this.generateByCountry("Pays de Galles", 1));
        newFootballPlayers.addAll(this.generateByCountry("Nigéria", 2));
        newFootballPlayers.addAll(this.generateByCountry("Tunisie", 2));
        newFootballPlayers.addAll(this.generateByCountry("Suède", 1));
        newFootballPlayers.addAll(this.generateByCountry("Zimbabwe", 1));
        newFootballPlayers.addAll(this.generateByCountry("Cap Vert", 1));
        newFootballPlayers.addAll(this.generateByCountry("Norvège", 1));
        newFootballPlayers.addAll(this.generateByCountry("Serbie", 1));
        newFootballPlayers.addAll(this.generateByCountry("Australie", 1));
        newFootballPlayers.addAll(this.generateByCountry("Bénin", 1));
        newFootballPlayers.addAll(this.generateByCountry("Guinée-Bissau", 1));
        newFootballPlayers.addAll(this.generateByCountry("Haïti", 1));
        newFootballPlayers.addAll(this.generateByCountry("Gambie", 1));
        newFootballPlayers.addAll(this.generateByCountry("Géorgie", 1));

        Collections.shuffle(newFootballPlayers);

        int quotaGoalkeepers = this.teamRepository.getGoalkeepersQuotaSumForTeams(leagueId);
        int quotaDefenders = this.teamRepository.getDefendersQuotaSumForTeams(leagueId);
        int quotaMidfielders = this.teamRepository.getMidfieldersQuotaSumForTeams(leagueId);
        int quotaForwards = this.teamRepository.getForwardsQuotaSumForTeams(leagueId);

        int i = 0;

        for (FootballPlayer fp : newFootballPlayers) {
            PlayerCharacteristics pc = new PlayerCharacteristics();

            this.playerCharacteristicsRepository.save(pc);

            if (i < quotaGoalkeepers) {
                fp.setPost(EFootballPlayerPost.GOALKEEPER);
            } else if (i >= quotaGoalkeepers && i < (quotaGoalkeepers + quotaDefenders)) {
                fp.setPost(EFootballPlayerPost.DEFENDER);
            } else if (i >= (quotaGoalkeepers + quotaDefenders) && i < (quotaGoalkeepers + quotaDefenders + quotaMidfielders)) {
                fp.setPost(EFootballPlayerPost.MIDFIELDER);
            } else if (i >= (quotaGoalkeepers + quotaDefenders + quotaMidfielders) && i < (quotaGoalkeepers + quotaDefenders + quotaMidfielders + quotaForwards)) {
                fp.setPost(EFootballPlayerPost.FORWARD);
            }

            fp.setPlayerCharacteristics(pc);
            
            i++;
        }

        this.footballPlayerRepository.saveAll(newFootballPlayers);

        return newFootballPlayers;
    }

    private List<FootballPlayer> generateByCountry(String country, int number) {
        Nationality nationality = this.nationalityRepository.getByCountry(country);
        List<FootballPlayer> players = new ArrayList<>();

        for (int i = 0; i < number; i++) {
            FootballPlayer f = new FootballPlayer();

            f.setNationality(nationality);
            f.setFirstName(this.firstNameRepository.getRandomFirstNameByCountry(nationality.getId()));
            f.setLastName(this.lastNameRepository.getRandomLastNameByCountry(nationality.getId()));
            players.add(f);
        }

        return players;
    }
}
