package fr.kemstormy.discord.service;

import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kemstormy.discord.model.Ladder;
import fr.kemstormy.discord.model.League;
import fr.kemstormy.discord.repository.LadderRepository;
import fr.kemstormy.discord.repository.LeagueRepository;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

@Service
public class LadderService {
    @Autowired
    public LadderRepository ladderRepository;

    @Autowired
    public LeagueRepository leagueRepository;

    public Ladder getLadderByLeagueAndTeam(Long leagueId, Long teamId) {
        return this.ladderRepository.getByLeagueAndTeam(leagueId, teamId);
    }

    public EmbedBuilder createEmbedLadder(Ladder ladder, Long leagueId) {
        League league = this.leagueRepository.findById(leagueId).orElseThrow();
        EmbedBuilder embedBuilder = new EmbedBuilder();

        BufferedImage image = this.createLadderImage();

        embedBuilder.setImage(image);

        return embedBuilder;
    }

    // Method to draw the picture based on the parameters
    private void drawPicture(Graphics graphics, int parameter1, int parameter2, Color fillColor) {
        // Draw a rectangle using the parameters
        graphics.setColor(fillColor);
        graphics.fillRect(parameter1, parameter2, 100, 100);
        
        // Draw other shapes or perform other operations based on the parameters
    }

    private BufferedImage createLadderImage() {
		int width = 500; // Width of the image
        int height = 500; // Height of the image
        String outputFilePath = "dynamic_picture.png"; // Output file path
        
        // Create a BufferedImage object with the specified width and height
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        // Get the Graphics object from the image
        Graphics graphics = image.getGraphics();
        
        // Set the parameters for the picture
        int parameter1 = 100; // Example parameter 1
        int parameter2 = 200; // Example parameter 2
        Color fillColor = Color.RED; // Example fill color
        
        // Draw the picture using the parameters
        drawPicture(graphics, parameter1, parameter2, fillColor);
        
        // Save the image to a file
        try {
            File outputFile = new File(outputFilePath);
            ImageIO.write(image, "png", outputFile);
            System.out.println("Dynamic picture generated successfully!");
        } catch (Exception e) {
            System.out.println("Failed to generate dynamic picture: " + e.getMessage());
        }

        return image;
    }
}
