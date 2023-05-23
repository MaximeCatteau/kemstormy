package fr.kemstormy.discord.model;

import io.micrometer.common.lang.Nullable;
import jakarta.persistence.Entity;
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

    @OneToOne
    @Nullable
    private Stadium stadium;

    /**
     * Quotas of player, each club has several quotas of posts
     * depending on their strategy
     * 
     * 23 players max
     */

    
    private int quotaGoalKeepers;

    private int quotaDefenders;

    private int quotaMidfielders;

    private int quotaForwards;
}
