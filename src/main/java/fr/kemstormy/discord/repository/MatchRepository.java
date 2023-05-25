package fr.kemstormy.discord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.Match;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    @Query(nativeQuery = true, value = "select * from match order by random() limit 1")
    Match getRandomMatch();
}
