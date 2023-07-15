package fr.kemstormy.discord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.Nationality;

@Repository
public interface NationalityRepository extends JpaRepository<Nationality, Long> {
    
    @Query(nativeQuery = true, value = "select * from nationality where country = :country")
    Nationality getByCountry(@Param("country") String country);
}
