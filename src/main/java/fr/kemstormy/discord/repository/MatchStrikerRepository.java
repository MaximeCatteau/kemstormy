package fr.kemstormy.discord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.MatchStriker;

@Repository
public interface MatchStrikerRepository extends JpaRepository<MatchStriker, Long> {
    
}
