package fr.kemstormy.discord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.kemstormy.discord.model.League;

public interface LeagueRepository extends JpaRepository<League, Long> {

    @Query(nativeQuery = true, value = "select * from league where lower(name) = :name")
    public League findByLeagueName(@Param("name") String name);
    
}
