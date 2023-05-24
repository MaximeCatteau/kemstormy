package fr.kemstormy.discord.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kemstormy.discord.model.League;
import fr.kemstormy.discord.repository.LeagueRepository;


@Service
public class LeagueService {
    @Autowired
    private LeagueRepository leagueRepository;

    public League getLeagueByName(String leagueName) {
        return this.leagueRepository.findByLeagueName(leagueName);
    }

}