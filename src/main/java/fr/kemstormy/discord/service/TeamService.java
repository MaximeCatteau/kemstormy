package fr.kemstormy.discord.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kemstormy.discord.enums.EFootballPlayerGenerationType;
import fr.kemstormy.discord.enums.EFootballPlayerPost;
import fr.kemstormy.discord.model.FootballPlayer;
import fr.kemstormy.discord.model.Team;
import fr.kemstormy.discord.repository.FootballPlayerRepository;
import fr.kemstormy.discord.repository.TeamRepository;

@Service
public class TeamService {
    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private FootballPlayerRepository footballPlayerRepository;

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
        // SI LE JOUEUR EST UN VRAI
        if (EFootballPlayerGenerationType.BY_PLAYER == footballPlayer.getGenerationType()) {
            // On cherche une équipe qui a un besoin de vrai joueur à son poste
            // On envoie la proposition
            if (EFootballPlayerPost.GOALKEEPER.equals(footballPlayer.getPost())) {
                return this.teamRepository.getMatchingTeamForRealGoalkeepers();
            } else if (EFootballPlayerPost.DEFENDER.equals(footballPlayer.getPost())) {
                return this.teamRepository.getMatchingTeamForRealDefenders();
            } else if (EFootballPlayerPost.MIDFIELDER.equals(footballPlayer.getPost())) {
                return this.teamRepository.getMatchingTeamForRealMidfielders();
            } else if (EFootballPlayerPost.FORWARD.equals(footballPlayer.getPost())) {
                return this.teamRepository.getMatchingTeamForRealForwards();
            } else {
                return null;
            }
        } else if (EFootballPlayerGenerationType.BY_BOT == footballPlayer.getGenerationType()) {
            // SI LE JOUEUR EST UN BOT
            if (EFootballPlayerPost.GOALKEEPER.equals(footballPlayer.getPost())) {
                return this.teamRepository.getMatchingTeamForGoalkeepers();
            } else if (EFootballPlayerPost.DEFENDER.equals(footballPlayer.getPost())) {
                return this.teamRepository.getMatchingTeamForDefenders();
            } else if (EFootballPlayerPost.MIDFIELDER.equals(footballPlayer.getPost())) {
                return this.teamRepository.getMatchingTeamForMidfielders();
            } else if (EFootballPlayerPost.FORWARD.equals(footballPlayer.getPost())) {
                return this.teamRepository.getMatchingTeamForForwards();
            } else {
                return null;
            }
        }

        return null;
    }

    public void recruitPlayer(Team team, FootballPlayer footballPlayer) {
        // trouver le joueur qui va être remplacé et le libérer (TODO : liste de transferts)
        if (willReplace(team, footballPlayer)) {
            FootballPlayer weakestOfTeam = this.findWeakestFootballPlayer(team, footballPlayer.getPost());
            this.freePlayer(weakestOfTeam);
            System.out.println("[ DEBUG ] " + weakestOfTeam.getMatchName() + " a été libéré de son club **" + team.getName() + "**.");
        }
        
        footballPlayer.setClub(team);
        System.out.println("[ DEBUG ] " + footballPlayer.getMatchName() + " a été transféré à **" + team.getName() + "**.");
        this.footballPlayerRepository.save(footballPlayer);
    }

    private void freePlayer(FootballPlayer fp) {
        fp.setClub(null);

        this.footballPlayerRepository.save(fp);
    }

    private boolean willReplace(Team team, FootballPlayer fp) {
        int quota = 0;
        int samePlayerAtPost = this.footballPlayerRepository.getFootballPlayersByTeamAndPost(team.getId(), fp.getPost().ordinal()).size();;       
        if (EFootballPlayerPost.GOALKEEPER.equals(fp.getPost())) {
            quota = team.getQuotaGoalKeepers();
        } else if (EFootballPlayerPost.DEFENDER.equals(fp.getPost())) {
            quota = team.getQuotaDefenders();
        }
        else if (EFootballPlayerPost.MIDFIELDER.equals(fp.getPost())) {
            quota = team.getQuotaMidfielders();
        }
        else if (EFootballPlayerPost.FORWARD.equals(fp.getPost())) {
            quota = team.getQuotaForwards();
        }

        return quota >= samePlayerAtPost;
    }

    private FootballPlayer findWeakestFootballPlayer(Team team, EFootballPlayerPost post) {
        List<FootballPlayer> teamPlayers = this.footballPlayerRepository.getFootballPlayersByTeamAndPost(team.getId(), post.ordinal());
        FootballPlayer weakest = teamPlayers.get(0);

        for(FootballPlayer fp : teamPlayers) {
            if (weakest.getOverallCharacteristicsAverage() > fp.getOverallCharacteristicsAverage()) {
                weakest = fp;
            }
        }

        return weakest;
    }
}
