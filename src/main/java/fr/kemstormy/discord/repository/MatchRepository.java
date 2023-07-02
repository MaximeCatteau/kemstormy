package fr.kemstormy.discord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.Match;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    @Query(nativeQuery = true, value = "select * from match where status = 0 order by random() limit 1")
    Match getRandomMatch();

    @Query(nativeQuery = true, value = "select * from match where status = 0 order by date asc limit 1")
    Match getNextMatchToPlay();

    @Query(nativeQuery = true, value = "select count(*) from match where status = 0 and competition_id = :leagueId")
    Integer countRemainingLeagueMatchs(@Param("leagueId") Long leagueId);
}
