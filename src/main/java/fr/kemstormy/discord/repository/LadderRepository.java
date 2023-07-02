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

    @Query(nativeQuery = true, value = "select * from ladder where league_id = :leagueId order by victories * 3 + draws desc, scored_goals - conceded_goals desc")
    public List<Ladder> findByLeagueId(@Param("leagueId") Long leagueId);

    @Query(nativeQuery = true, value = "select * from ladder where league_id = :leagueId order by victories * 3 + draws desc, scored_goals - conceded_goals desc limit 1")
    public Ladder getChampionOfLeague(@Param("leagueId") Long leagueId);

    @Query(nativeQuery = true, value = "select * from ladder where league_id = :leagueId order by victories * 3 + draws asc, scored_goals - conceded_goals asc limit 1")
    public Ladder getLastOfLeague(@Param("leagueId") Long leagueId);

    @Query(nativeQuery = true, value = "select * from ladder where league_id = :leagueId order by conceded_goals asc limit 1")
    public Ladder getBestDefence(@Param("leagueId") Long leagueId);
}
