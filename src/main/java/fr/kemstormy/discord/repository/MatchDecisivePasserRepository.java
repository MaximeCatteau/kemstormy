package fr.kemstormy.discord.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.MatchDecisivePasser;
import jakarta.persistence.Tuple;

@Repository
public interface MatchDecisivePasserRepository extends JpaRepository<MatchDecisivePasser, Long> {
    @Query(nativeQuery = true, value = "select count(*) as \"assists\", fp.first_name as \"first_name\", fp.last_name as \"last_name\", t.name as \"team_name\" from match_decisive_passer mdp left join match m on mdp.match_id = m.id left join football_player fp on fp.id = mdp.football_player_id left join team t on t.id = fp.club_id where t.league_id = :leagueId group by first_name, last_name, t.name order by \"assists\" desc limit 10")
    List<Tuple> getDecisivePassersLadder(@Param("leagueId") Long leagueId);

    @Query(nativeQuery = true, value = "select count(*) from match_decisive_passer where football_player_id = :playerId")
    Integer countAssistsByPlayer(@Param("playerId") Long playerId);
}
