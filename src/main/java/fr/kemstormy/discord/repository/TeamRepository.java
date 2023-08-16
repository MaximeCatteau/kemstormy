package fr.kemstormy.discord.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    @Query(nativeQuery = true, value = "select * from team where lower(name) = lower(:name)")
    public Team findByTeamName(@Param("name") String name);

    @Query(nativeQuery = true, value = "select * from team t where league_id = 1 and quota_goal_keepers > (select count(*) from football_player fp where club_id = t.id and post = 0) order by random() limit 1")
    public Team getMatchingTeamForGoalkeepers();

    @Query(nativeQuery = true, value = 
        "select * from team t " + 
        "where league_id = 1 " + 
        "and (" + 
            "quota_goal_keepers > (" + 
                "select count(*) from football_player fp " + 
                "where club_id = t.id " + 
                "and post = 0" + 
            ") " + 
            "or (" + 
                "quota_goal_keepers > (" + 
                    "select count(*) from football_player fp2 " + 
                    "where club_id = t.id " + 
                    "and post = 0 " + 
                    "and fp2.generation_type != 0" + 
                ")" + 
            ") " + 
        ") " +
        "order by random() limit 1")
    public Team getMatchingTeamForRealGoalkeepers();

    @Query(nativeQuery = true, value = "select * from team t where league_id = 1 and quota_defenders > (select count(*) from football_player fp where club_id = t.id and post = 1) order by random() limit 1")
    public Team getMatchingTeamForDefenders();

    @Query(nativeQuery = true, value = 
        "select * from team t " + 
        "where league_id = 1 " + 
        "and (" + 
            "quota_defenders > (" + 
                "select count(*) from football_player fp " + 
                "where club_id = t.id " + 
                "and post = 1" + 
            ") " + 
            "or (" + 
                "quota_defenders > (" + 
                    "select count(*) from football_player fp2 " + 
                    "where club_id = t.id " + 
                    "and post = 1 " + 
                    "and fp2.generation_type != 0" + 
                ")" + 
            ") " + 
        ") " +
        "order by random() limit 1")
    public Team getMatchingTeamForRealDefenders();

    @Query(nativeQuery = true, value = "select * from team t where league_id = 1 and quota_midfielders > (select count(*) from football_player fp where club_id = t.id and post = 2) order by random() limit 1")
    public Team getMatchingTeamForMidfielders();

    @Query(nativeQuery = true, value = 
        "select * from team t " + 
        "where league_id = 1 " + 
        "and (" + 
            "quota_midfielders > (" + 
                "select count(*) from football_player fp " + 
                "where club_id = t.id " + 
                "and post = 2" + 
            ") " + 
            "or (" + 
                "quota_midfielders > (" + 
                    "select count(*) from football_player fp2 " + 
                    "where club_id = t.id " + 
                    "and post = 2 " + 
                    "and fp2.generation_type != 0" + 
                ")" + 
            ") " + 
        ") " +
        "order by random() limit 1")
    public Team getMatchingTeamForRealMidfielders();

    @Query(nativeQuery = true, value = "select * from team t where league_id = 1 and quota_forwards > (select count(*) from football_player fp where club_id = t.id and post = 3) order by random() limit 1")
    public Team getMatchingTeamForForwards();

    @Query(nativeQuery = true, value = 
        "select * from team t " + 
        "where league_id = 1 " + 
        "and (" + 
            "quota_forwards > (" + 
                "select count(*) from football_player fp " + 
                "where club_id = t.id " + 
                "and post = 3" + 
            ") " + 
            "or (" + 
                "quota_forwards > (" + 
                    "select count(*) from football_player fp2 " + 
                    "where club_id = t.id " + 
                    "and post = 3 " + 
                    "and fp2.generation_type != 0" + 
                ")" + 
            ") " + 
        ") " +
        "order by random() limit 1")
    public Team getMatchingTeamForRealForwards();

    @Query(nativeQuery = true, value = "select * from team where league_id  = :leagueId")
    public List<Team> getLeagueTeams(@Param("leagueId") Long leagueId);
    
    @Query(nativeQuery = true, value = "select * from team t order by random() limit(2)")
    public List<Team> getTwoRandomTeamsForMatch();

    @Query(nativeQuery = true, value = "select sum(quota_goal_keepers) from team t where league_id = :leagueId")
    int getGoalkeepersQuotaSumForTeams(@Param("leagueId") Long leagueId);

    @Query(nativeQuery = true, value = "select sum(quota_defenders) from team t where league_id = :leagueId")
    int getDefendersQuotaSumForTeams(@Param("leagueId") Long leagueId);
    
    @Query(nativeQuery = true, value = "select sum(quota_midfielders) from team t where league_id = :leagueId")
    int getMidfieldersQuotaSumForTeams(@Param("leagueId") Long leagueId);
    
    @Query(nativeQuery = true, value = "select sum(quota_forwards) from team t where league_id = :leagueId")
    int getForwardsQuotaSumForTeams(@Param("leagueId") Long leagueId);
}
