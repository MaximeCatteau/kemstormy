package fr.kemstormy.discord.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kemstormy.discord.model.FootballPlayer;
import fr.kemstormy.discord.model.Team;
import fr.kemstormy.discord.repository.TeamRepository;

@Service
public class TeamService {
    @Autowired
    private TeamRepository teamRepository;

    public List<Team> getAllTeams() {
        return this.teamRepository.findAll();
    }

    public Team getTeamByName(String teamName) {
        return this.teamRepository.findByTeamName(teamName);
    }

    public List<Team> getLeagueTeams(Long leagueId) {
        return this.teamRepository.getLeagueTeams(leagueId);
    }

    public List<Team> generateAllTeams(List<Team> teams) {
        return this.teamRepository.saveAll(teams);
    }

    public void deleteAllTeams() {
        this.teamRepository.deleteAll();
    }

    public List<Team> composeRandomMatch() {
        return this.teamRepository.getTwoRandomTeamsForMatch();
    }

    public Team handleRecruitment(FootballPlayer footballPlayer) {
        return this.teamRepository.getMatchingTeam(footballPlayer.getPost().ordinal());
    }
}
