package game;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import javax.swing.*;

public class Game extends JPanel implements ActionListener, KeyListener, Serializable {
    private static final long serialVersionUID = 1L;

    private Timer timer;
    private TimerTask gameTask;
    private int playerX, playerY, playerVelocityY = 0;
    private boolean onPlatform = false;
    private ArrayList<Rectangle> platforms = new ArrayList<>();
    private ArrayList<Integer> platformIds = new ArrayList<>();
    private ArrayList<Integer> hitPlatformIds = new ArrayList<>();
    private int nextPlatformId = 0;
    private int score = 0;
    private boolean isGameOver = false;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private JButton restartButton;
    private ArrayList<ScoreEntry> highScores = new ArrayList<>();

    private static final String MASTER_KEY = "$2a$10$C/WPAWTBtUBx4hBM2.rEcOospCUVMcgeXhDKoCt3cYK0Xms5l.mr6";
    private static final String BIN_ID = "67f7ac438960c979a58236c7";

    public Game() {
        setLayout(null);
        
        // Create components first
        restartButton = new JButton("Restart");
        restartButton.setBounds(390, 10, 100, 30);
        restartButton.addActionListener(e -> {
            restartGame();
            requestFocusInWindow();
        });
        add(restartButton);
        
        // Then setup game logic
        timer = new Timer();
        gameTask = new TimerTask() {
            @Override
            public void run() {
                updateGame();
            }
        };
        addKeyListener(this); 
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        createPlatforms();
        loadHighScoresOnline();
        timer.schedule(gameTask, 0, 10);
        
        // Make sure the panel gets focus to detect keyboard events
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                requestFocusInWindow();
            }
        });
    }

    public void createPlatforms() {
        platforms.clear();
        platformIds.clear();
        hitPlatformIds.clear();
        nextPlatformId = 0;

        Rectangle firstPlatform = new Rectangle(200, 400, 100, 10);
        platforms.add(firstPlatform);
        platformIds.add(nextPlatformId++);
        playerX = firstPlatform.x + firstPlatform.width / 2 - 10;
        playerY = firstPlatform.y - 20;

        Random random = new Random();
        for (int i = 1; i < 5; i++) {
            platforms.add(new Rectangle(random.nextInt(400), 400 - i * 100, 100, 10));
            platformIds.add(nextPlatformId++);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 500, 500);

        g.setColor(Color.RED);
        g.fillRect(playerX, playerY, 20, 20);

        for (int i = 0; i < platforms.size(); i++) {
            Rectangle platform = platforms.get(i);
            int platformId = platformIds.get(i);
            g.setColor(hitPlatformIds.contains(platformId) ? Color.GRAY : Color.GREEN);
            g.fillRect(platform.x, platform.y, platform.width, platform.height);
        }

        g.setColor(Color.WHITE);
        g.drawString("Height: " + score, 10, 20);

        if (isGameOver) {
            g.drawString("Game Over! Score: " + score, 180, 250);
            g.drawString("High Scores:", 200, 270);
            for (int i = 0; i < highScores.size() && i < 5; i++) {
                ScoreEntry entry = highScores.get(i);
                g.drawString((i + 1) + ". " + entry.name + ": " + entry.score, 200, 290 + i * 20);
            }
        }
    }

    public void updateGame() {
        if (isGameOver) {
            repaint();
            return;
        }

        playerY += playerVelocityY;

        if (playerY > 500) {
            isGameOver = true;
            updateHighScores();
            saveHighScoresOnline();
        }

        bounceIfOnPlatform();

        if (moveLeft && playerX > 0) {
            playerX -= 10;
        }
        if (moveRight && playerX < 480) {
            playerX += 10;
        }

        Random random = new Random();
        for (int i = 0; i < platforms.size(); i++) {
            Rectangle platform = platforms.get(i);
            platform.y += 1;
            if (platform.y > 500) {
                platform.y = -10;
                platform.x = random.nextInt(400);
                platformIds.set(i, nextPlatformId++);
            }
        }

        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // This is now empty as we're using java.util.Timer instead of javax.swing.Timer
    }

    public void bounceIfOnPlatform() {
        boolean touchedPlatform = false;

        for (int i = 0; i < platforms.size(); i++) {
            Rectangle platform = platforms.get(i);
            int platformId = platformIds.get(i);
            if (playerY + 20 >= platform.y && playerY + 20 <= platform.y + 10 &&
                playerX + 20 > platform.x && playerX < platform.x + platform.width) {
                touchedPlatform = true;
                playerVelocityY = -15;
                if (!hitPlatformIds.contains(platformId)) {
                    score++;
                    hitPlatformIds.add(platformId);
                }
            }
        }

        if (!touchedPlatform) {
            playerVelocityY += 1;
        }
    }

    public void restartGame() {
        isGameOver = false;
        score = 0;
        playerVelocityY = 0;
        moveLeft = false;
        moveRight = false;
        onPlatform = false;
        createPlatforms();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) moveLeft = true;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) moveRight = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) moveLeft = false;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) moveRight = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    public void updateHighScores() {
        boolean qualifies = highScores.size() < 5 || score > highScores.get(highScores.size() - 1).score;
        if (qualifies) {
            String name = JOptionPane.showInputDialog(this, "You made it to the high score list!\nEnter your name:");
            if (name == null || name.trim().isEmpty()) {
                name = "Anonymous";
            }
            highScores.add(new ScoreEntry(name.trim(), score));
            highScores.sort((a, b) -> Integer.compare(b.score, a.score));
            if (highScores.size() > 5) {
                highScores = new ArrayList<>(highScores.subList(0, 5));
            }
        }
    }

    public String buildHighScoreJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"record\": [");

        for (int i = 0; i < highScores.size(); i++) {
            ScoreEntry entry = highScores.get(i);
            sb.append("{\"name\":\"").append(entry.name)
              .append("\",\"score\":").append(entry.score).append("}");
            if (i < highScores.size() - 1) {
                sb.append(",");
            }
        }

        sb.append("]}");
        return sb.toString();
    }

    public void saveHighScoresOnline() {
        try {
            URL url = new URL("https://api.jsonbin.io/v3/b/" + BIN_ID);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Master-Key", MASTER_KEY);
            connection.setDoOutput(true);

            String json = buildHighScoreJson();

            OutputStream os = connection.getOutputStream();
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            int responseCode = connection.getResponseCode();
            System.out.println("Save response code: " + responseCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadHighScoresOnline() {
        try {
            URL url = new URL("https://api.jsonbin.io/v3/b/" + BIN_ID + "/latest");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Master-Key", MASTER_KEY);

            InputStream responseStream = connection.getInputStream();
            Scanner scanner = new Scanner(responseStream, StandardCharsets.UTF_8);
            String responseBody = scanner.useDelimiter("\\A").next();
            scanner.close();

            highScores.clear();
            int index = responseBody.indexOf("[");
            int endIndex = responseBody.indexOf("]");
            if (index != -1 && endIndex != -1) {
                String arrayContent = responseBody.substring(index + 1, endIndex);
                String[] entries = arrayContent.split("\\},\\{");

                for (String entry : entries) {
                    entry = entry.replace("{", "").replace("}", "").replace("\"", "");
                    String[] parts = entry.split(",");
                    String name = "";
                    int score = 0;
                    for (String part : parts) {
                        String[] keyValue = part.split(":");
                        if (keyValue[0].trim().equals("name")) {
                            name = keyValue[1];
                        } else if (keyValue[0].trim().equals("score")) {
                            score = Integer.parseInt(keyValue[1].trim());
                        }
                    }
                    highScores.add(new ScoreEntry(name, score));
                }
            }

            System.out.println("Highscores loaded from server!");

        } catch (Exception e) {
            e.printStackTrace();
            highScores = new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        Game game = new Game();
        frame.setTitle("School Project - Java Game");
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(game);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}