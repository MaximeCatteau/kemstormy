package fr.kemstormy.discord.resource;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import fr.kemstormy.discord.model.FootballPlayer;
import fr.kemstormy.discord.model.PlayerCharacteristics;
import fr.kemstormy.discord.model.Stadium;
import lombok.Data;

@Data
public class XpData {

    private Map<Integer, Integer> loadXpPerLevel() throws IOException {
        Map<Integer, Integer> xpPerLevel = new HashMap<>();
        List<String> lvls = Files.readAllLines(Paths.get("gamedata\\xp_steps.txt"), StandardCharsets.UTF_8);

        int lvl = 1;
        for (String step : lvls) {
            xpPerLevel.put(lvl, Integer.parseInt(step));
            lvl++;
        }

        return xpPerLevel;
    }

    /***
     * STADIUM XP PROCESSING
     * @throws IOException
     */

     public boolean willStadiumUp(Stadium s, int acquiredExperience) throws IOException {
        int stadiumLevel = s.getLevel();
        int stadiumExperience = s.getExperience();

        int nextStadiumLevel = stadiumLevel + 1;
        int addedExperience = stadiumExperience + acquiredExperience;

        Map<Integer, Integer> xpMapData = this.loadXpPerLevel();

        int necessaryXp = xpMapData.get(nextStadiumLevel);

        return addedExperience >= necessaryXp;
     }

     /***
      * PLAYER XP PROCESSING
      */

      public boolean willPlayerUp(FootballPlayer fp, int acquiredExperience) throws IOException {
        int footballPlayerLevel = fp.getLevel();
        int footballPlayerExperience = fp.getPlayerCharacteristics().getExperience();

        int nextFootballPlayerLevel = footballPlayerLevel + 1;
        int addedExperience = footballPlayerExperience + acquiredExperience;

        Map<Integer, Integer> xpMapData = this.loadXpPerLevel();

        int necessaryXp = xpMapData.get(nextFootballPlayerLevel);

        return addedExperience >= necessaryXp;
      }
}
