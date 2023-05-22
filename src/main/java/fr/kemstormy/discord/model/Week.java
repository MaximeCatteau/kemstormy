package fr.kemstormy.discord.model;

import java.util.List;

import lombok.Data;

@Data
public class Week {
    int number;
    List<Match> matchs;
}
