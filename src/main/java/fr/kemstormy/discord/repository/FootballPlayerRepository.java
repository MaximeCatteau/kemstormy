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

    @Query(nativeQuery = true, value = "select * from football_player fp where club_id = :clubId and post = 0 order by random() limit 1")
    public FootballPlayer getRandomGoalkeeper(@Param("clubId") Long clubId);

    @Query(nativeQuery = true, value = "select * from football_player fp where club_id = :clubId and post = 1 order by random() limit :limit")
    public List<FootballPlayer> getRandomDefenders(@Param("clubId") Long clubId, @Param("limit") int limit);

    @Query(nativeQuery = true, value = "select * from football_player fp where club_id = :clubId and post = 2 order by random() limit :limit")
    public List<FootballPlayer> getRandomMidfielders(@Param("clubId") Long clubId, @Param("limit") int limit);

    @Query(nativeQuery = true, value = "select * from football_player fp where club_id = :clubId and post = 3 order by random() limit :limit")
    public List<FootballPlayer> getRandomAttackers(@Param("clubId") Long clubId, @Param("limit") int limit);

    @Query(nativeQuery = true, value = "select * from football_player fp where owner_id = :ownerId and generation_type = 1")
    public FootballPlayer findByOwnerId(@Param("ownerId") Long ownerId);

    @Query(nativeQuery = true, value = "select * from football_player fp where lower(unaccent(first_name)) = lower(:firstName) and lower(unaccent(last_name)) = lower(unaccent(:lastName));")
    public FootballPlayer findByFirstNameAndLastName(@Param("firstName") String firstName, @Param("lastName") String lastName);
}
