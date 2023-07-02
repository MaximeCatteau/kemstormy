package fr.kemstormy.discord.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.PlayerRecord;

@Repository
public interface PlayerRecordRepository extends JpaRepository<PlayerRecord, Long> {
    @Query(nativeQuery = true, value = "select * from player_record where football_player_id = :playerId")
    List<PlayerRecord> findPlayerRecordsByPlayerId(@Param("playerId") Long footballPlayerId);
}
