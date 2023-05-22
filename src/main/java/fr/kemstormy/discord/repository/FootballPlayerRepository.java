package fr.kemstormy.discord.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.FootballPlayer;

@Repository
public interface FootballPlayerRepository extends JpaRepository<FootballPlayer, Long> {    
    @Query(nativeQuery = true, value = "delete from football_player where generation_type = 0")
    public int deleteAllBotPlayers();

    @Query(nativeQuery = true, value = "select * from football_player fp where club_id is null order by random() limit 1")
    public FootballPlayer getFootballPlayerWithNoClub();

    @Query(nativeQuery = true, value = "select * from football_player fp where club_id = :clubId and post = :post")
    public List<FootballPlayer> getFootballPlayersByTeamAndPost(@Param("clubId") Long clubId, @Param("post") int post);
}
