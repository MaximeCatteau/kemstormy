package fr.kemstormy.discord.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kemstormy.discord.model.Ladder;
import fr.kemstormy.discord.repository.LadderRepository;

@Service
public class LadderService {
    @Autowired
    public LadderRepository ladderRepository;

    public Ladder getLadderByLeagueAndTeam(Long leagueId, Long teamId) {
        return this.ladderRepository.getByLeagueAndTeam(leagueId, teamId);
    }
}
