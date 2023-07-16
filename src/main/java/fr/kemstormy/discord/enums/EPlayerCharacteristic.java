package fr.kemstormy.discord.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public enum EPlayerCharacteristic {
    PASS, LONG_PASS, SHOT, LONG_SHOT, INTERCEPTION, SHOT_STOPPING, REFLEXES, CORNERS, FREEKICK, PENALTY, TACKLE, DRIBBLE;
    public static EPlayerCharacteristic randomPlayerCharacteristic() {
        Random random = new Random();
        List<EPlayerCharacteristic> characteristics = Arrays.asList(values());

        return characteristics.get(random.nextInt(characteristics.size()));
    }
}
