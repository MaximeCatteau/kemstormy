package fr.kemstormy.discord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.MatchDecisivePasser;

@Repository
public interface MatchDecisivePasserRepository extends JpaRepository<MatchDecisivePasser, Long> {
    
}
