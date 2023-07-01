package fr.kemstormy.discord.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.Ladder;

@Repository
public interface LadderRepository extends JpaRepository<Ladder, Long> {
    @Query(nativeQuery = true, value = "select * from ladder where league_id = :leagueId and team_id = :teamId")
    public Ladder getByLeagueAndTeam(@Param("leagueId") Long leagueId, @Param("teamId") Long teamId);

    @Query(nativeQuery = true, value = "select * from ladder where league_id = :leagueId order by victories * 3 + draws desc")
    public List<Ladder> findByLeagueId(@Param("leagueId") Long leagueId);
}