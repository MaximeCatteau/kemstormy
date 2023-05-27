package fr.kemstormy.discord.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table
public class Ladder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne
    League league;

    @OneToOne
    Team team;

    int victories = 0;

    int draws = 0;

    int loses = 0;

    int scoredGoals = 0;

    int concededGoals = 0;
}
