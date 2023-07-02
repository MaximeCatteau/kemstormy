package fr.kemstormy.discord.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.MatchStriker;
import jakarta.persistence.Tuple;

@Repository
public interface MatchStrikerRepository extends JpaRepository<MatchStriker, Long> {
    @Query(nativeQuery = true, value = "select count(*) as \"scored_goals\", fp.first_name as \"first_name\", fp.last_name as \"last_name\", t.name as \"team_name\" from match_striker ms left join match m on ms.match_id = m.id left join football_player fp on fp.id = ms.football_player_id left join team t on t.id = fp.club_id where t.league_id = :leagueId group by first_name, last_name, t.name order by \"scored_goals\" desc limit 10")
    List<Tuple> getStrikersLadder(@Param("leagueId") Long leagueId);

    @Query(nativeQuery = true, value = "select count(*) as \"scored_goals\", fp.first_name as \"first_name\", fp.last_name as \"last_name\", t.name as \"team_name\" from match_striker ms left join match m on ms.match_id = m.id left join football_player fp on fp.id = ms.football_player_id left join team t on t.id = fp.club_id where t.league_id = :leagueId group by first_name, last_name, t.name order by \"scored_goals\" desc limit 1")
    Tuple getBestStriker(@Param("leagueId") Long leagueId);

    @Query(nativeQuery = true, value = "select count(*) from match_striker where football_player_id = :playerId")
    Integer countScoredGoalsByPlayer(@Param("playerId") Long playerId);
}
