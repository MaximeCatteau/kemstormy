package fr.kemstormy.discord.model;

import io.micrometer.common.lang.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String logo;

    @OneToOne(fetch = FetchType.LAZY)
    @Nullable
    private Stadium stadium;

    @OneToOne(fetch = FetchType.LAZY)
    @Nullable
    private League league;

    @Nullable
    private String homeJersey;

    @Nullable
    private String awayJersey;
    /**
     * Quotas of player, each club has several quotas of posts
     * depending on their strategy
     * 
     * 23 players max
     */

    private int quotaGoalKeepers = 1;

    private int quotaDefenders = 0;

    private int quotaMidfielders = 0;

    private int quotaForwards = 0;

    @Nullable
    private String mainColor;

    @Nullable
    private String secondaryColor;

    private int budget = 0;
}
