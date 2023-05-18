package fr.kemstormy.discord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.FootballPlayer;

@Repository
public interface FootballPlayerRepository extends JpaRepository<FootballPlayer, Long> {
    
}
