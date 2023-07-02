package fr.kemstormy.discord.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kemstormy.discord.model.PlayerRecord;
import fr.kemstormy.discord.repository.PlayerRecordRepository;

@Service
public class PlayerRecordService {
    @Autowired
    private PlayerRecordRepository playerRecordRepository;

    public List<PlayerRecord> getPlayerRecordsByPlayer(Long footballPlayerId) {
        return this.playerRecordRepository.findPlayerRecordsByPlayerId(footballPlayerId);
    }
}
