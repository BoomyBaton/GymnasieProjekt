// Uppdaterad LeaderboardManager-klass att använda med Svelte-servern
class LeaderboardManager {
    private static final String SERVER_URL = "http://localhost:8080/api/leaderboard"; // Uppdaterad API-endpoint
    
    // Skicka poäng till servern
    public static boolean submitScore(String playerName, int score) {
        try {
            URL url = new URL(SERVER_URL);
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
            return responseCode == 201; // Svelte-servern returnerar 201 Created
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Hämta top 10 från servern
    public static ArrayList<LeaderboardEntry> getTopScores() {
        ArrayList<LeaderboardEntry> scores = new ArrayList<>();
        
        try {
            URL url = new URL(SERVER_URL);
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
                
                // Enkel parsning av JSON-svar
                String jsonResp = response.toString();
                if (jsonResp.contains("[") && jsonResp.contains("]")) {
                    String[] entries = jsonResp.substring(jsonResp.indexOf("[") + 1, jsonResp.lastIndexOf("]")).split("},");
                    for (String entry : entries) {
                        if (entry.contains("playerName") && entry.contains("score")) {
                            String name = entry.substring(entry.indexOf("playerName") + 13);
                            name = name.substring(0, name.indexOf("\""));
                            
                            String scoreStr = entry.substring(entry.indexOf("score") + 7);
                            if (scoreStr.contains(",")) {
                                scoreStr = scoreStr.substring(0, scoreStr.indexOf(","));
                            } else if (scoreStr.contains("}")) {
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
    
    // Resten av koden är samma som tidigare
    // ...
}