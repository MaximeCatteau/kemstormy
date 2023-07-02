package fr.kemstormy.discord.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kemstormy.discord.model.TeamRecord;
import fr.kemstormy.discord.repository.TeamRecordRepository;

@Service
public class TeamRecordService {
    @Autowired
    private TeamRecordRepository teamRecordRepository;

    public List<TeamRecord> getTeamRecordByTeam(Long teamId) {
        return this.teamRecordRepository.findTeamRecordByTeamId(teamId);
    }
}
