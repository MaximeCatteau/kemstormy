package fr.kemstormy.discord.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kemstormy.discord.model.FootballPlayer;
import fr.kemstormy.discord.repository.FootballPlayerRepository;

@Service
public class FootballPlayerService {
    @Autowired
    private FootballPlayerRepository footballPlayerRepository;

    public List<FootballPlayer> getAllFootballPlayers() {
        return this.footballPlayerRepository.findAll();
    }
}
