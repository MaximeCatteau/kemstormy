package fr.kemstormy.discord.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.kemstormy.discord.model.Stadium;

public interface StadiumRepository extends JpaRepository<Stadium, Long> {
    
}
