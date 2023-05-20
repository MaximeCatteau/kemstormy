package fr.kemstormy.discord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long>{
    
}
