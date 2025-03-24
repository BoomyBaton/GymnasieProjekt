package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class Game extends JPanel implements ActionListener, KeyListener {
    Timer timer;
    int playerX, playerY, playerVelocityY = 0;
    boolean onPlatform = false;
    ArrayList<Rectangle> platforms = new ArrayList<>();
    // Track the ID of each platform to uniquely identify them
    ArrayList<Integer> platformIds = new ArrayList<>();
    // Track which platform IDs the player has already hit
    ArrayList<Integer> hitPlatformIds = new ArrayList<>();
    int nextPlatformId = 0;
    int score = 0;
    boolean isGameOver = false;

    // Variabler för sidledsrörelse
    boolean moveLeft = false;
    boolean moveRight = false;

    JButton restartButton;

    public Game() {
        timer = new Timer(10, this); // Speluppdatering var 10ms
        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        createPlatforms();
        timer.start();

        // Skapa och konfigurera restart-knappen
        restartButton = new JButton("Restart");
        restartButton.setBounds(200, 300, 100, 30); // Placera knappen på panelen
        restartButton.addActionListener(e -> {
            restartGame();
            // Restore focus to the panel after button click
            requestFocusInWindow();
        }); 	
        restartButton.setVisible(false); // Dölj knappen initialt
        setLayout(null);
        add(restartButton);
    }

    public void createPlatforms() {
        platforms.clear();
        platformIds.clear();
        hitPlatformIds.clear();
        nextPlatformId = 0;

        // Skapa första plattformen där spelaren ska börja
        Rectangle firstPlatform = new Rectangle(200, 400, 100, 10);
        platforms.add(firstPlatform);
        platformIds.add(nextPlatformId++);

        // Startposition för spelaren på den första plattformen
        playerX = firstPlatform.x + firstPlatform.width / 2 - 10; // Placera spelaren i mitten
        playerY = firstPlatform.y - 20; // Placera spelaren ovanpå plattformen

        // Skapa fler slumpmässiga plattformar
        for (int i = 1; i < 5; i++) {
            platforms.add(new Rectangle(new Random().nextInt(300), 400 - i * 100, 100, 10));
            platformIds.add(nextPlatformId++);
        }
    }

    public void paint(Graphics g) {
        super.paint(g);

        // Bakgrund
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 500, 500);

        // Spelare
        g.setColor(Color.RED);
        g.fillRect(playerX, playerY, 20, 20);

        // Plattformar
        for (int i = 0; i < platforms.size(); i++) {
            Rectangle platform = platforms.get(i);
            int platformId = platformIds.get(i);
            
            // Färga plattformen baserat på om den har besökts
            if (hitPlatformIds.contains(platformId)) {
                g.setColor(Color.GRAY); // Besökta plattformar är gråa
            } else {
                g.setColor(Color.GREEN); // Obesökta plattformar är gröna
            }
            
            g.fillRect(platform.x, platform.y, platform.width, platform.height);
        }

        // Poäng
        g.setColor(Color.WHITE);
        g.drawString("Höjd: " + score, 10, 10);

        // Kontrollera om spelet är över
        if (isGameOver) {
            g.setColor(Color.WHITE);
            g.drawString("Game Over! Poäng: " + score, 200, 250);
            restartButton.setVisible(true); // Visa knappen om spelet är över
        } else {
            restartButton.setVisible(false); // Dölj knappen om spelet inte är över
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (isGameOver) return;

        playerY += playerVelocityY; // Uppdatera spelarens y-position baserat på hastigheten

        // Kontrollera om spelaren faller ner (game over)
        if (playerY > 500) {
            isGameOver = true;
        }

        // Anropa funktionen som gör att spelaren studsar om den landar på en plattform
        bounceIfOnPlatform();

        // Hantera sidledsrörelse om vänster- eller högerpilen hålls in
        if (moveLeft && playerX > 0) {
            playerX -= 10; // Flytta vänster
        }
        if (moveRight && playerX < 480) {
            playerX += 10; // Flytta höger
        }

        // Flytta plattformarna och skapa nya
        for (int i = 0; i < platforms.size(); i++) {
            Rectangle platform = platforms.get(i);
            platform.y += 1;
            if (platform.y > 500) {
                // När en plattform går utanför skärmen, skapa en ny
                platform.y = -10;
                platform.x = new Random().nextInt(400);
                
                // Ge plattformen ett nytt ID
                platformIds.set(i, nextPlatformId++);
            }
        }

        repaint(); // Rita om spelet
    }

    public void bounceIfOnPlatform() {
        // Kontrollera om spelaren landar på en plattform
        for (int i = 0; i < platforms.size(); i++) {
            Rectangle platform = platforms.get(i);
            int platformId = platformIds.get(i);
            
            if (playerY + 20 >= platform.y && playerY + 20 <= platform.y + 10 &&
                playerX + 20 > platform.x && playerX < platform.x + platform.width) {
                onPlatform = true;
                playerVelocityY = -15; // Justera detta värde för att ändra hoppets höjd
                
                // Kolla om denna plattform inte har besökts tidigare
                if (!hitPlatformIds.contains(platformId)) {
                    score++; // Öka poängen endast för nya plattformar
                    hitPlatformIds.add(platformId); // Markera plattformen som besökt
                }
            }
        }

        // Om inte på plattform, applicera gravitation
        if (!onPlatform) {
            playerVelocityY += 1; // Gravitation
        }

        onPlatform = false; // Återställ för nästa kontroll
    }

    public void restartGame() {
        // Reset game state variables
        isGameOver = false;
        score = 0;
        playerVelocityY = 0;
        moveLeft = false;
        moveRight = false;
        onPlatform = false;
        
        // Recreate all platforms and reset player position
        createPlatforms();
        
        // Make sure the timer is running
        if (!timer.isRunning()) {
            timer.start();
        }
        
        repaint();
    }

    // Hantera knapptryckningar för att flytta spelaren
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            moveLeft = true; // Flytta vänster när vänsterpilen hålls in
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            moveRight = true; // Flytta höger när högerpilen hålls in
        }
    }

    // Släpp tangent
    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            moveLeft = false; // Sluta flytta vänster när vänsterpilen släpps
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            moveRight = false; // Sluta flytta höger när högerpilen släpps
        }
    }

    @Override
    public void keyTyped(KeyEvent e) { }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        Game game = new Game();
        frame.setTitle("Gymnasieprojekt - Java Game");
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(game);
        frame.setVisible(true);
    }
}