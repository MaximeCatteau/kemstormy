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
import fr.kemstormy.discord.model.PlayerCharacteristics;
import fr.kemstormy.discord.repository.FootballPlayerRepository;
import fr.kemstormy.discord.repository.PlayerCharacteristicsRepository;

@Service
public class FootballPlayerService {
    @Autowired
    private FootballPlayerRepository footballPlayerRepository;
    
    @Autowired
    private PlayerCharacteristicsRepository playerCharacteristicsRepository;

    public List<FootballPlayer> getAllFootballPlayers() {
        return this.footballPlayerRepository.findAll();
    }

    public List<FootballPlayer> generateBotFootballPlayers() {
        //this.deleteAllBotPlayers();

        List<FootballPlayer> footballPlayers = new ArrayList<>();

        List<String> frenchFirstNames = Collections.emptyList();
        List<String> frenchLastNames = Collections.emptyList();
        List<String> northAfricanFirstNames = Collections.emptyList();
        List<String> northAfricanLastNames = Collections.emptyList();

        Random random = new Random();

        try {
            frenchFirstNames = Files.readAllLines(Paths.get("first_names\\french_first_names.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            frenchLastNames = Files.readAllLines(Paths.get("last_names\\french_last_names.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            northAfricanFirstNames = Files.readAllLines(Paths.get("first_names\\north_african_first_names.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            northAfricanLastNames = Files.readAllLines(Paths.get("last_names\\north_african_last_names.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 20; i++) {
            FootballPlayer newFootballPlayer = new FootballPlayer();
            PlayerCharacteristics playerCharacteristics = new PlayerCharacteristics();

            playerCharacteristics = this.playerCharacteristicsRepository.save(playerCharacteristics);
            random = new Random();
            newFootballPlayer.setFirstName(frenchFirstNames.get(random.nextInt(frenchFirstNames.size())));
            random = new Random();
            newFootballPlayer.setLastName(frenchLastNames.get(random.nextInt(frenchLastNames.size())));
            newFootballPlayer.setGenerationType(EFootballPlayerGenerationType.BY_BOT);
            newFootballPlayer.setAge(18);
            newFootballPlayer.setOwner(null);
            newFootballPlayer.setPost(EFootballPlayerPost.randomPlayerPost());
            newFootballPlayer.setPlayerCharacteristics(playerCharacteristics);

            footballPlayers.add(newFootballPlayer);
        }

        for (int i = 0; i < 10; i++) {
            FootballPlayer newFootballPlayer = new FootballPlayer();
            random = new Random();
            newFootballPlayer.setFirstName(northAfricanFirstNames.get(random.nextInt(northAfricanFirstNames.size())));
            random = new Random();
            newFootballPlayer.setLastName(northAfricanLastNames.get(random.nextInt(northAfricanLastNames.size())));
            newFootballPlayer.setGenerationType(EFootballPlayerGenerationType.BY_BOT);
            newFootballPlayer.setAge(18);
            newFootballPlayer.setOwner(null);
            newFootballPlayer.setPost(EFootballPlayerPost.randomPlayerPost());

            footballPlayers.add(newFootballPlayer);
        }

        this.footballPlayerRepository.saveAll(footballPlayers);

        return footballPlayers;
    }

    public FootballPlayer createOrUpdateFootballPlayer(FootballPlayer footballPlayer) {
        if(footballPlayer.getId() == null) { 
            footballPlayer.setGenerationType(EFootballPlayerGenerationType.BY_PLAYER);
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
}
