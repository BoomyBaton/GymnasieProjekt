package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.Comparator;

// Klass för att representera ett leaderboard-inlägg
class LeaderboardEntry implements Serializable {
    private String playerName;
    private int score;
    
    public LeaderboardEntry(String playerName, int score) {
        this.playerName = playerName;
        this.score = score;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public int getScore() {
        return score;
    }
    
    @Override
    public String toString() {
        return playerName + ": " + score;
    }
}

// Klass för att hantera leaderboard-funktionalitet
class LeaderboardManager {
    private static final String SERVER_URL = "http://localhost:8080/leaderboard"; // Ändra till din server URL
    
    // Skicka poäng till servern
    public static boolean submitScore(String playerName, int score) {
        try {
            URL url = new URL(SERVER_URL + "/submit");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            // Skapa JSON-data
            String jsonData = "{\"playerName\":\"" + playerName + "\",\"score\":" + score + "}";
            
            // Skicka data
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonData.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            // Kontrollera svar
            int responseCode = conn.getResponseCode();
            return responseCode == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Hämta top 10 från servern
    public static ArrayList<LeaderboardEntry> getTopScores() {
        ArrayList<LeaderboardEntry> scores = new ArrayList<>();
        
        try {
            URL url = new URL(SERVER_URL + "/top");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            // Läs svar
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                
                // Enkel parsning av JSON-svar (i en riktig implementation bör du använda ett JSON-bibliotek)
                String jsonResp = response.toString();
                // Denna parsing är mycket grundläggande och skulle ersättas med ett JSON-bibliotek
                if (jsonResp.contains("[") && jsonResp.contains("]")) {
                    String[] entries = jsonResp.substring(jsonResp.indexOf("[") + 1, jsonResp.lastIndexOf("]")).split("},");
                    for (String entry : entries) {
                        if (entry.contains("playerName") && entry.contains("score")) {
                            String name = entry.substring(entry.indexOf("playerName") + 13);
                            name = name.substring(0, name.indexOf("\""));
                            
                            String scoreStr = entry.substring(entry.indexOf("score") + 7);
                            if (scoreStr.contains("}")) {
                                scoreStr = scoreStr.substring(0, scoreStr.indexOf("}"));
                            }
                            int score = Integer.parseInt(scoreStr.trim());
                            
                            scores.add(new LeaderboardEntry(name, score));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Om något går fel, lägg till dummy-data
            loadLocalScores(scores);
        }
        
        if (scores.isEmpty()) {
            loadLocalScores(scores);
        }
        
        // Sortera efter poäng (högst först)
        Collections.sort(scores, Comparator.comparing(LeaderboardEntry::getScore).reversed());
        return scores;
    }
    
    // Hämta lokala poäng om servern inte är tillgänglig
    private static void loadLocalScores(ArrayList<LeaderboardEntry> scores) {
        try {
            File file = new File("leaderboard.dat");
            if (file.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    @SuppressWarnings("unchecked")
                    ArrayList<LeaderboardEntry> savedScores = (ArrayList<LeaderboardEntry>) ois.readObject();
                    scores.addAll(savedScores);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Spara poäng lokalt om servern inte är tillgänglig
    public static void saveLocalScore(String playerName, int score) {
        ArrayList<LeaderboardEntry> scores = new ArrayList<>();
        loadLocalScores(scores);
        
        scores.add(new LeaderboardEntry(playerName, score));
        
        // Sortera efter poäng (högst först)
        Collections.sort(scores, Comparator.comparing(LeaderboardEntry::getScore).reversed());
        
        // Spara bara top 10
        if (scores.size() > 10) {
            scores = new ArrayList<>(scores.subList(0, 10));
        }
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("leaderboard.dat"))) {
            oos.writeObject(scores);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// Klass för att visa leaderboard-dialog
class LeaderboardDialog extends JDialog {
    private JList<LeaderboardEntry> leaderboardList;
    private DefaultListModel<LeaderboardEntry> listModel;
    
    public LeaderboardDialog(JFrame parent) {
        super(parent, "Leaderboard", true);
        
        setSize(300, 400);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        
        // Skapa lista
        listModel = new DefaultListModel<>();
        leaderboardList = new JList<>(listModel);
        leaderboardList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                LeaderboardEntry entry = (LeaderboardEntry) value;
                label.setText((index + 1) + ". " + entry.getPlayerName() + " - " + entry.getScore());
                return label;
            }
        });
        
        // Lägga till scrollpane
        JScrollPane scrollPane = new JScrollPane(leaderboardList);
        add(scrollPane, BorderLayout.CENTER);
        
        // Lägga till stängknapp
        JButton closeButton = new JButton("Stäng");
        closeButton.addActionListener(e -> dispose());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Lägg till titel
        JLabel titleLabel = new JLabel("Top 10 Scores", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        
        // Ladda poäng
        loadScores();
    }
    
    private void loadScores() {
        listModel.clear();
        ArrayList<LeaderboardEntry> scores = LeaderboardManager.getTopScores();
        for (LeaderboardEntry entry : scores) {
            listModel.addElement(entry);
        }
    }
}

// Dialogruta för inmatning av spelarnamn
class NameInputDialog extends JDialog {
    private JTextField nameField;
    private String playerName = null;
    
    public NameInputDialog(JFrame parent) {
        super(parent, "Ange ditt namn", true);
        
        setSize(300, 150);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        
        // Skapa panel för namn
        JPanel namePanel = new JPanel(new FlowLayout());
        namePanel.add(new JLabel("Ditt namn: "));
        nameField = new JTextField(15);
        namePanel.add(nameField);
        add(namePanel, BorderLayout.CENTER);
        
        // Skapa knappanel
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            if (!nameField.getText().trim().isEmpty()) {
                playerName = nameField.getText().trim();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Vänligen ange ditt namn", "Fel", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Lägg till instruktion
        JLabel instructionLabel = new JLabel("Ange ditt namn för leaderboard", JLabel.CENTER);
        add(instructionLabel, BorderLayout.NORTH);
    }
    
    public String getPlayerName() {
        return playerName;
    }
}

public class gametest extends JPanel implements ActionListener, KeyListener {
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
    JButton leaderboardButton;
    
    // Spelarnamn för leaderboard
    private String playerName = "Player";
    
    // Referens till föräldraramen
    private JFrame parentFrame;

    public gametest() {
        timer = new Timer(10, this); // Speluppdatering var 10ms
        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        createPlatforms();
        timer.start();

        // Skapa och konfigurera restart-knappen
        restartButton = new JButton("Restart");
        restartButton.setBounds(150, 300, 100, 30); // Placera knappen på panelen
        restartButton.addActionListener(e -> {
            restartGame();
            // Restore focus to the panel after button click
            requestFocusInWindow();
        }); 	
        restartButton.setVisible(false); // Dölj knappen initialt
        
        // Skapa och konfigurera leaderboard-knappen
        leaderboardButton = new JButton("Leaderboard");
        leaderboardButton.setBounds(260, 300, 120, 30); // Placera knappen på panelen
        leaderboardButton.addActionListener(e -> {
            showLeaderboard();
            // Restore focus to the panel after button click
            requestFocusInWindow();
        });
        leaderboardButton.setVisible(false); // Dölj knappen initialt
        
        setLayout(null);
        add(restartButton);
        add(leaderboardButton);
    }
    
    // Metod för att sätta föräldrareferensen
    public void setParentFrame(JFrame frame) {
        this.parentFrame = frame;
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
        g.drawString("Spelare: " + playerName, 10, 30);

        // Kontrollera om spelet är över
        if (isGameOver) {
            g.setColor(Color.WHITE);
            g.drawString("Game Over! Poäng: " + score, 200, 250);
            restartButton.setVisible(true); // Visa knappen om spelet är över
            leaderboardButton.setVisible(true); // Visa leaderboard-knappen om spelet är över
        } else {
            restartButton.setVisible(false); // Dölj knappen om spelet inte är över
            leaderboardButton.setVisible(false); // Dölj leaderboard-knappen om spelet inte är över
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (isGameOver) return;

        playerY += playerVelocityY; // Uppdatera spelarens y-position baserat på hastigheten

        // Kontrollera om spelaren faller ner (game over)
        if (playerY > 500) {
            gameOver();
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
    
    // Metod för att visa leaderboard
    private void showLeaderboard() {
        LeaderboardDialog dialog = new LeaderboardDialog(parentFrame);
        dialog.setVisible(true);
    }
    
    // Metod för att hantera game over
    private void gameOver() {
        isGameOver = true;
        
        // Försök först att skicka poäng till servern
        boolean scoreSaved = LeaderboardManager.submitScore(playerName, score);
        
        // Om det misslyckades, spara lokalt
        if (!scoreSaved) {
            LeaderboardManager.saveLocalScore(playerName, score);
        }
    }

    public void restartGame() {
        // Fråga efter spelarnamn
        NameInputDialog dialog = new NameInputDialog(parentFrame);
        dialog.setVisible(true);
        
        // Om användaren angett ett namn, använd det
        if (dialog.getPlayerName() != null) {
            playerName = dialog.getPlayerName();
        }
        
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
        gametest game = new gametest();
        game.setParentFrame(frame); // Sätt föräldrareferensen
        
        // Fråga efter spelarnamn
        frame.setVisible(true); // Måste göra ramen synlig först
        NameInputDialog dialog = new NameInputDialog(frame);
        dialog.setVisible(true);
        
        // Om användaren angett ett namn, använd det
        if (dialog.getPlayerName() != null) {
            game.playerName = dialog.getPlayerName();
        }
        
        frame.setTitle("Gymnasieprojekt - Java Game");
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(game);
        frame.setLocationRelativeTo(null); // Centrera fönstret
    }
}