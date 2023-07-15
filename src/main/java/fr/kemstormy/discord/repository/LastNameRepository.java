package fr.kemstormy.discord.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.LastName;

@Repository
public interface LastNameRepository extends JpaRepository<LastName, Long> {
 
    @Query(nativeQuery = true, value = "select fn.lastname from last_name fn left join last_name_nationalities fnn on fnn.last_name_id = fn.id where fnn.nationalities_id = :country order by random() limit 1")
    String getRandomLastNameByCountry(@Param("country") Long country);
}
