package fr.kemstormy.discord.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.kemstormy.discord.model.League;

public interface LeagueRepository extends JpaRepository<League, Long> {
    
}
