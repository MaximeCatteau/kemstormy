package fr.kemstormy.discord.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.TeamRecord;

@Repository
public interface TeamRecordRepository extends JpaRepository<TeamRecord, Long> {
    @Query(nativeQuery = true, value = "select * from team_record where team_id = :teamId")
    List<TeamRecord> findTeamRecordByTeamId(@Param("teamId") Long teamId);
}
