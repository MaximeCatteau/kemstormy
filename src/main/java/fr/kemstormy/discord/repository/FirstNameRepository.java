package fr.kemstormy.discord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.FirstName;

@Repository
public interface FirstNameRepository extends JpaRepository<FirstName, Long> {
    
    @Query(nativeQuery = true, value = "select fn.firstname from first_name fn left join first_name_nationalities fnn on fnn.first_name_id = fn.id where fnn.nationalities_id = :country order by random() limit 1")
    String getRandomFirstNameByCountry(@Param("country") Long country);
}
