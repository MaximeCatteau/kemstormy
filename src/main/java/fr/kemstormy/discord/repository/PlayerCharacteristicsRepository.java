package fr.kemstormy.discord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.PlayerCharacteristics;

@Repository
public interface PlayerCharacteristicsRepository extends JpaRepository<PlayerCharacteristics, Long> {
    
}
