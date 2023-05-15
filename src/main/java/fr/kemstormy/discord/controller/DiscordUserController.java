package fr.kemstormy.discord.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.kemstormy.discord.model.DiscordUser;
import fr.kemstormy.discord.service.DiscordUserService;

@RestController
@RequestMapping("/api")
public class DiscordUserController {
    @Autowired
    private DiscordUserService discordUserService;

    @GetMapping("/discord/users")
    public ResponseEntity<List<DiscordUser>> getAllDiscordUsers() {
        return new ResponseEntity<List<DiscordUser>>(this.discordUserService.getAllDiscordUsers(), HttpStatus.OK);
    }

    @PostMapping("/discord/user")
    public ResponseEntity<DiscordUser> createOrUpdateDiscordUser(@RequestParam String discordId) {
        DiscordUser newUser = new DiscordUser();
        newUser.setDiscordId(discordId);

        return new ResponseEntity<DiscordUser>(this.discordUserService.createOrUpdateDiscordUser(newUser), HttpStatus.OK);
    }
}
