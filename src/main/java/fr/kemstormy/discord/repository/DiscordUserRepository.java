package fr.kemstormy.discord.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.kemstormy.discord.model.DiscordUser;

@Repository
public interface DiscordUserRepository extends JpaRepository<DiscordUser, Long>{
    @Query(nativeQuery = true, value = "SELECT * from discord_user WHERE discord_id = :discordId")
    List<DiscordUser> findByDiscordId(@Param("discordId") String discordId);
}
