package fr.kemstormy.discord.service;

import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kemstormy.discord.model.League;
import fr.kemstormy.discord.model.Team;
import fr.kemstormy.discord.repository.LeagueRepository;
import fr.kemstormy.discord.repository.MatchDecisivePasserRepository;
import fr.kemstormy.discord.repository.MatchStrikerRepository;
import fr.kemstormy.discord.repository.TeamRepository;
import fr.kemstormy.discord.resource.PasserLadderResource;
import fr.kemstormy.discord.resource.StrikerLadderResource;
import jakarta.persistence.Tuple;

@Service
public class MatchDecisivePassersService {
    @Autowired
    private MatchDecisivePasserRepository matchDecisivePasserRepository;

    @Autowired 
    private LeagueRepository leagueRepository;

    @Autowired
    private TeamRepository teamRepository;

    public List<PasserLadderResource> getDecisivePassersLadder(Long leagueId) {
        List<Tuple> tuples = this.matchDecisivePasserRepository.getDecisivePassersLadder(leagueId);
        List<PasserLadderResource> passerLadderResources = new ArrayList<>();
        
        try {
            for (Tuple t : tuples) {
                Set<Entry<String, Object>> entry = t.getElements().stream().map(e -> new HashMap.SimpleEntry<String, Object>(e.getAlias(), t.get(e))).collect(Collectors.toSet());
                PasserLadderResource res = new PasserLadderResource();

                Iterator it = entry.iterator();
                while (it.hasNext()) {
                    Map.Entry ent = (Map.Entry) it.next();

                    if (((String) ent.getKey()).equals("assists")) {
                        res.setAssists((Long) ent.getValue());
                    } else if (((String) ent.getKey()).equals("first_name")) {
                        res.setFirstName((String) ent.getValue());
                    } else if (((String) ent.getKey()).equals("last_name")) {
                        res.setLastName((String) ent.getValue());
                    } else if (((String) ent.getKey()).equals("team_name")) {
                        res.setTeamName((String) ent.getValue());
                    }
                }

                passerLadderResources.add(res);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return passerLadderResources;
    }

    public EmbedBuilder createEmbedDecisivePassersLadder(List<PasserLadderResource> passerLadder, Long leagueId) {
        League league = this.leagueRepository.findById(leagueId).orElseThrow();
        EmbedBuilder embedBuilder = new EmbedBuilder();

        BufferedImage image;
        try {
            image = this.createPassersLadderImmage(passerLadder);
            embedBuilder.setImage(image);
        } catch (FontFormatException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return embedBuilder;
    }

    private BufferedImage createPassersLadderImmage(List<PasserLadderResource> passerLadder) throws FontFormatException, IOException {
		int width = 1300; // Width of the image
        int height = 1000; // Height of the image
        String hexBackgroundColor = "#022149";
        int r = Integer.valueOf(hexBackgroundColor.substring(1, 3), 16);
        int g = Integer.valueOf(hexBackgroundColor.substring(3, 5), 16);
        int b = Integer.valueOf(hexBackgroundColor.substring(5, 7), 16);
        String outputFilePath = "passers_ladder.png"; // Output file path
        
        // Create a BufferedImage object with the specified width and height
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        // Get the Graphics object from the image
        Graphics graphics = image.getGraphics();
        
        // Set the parameters for the picture
        int parameter1 = 0; // Example parameter 1
        int parameter2 = 0; // Example parameter 2
        Color fillColor = new Color(r, g, b); // Example fill color
        
        // Draw the picture using the parameters
        drawPicture(graphics, parameter1, parameter2, fillColor, passerLadder);
        
        // Save the image to a file
        try {
            File outputFile = new File(outputFilePath);
            ImageIO.write(image, "png", outputFile);
            System.out.println("Ladder pic generated successfully!");
        } catch (Exception e) {
            System.out.println("Failed to generate dynamic picture: " + e.getMessage());
        }

        return image;
    }

    private void drawPicture(Graphics graphics, int parameter1, int parameter2, Color fillColor, List<PasserLadderResource> ladder) throws FontFormatException, IOException {
        // Register font
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("LigueV-Medium.otf")));

        // List<String> fontsAvailable = Arrays.asList(ge.getAvailableFontFamilyNames());
        
        // Draw a rectangle using the parameters
        graphics.setColor(fillColor);
        graphics.fillRect(parameter1, parameter2, 1300, 1000);

        String hexHeadersColor = "#cdfb0a";
        int r = Integer.valueOf(hexHeadersColor.substring(1, 3), 16);
        int g = Integer.valueOf(hexHeadersColor.substring(3, 5), 16);
        int b = Integer.valueOf(hexHeadersColor.substring(5, 7), 16);

        Color headersColors = new Color(r, g, b);
        graphics.setColor(headersColors);

        graphics.setFont(new Font("Ligue 1 V1 Medium", Font.BOLD, 30));

        // columns headers
        graphics.drawString("#", 30, 50);
        graphics.drawString("Joueur", 110, 50);
        graphics.drawString("Équipe", 510, 50);
        graphics.drawString("Passes décisives", 1060, 50);
        
        // Draw other shapes or perform other operations based on the parameters
        int i = 1;
        int heightThreshold = 125;
        graphics.setColor(Color.WHITE);

        for (PasserLadderResource l : ladder) {
            Team team = this.teamRepository.findByTeamName(l.getTeamName());

            graphics.setColor(headersColors);

            graphics.drawLine(30, heightThreshold - 50, 1300, heightThreshold - 50);

            graphics.setColor(Color.WHITE);
            
            graphics.drawString("" + i, 30, heightThreshold);
            graphics.drawString(l.getFirstName() + " " + l.getLastName(), 110, heightThreshold);

            // logo
            URL url = new URL(team.getLogo());
            Image image = ImageIO.read(url);
            BufferedImage buffImg = ImageIO.read(url);

            graphics.drawImage(image, 510, heightThreshold-40, buffImg.getWidth()/20, buffImg.getHeight()/20, null);
            
            graphics.drawString(team.getName(), 570, heightThreshold);

            graphics.drawString("" + l.getAssists(), 1060, heightThreshold);

            heightThreshold += 75;
            i++;
        }
    }
}
