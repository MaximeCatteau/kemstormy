package fr.kemstormy.discord.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kemstormy.discord.model.DiscordUser;
import fr.kemstormy.discord.repository.DiscordUserRepository;
import lombok.Data;

@Data
@Service
public class DiscordUserService {
    @Autowired
    private DiscordUserRepository discordUserRepository;

    public List<DiscordUser> getAllDiscordUsers() {
        return StreamSupport.stream(this.discordUserRepository.findAll().spliterator(), false).collect(Collectors.toList());
    }

    public DiscordUser createOrUpdateDiscordUser(DiscordUser discordUser) {
        List<DiscordUser> du = this.discordUserRepository.findByDiscordId(discordUser.getDiscordId());
        if (du != null && du.size() > 0) {
            return du.get(0);
        }
        return this.discordUserRepository.save(discordUser);
    }
}
