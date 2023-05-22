package fr.kemstormy.discord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    @Query(nativeQuery = true, value = "select * from team where lower(name) = :name")
    public Team findByTeamName(@Param("name") String name);

    @Query(nativeQuery = true, value = "select * from team t where quota_defenders > (select count(*) from football_player fp where club_id = t.id and post = :post) order by random() limit 1")
    public Team getMatchingTeam(@Param("post") int post);
}
