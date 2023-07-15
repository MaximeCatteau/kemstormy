package fr.kemstormy.discord.service;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kemstormy.discord.model.Ladder;
import fr.kemstormy.discord.model.League;
import fr.kemstormy.discord.model.Team;
import fr.kemstormy.discord.repository.LadderRepository;
import fr.kemstormy.discord.repository.LeagueRepository;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

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

    public List<Ladder> getLaddersByLeague(Long leagueId) {
        return this.ladderRepository.findByLeagueId(leagueId);
    }

    public EmbedBuilder createEmbedLadder(List<Ladder> ladder, Long leagueId) {
        League league = this.leagueRepository.findById(leagueId).orElseThrow();
        EmbedBuilder embedBuilder = new EmbedBuilder();

        BufferedImage image;
        try {
            image = this.createLadderImage(ladder);
            embedBuilder.setImage(image);
            embedBuilder.setColor(null);
        } catch (FontFormatException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return embedBuilder;
    }

    // Method to draw the picture based on the parameters
    private void drawPicture(Graphics graphics, int parameter1, int parameter2, Color fillColor, List<Ladder> ladder) throws FontFormatException, IOException {
        // Register font
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("LigueV-Medium.otf")));

        // List<String> fontsAvailable = Arrays.asList(ge.getAvailableFontFamilyNames());
        
        // Draw a rectangle using the parameters
        graphics.setColor(fillColor);
        graphics.fillRect(parameter1, parameter2, 2400, 1450);

        String hexHeadersColor = "#cdfb0a";
        int r = Integer.valueOf(hexHeadersColor.substring(1, 3), 16);
        int g = Integer.valueOf(hexHeadersColor.substring(3, 5), 16);
        int b = Integer.valueOf(hexHeadersColor.substring(5, 7), 16);

        Color headersColors = new Color(r, g, b);
        graphics.setColor(headersColors);

        graphics.setFont(new Font("Ligue 1 V1 Medium", Font.BOLD, 30));

        // columns headers
        graphics.drawString("#", 30, 50);
        graphics.drawString("Équipe", 110, 50);
        graphics.drawString("Points", 510, 50);
        graphics.drawString("Journées", 760, 50);
        graphics.drawString("Victoires", 1010, 50);
        graphics.drawString("Nuls", 1260, 50);
        graphics.drawString("Défaites", 1510, 50);
        graphics.drawString("Buts marqués", 1760, 50);
        graphics.drawString("Buts encaissés", 2010, 50);
        graphics.drawString("Diff.", 2260, 50);
        
        // Draw other shapes or perform other operations based on the parameters
        int i = 1;
        int heightThreshold = 125;
        graphics.setColor(Color.WHITE);

        for (Ladder l : ladder) {
            Team team = l.getTeam();
            int points = l.getDraws() + l.getVictories() * 3;
            int days = l.getVictories() + l.getDraws() + l.getLoses();
            int diff = l.getScoredGoals() - l.getConcededGoals();

            graphics.setColor(headersColors);

            graphics.drawLine(30, heightThreshold - 50, 2400, heightThreshold - 50);

            graphics.setColor(Color.WHITE);
            
            graphics.drawString("" + i, 30, heightThreshold);

            // logo
            URL url = new URL(team.getLogo());
            Image image = ImageIO.read(url);
            BufferedImage buffImg = ImageIO.read(url);

            graphics.drawImage(image, 110, heightThreshold-40, buffImg.getWidth()/20, buffImg.getHeight()/20, null);
            
            graphics.drawString(team.getName(), 170, heightThreshold);
            graphics.drawString("" + points, 510, heightThreshold);
            graphics.drawString("" + days, 760, heightThreshold);
            graphics.drawString("" + l.getVictories(), 1010, heightThreshold);
            graphics.drawString("" + l.getDraws(), 1260, heightThreshold);
            graphics.drawString("" + l.getLoses(), 1510, heightThreshold);
            graphics.drawString("" + l.getScoredGoals(), 1760, heightThreshold);
            graphics.drawString("" + l.getConcededGoals(), 2010, heightThreshold);
            graphics.drawString("" + diff, 2260, heightThreshold);

            heightThreshold += 75;
            i++;
        }
    }

    private BufferedImage createLadderImage(List<Ladder> ladder) throws FontFormatException, IOException {
		int width = 2400; // Width of the image
        int height = 1450; // Height of the image
        String hexBackgroundColor = "#022149";
        int r = Integer.valueOf(hexBackgroundColor.substring(1, 3), 16);
        int g = Integer.valueOf(hexBackgroundColor.substring(3, 5), 16);
        int b = Integer.valueOf(hexBackgroundColor.substring(5, 7), 16);
        String outputFilePath = "dynamic_picture.png"; // Output file path
        
        // Create a BufferedImage object with the specified width and height
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        // Get the Graphics object from the image
        Graphics graphics = image.getGraphics();
        
        // Set the parameters for the picture
        int parameter1 = 0; // Example parameter 1
        int parameter2 = 0; // Example parameter 2
        Color fillColor = new Color(r, g, b); // Example fill color
        
        // Draw the picture using the parameters
        drawPicture(graphics, parameter1, parameter2, fillColor, ladder);
        
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
