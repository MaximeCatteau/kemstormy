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
import fr.kemstormy.discord.repository.FootballPlayerRepository;

@Service
public class FootballPlayerService {
    @Autowired
    private FootballPlayerRepository footballPlayerRepository;

    public List<FootballPlayer> getAllFootballPlayers() {
        return this.footballPlayerRepository.findAll();
    }

    public List<FootballPlayer> generateBotFootballPlayers() {
        //this.deleteAllBotPlayers();

        List<FootballPlayer> footballPlayers = new ArrayList<>();

        List<String> firstNames = Collections.emptyList();
        List<String> lastNames = Collections.emptyList();

        Random random = new Random();

        try {
            firstNames = Files.readAllLines(Paths.get("first_names\\french_first_names.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            lastNames = Files.readAllLines(Paths.get("last_names\\french_last_names.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 20; i++) {
            FootballPlayer newFootballPlayer = new FootballPlayer();
            random = new Random();
            newFootballPlayer.setFirstName(firstNames.get(random.nextInt(firstNames.size())));
            random = new Random();
            newFootballPlayer.setLastName(lastNames.get(random.nextInt(lastNames.size())));
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
}
