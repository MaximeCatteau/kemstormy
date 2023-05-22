package fr.kemstormy.discord.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public enum EFootballPlayerPost {
    GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD;

    public static EFootballPlayerPost randomPlayerPost() {
        Random random = new Random();
        List<EFootballPlayerPost> posts = Arrays.asList(values());

        return posts.get(random.nextInt(posts.size()));
    }
}
