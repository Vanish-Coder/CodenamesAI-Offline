//codenamesRunner.java

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class codenamesRunner extends JFrame {
    private CodenamesBoard gameBoard;
    private GameState gameState;
    private JPanel boardPanel;
    private JPanel topPanel;
    private JLabel hintLabel;
    private JLabel numberLabel;
    private JLabel statusLabel;
    private JLabel guessCountLabel;
    private JLabel redScoreLabel;
    private JLabel blueScoreLabel;
    private JLabel remainingLabel;
    private JLabel timerLabel;
    private JButton newGameButton;
    private JButton endTurnButton;
    private JButton resetGuessButton;
    private JTextArea gameLogArea;
    
    private String currentTeam = "RED";
    private String startingTeam = "RED"; // Track which team goes first
    private int cardsToWinByStartingTeam = 9; // Starting team has 9
    private int cardsToWinByOtherTeam = 8;   // Other team has 8
    private int guessesRemaining = 0;
    private boolean waitingForHint = false;
    private boolean gameActive = true;
    private javax.swing.Timer turnTimer;
    private int secondsRemaining = 180; // 3 minutes = 180 seconds
    
    private static final String WORDS_FILE = "words.txt";
    private static final String STATE_FILE = "state.json";
    private static final String HINT_FILE = "hint.json";
    private static final String PYTHON_SCRIPT = "codenamesAI.py";
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new codenamesRunner());
    }
    
    public codenamesRunner() {
        setTitle("Codenames Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 1000);
        setLocationRelativeTo(null);
        setResizable(true);
        
        // Initialize game
        gameBoard = new CodenamesBoard();
        gameState = new GameState();
        startingTeam = new Random().nextBoolean() ? "RED" : "BLUE";
        gameBoard.generateBoard(startingTeam);
        saveStateToJSON();
        
        // UI Setup
        setLayout(new BorderLayout(10, 10));
        
        // Top panel with hints and status
        topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(4, 1, 5, 5));
        topPanel.setBackground(new Color(30, 30, 30));
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Status row
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
        statusRow.setBackground(new Color(30, 30, 30));
        statusLabel = new JLabel("Starting game...");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 28));
        statusRow.add(statusLabel);
        topPanel.add(statusRow);
        
        // Scores row
        JPanel scoresRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 60, 10));
        scoresRow.setBackground(new Color(30, 30, 30));
        redScoreLabel = new JLabel("RED: 0/9");
        redScoreLabel.setForeground(new Color(255, 100, 100));
        redScoreLabel.setFont(new Font("Arial", Font.BOLD, 24));
        blueScoreLabel = new JLabel("BLUE: 0/8");
        blueScoreLabel.setForeground(new Color(100, 150, 255));
        blueScoreLabel.setFont(new Font("Arial", Font.BOLD, 24));
        remainingLabel = new JLabel("Unrevealed: 25");
        remainingLabel.setForeground(Color.WHITE);
        remainingLabel.setFont(new Font("Arial", Font.BOLD, 20));
        scoresRow.add(redScoreLabel);
        scoresRow.add(blueScoreLabel);
        scoresRow.add(remainingLabel);
        topPanel.add(scoresRow);
        
        // Hint row
        JPanel hintRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        hintRow.setBackground(new Color(30, 30, 30));
        JLabel clueLabel = new JLabel("CLUE:");
        clueLabel.setForeground(Color.WHITE);
        clueLabel.setFont(new Font("Arial", Font.BOLD, 20));
        hintLabel = new JLabel("Waiting for spymaster clue...");
        hintLabel.setForeground(new Color(255, 255, 100));
        hintLabel.setFont(new Font("Arial", Font.BOLD, 32));
        JLabel countLabel = new JLabel("COUNT:");
        countLabel.setForeground(Color.WHITE);
        countLabel.setFont(new Font("Arial", Font.BOLD, 20));
        numberLabel = new JLabel("?");
        numberLabel.setForeground(new Color(255, 255, 100));
        numberLabel.setFont(new Font("Arial", Font.BOLD, 40));
        hintRow.add(clueLabel);
        hintRow.add(hintLabel);
        hintRow.add(countLabel);
        hintRow.add(numberLabel);
        topPanel.add(hintRow);
        
        // Guess count row
        JPanel guessRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        guessRow.setBackground(new Color(30, 30, 30));
        guessCountLabel = new JLabel("Guesses Remaining: 0");
        guessCountLabel.setForeground(new Color(200, 255, 200));
        guessCountLabel.setFont(new Font("Arial", Font.BOLD, 24));
        timerLabel = new JLabel("⏱ Time: 3:00");
        timerLabel.setForeground(new Color(255, 200, 100));
        timerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        guessRow.add(guessCountLabel);
        guessRow.add(timerLabel);
        topPanel.add(guessRow);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Center: Board and Log
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        centerPanel.setBackground(Color.WHITE);
        
        // Board panel
        boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(5, 5, 5, 5));
        boardPanel.setBackground(Color.WHITE);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        updateBoardDisplay();
        centerPanel.add(new JScrollPane(boardPanel));
        
        // Game log
        gameLogArea = new JTextArea();
        gameLogArea.setEditable(false);
        gameLogArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        gameLogArea.setBackground(new Color(30, 30, 30));
        gameLogArea.setForeground(new Color(200, 200, 200));
        gameLogArea.setLineWrap(true);
        gameLogArea.setWrapStyleWord(true);
        gameLogArea.setMargin(new Insets(10, 10, 10, 10));
        centerPanel.add(new JScrollPane(gameLogArea));
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Bottom panel with controls
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
        newGameButton = new JButton("New Game");
        newGameButton.setFont(new Font("Arial", Font.BOLD, 14));
        newGameButton.addActionListener(e -> startNewGame());
        endTurnButton = new JButton("End Turn");
        endTurnButton.setFont(new Font("Arial", Font.BOLD, 14));
        endTurnButton.addActionListener(e -> endTurn());
        resetGuessButton = new JButton("Reset Guesses");
        resetGuessButton.setFont(new Font("Arial", Font.BOLD, 14));
        resetGuessButton.addActionListener(e -> resetGuesses());
        bottomPanel.add(newGameButton);
        bottomPanel.add(endTurnButton);
        bottomPanel.add(resetGuessButton);
        add(bottomPanel, BorderLayout.SOUTH);
        
        setVisible(true);
        
        // Request initial hint
        requestAIHint();
    }
    
    private void updateBoardDisplay() {
        boardPanel.removeAll();
        for (String word : gameBoard.getAllWords()) {
            CardButton btn = new CardButton(word, gameBoard.getCardTeam(word), gameBoard.isRevealed(word));
            btn.addActionListener(e -> handleCardClick(word));
            boardPanel.add(btn);
        }
        boardPanel.revalidate();
        boardPanel.repaint();
    }
    
    private void handleCardClick(String word) {
        // Prevent card clicks during various states
        if (!gameActive || gameBoard.isRevealed(word) || waitingForHint || guessesRemaining <= 0) {
            return;
        }
        
        gameBoard.reveal(word);
        updateBoardDisplay();
        updateScores();
        
        String team = gameBoard.getCardTeam(word);
        guessesRemaining--;
        updateGuessCount();
        
        if (team.equals(currentTeam)) {
            // Correct guess!
            addLog("✓ " + word + " - Correct! (" + currentTeam + ")");
            checkWinCondition();
            if (gameActive && guessesRemaining > 0) {
                addLog("Team " + currentTeam + " can guess again!");
            } else if (guessesRemaining == 0) {
                // Auto-end turn when guesses run out
                addLog("Out of guesses! Turn ends.");
                switchTeam();
            }
        } else if (team.equals("ASSASSIN")) {
            // Game over - hit the assassin
            addLog("☠ " + word + " - ASSASSIN! Team " + currentTeam + " LOSES!");
            gameActive = false;
            if (turnTimer != null) {
                turnTimer.stop();
            }
            gameLogArea.append("\n=== GAME OVER ===\nTeam " + (currentTeam.equals("RED") ? "BLUE" : "RED") + " WINS!\n");
            endTurnButton.setEnabled(false);
            resetGuessButton.setEnabled(false);
        } else {
            // Wrong guess - end turn
            addLog("✗ " + word + " - Wrong! (" + team + ") Turn ENDS.");
            switchTeam();
        }
    }
    
    private void switchTeam() {
        // Stop the timer
        if (turnTimer != null) {
            turnTimer.stop();
        }
        
        currentTeam = currentTeam.equals("RED") ? "BLUE" : "RED";
        guessesRemaining = 0;
        secondsRemaining = 180; // Reset to 3 minutes
        updateGuessCount();
        updateStatus();
        addLog("\n>>> " + currentTeam + " TEAM'S TURN <<<\n");
        requestAIHint();
    }
    
    private void endTurn() {
        if (guessesRemaining > 0) {
            addLog("Team " + currentTeam + " ended turn early with " + guessesRemaining + " guess(es) remaining.");
        }
        switchTeam();
    }
    
    private void resetGuesses() {
        guessesRemaining = 0;
        updateGuessCount();
        addLog("Guesses reset to 0.");
    }
    
    private void startTurnTimer() {
        secondsRemaining = 180;
        if (turnTimer != null) {
            turnTimer.stop();
        }
        
        turnTimer = new javax.swing.Timer(1000, e -> {
            secondsRemaining--;
            updateTimerLabel();
            
            if (secondsRemaining <= 0) {
                turnTimer.stop();
                addLog("⏰ TIME'S UP! Turn automatically ends.");
                switchTeam();
            } else if (secondsRemaining == 30) {
                addLog("⚠ 30 seconds remaining!");
            } else if (secondsRemaining == 10) {
                addLog("⚠⚠ 10 seconds remaining!");
            }
        });
        turnTimer.start();
    }
    
    private void updateTimerLabel() {
        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;
        timerLabel.setText(String.format("⏱ Time: %d:%02d", minutes, seconds));
    }
    
    private void checkWinCondition() {
        int redRevealed = gameBoard.getRevealedCount("RED");
        int blueRevealed = gameBoard.getRevealedCount("BLUE");
        
        // Determine which team needs 9 and which needs 8
        int redTarget = startingTeam.equals("RED") ? 9 : 8;
        int blueTarget = startingTeam.equals("BLUE") ? 9 : 8;
        
        if (redRevealed == redTarget) {
            gameLogArea.append("\n=== GAME OVER ===\nTeam RED WINS!\n");
            gameActive = false;
            if (turnTimer != null) {
                turnTimer.stop();
            }
            endTurnButton.setEnabled(false);
            resetGuessButton.setEnabled(false);
        } else if (blueRevealed == blueTarget) {
            gameLogArea.append("\n=== GAME OVER ===\nTeam BLUE WINS!\n");
            gameActive = false;
            if (turnTimer != null) {
                turnTimer.stop();
            }
            endTurnButton.setEnabled(false);
            resetGuessButton.setEnabled(false);
        }
    }
    
    private void updateGuessCount() {
        guessCountLabel.setText("Guesses Remaining: " + guessesRemaining);
    }
    
    private void updateScores() {
        int redRevealed = gameBoard.getRevealedCount("RED");
        int blueRevealed = gameBoard.getRevealedCount("BLUE");
        int totalRevealed = redRevealed + blueRevealed + gameBoard.getRevealedCount("NEUTRAL") + (gameBoard.isRevealed(gameBoard.getAssassin()) ? 1 : 0);
        int unrevealed = 25 - totalRevealed;
        
        // Show targets based on which team started first
        int redTarget = startingTeam.equals("RED") ? 9 : 8;
        int blueTarget = startingTeam.equals("BLUE") ? 9 : 8;
        
        redScoreLabel.setText("RED: " + redRevealed + "/" + redTarget);
        blueScoreLabel.setText("BLUE: " + blueRevealed + "/" + blueTarget);
        remainingLabel.setText("Unrevealed: " + unrevealed);
    }
    
    private void updateStatus() {
        if (currentTeam.equals("RED")) {
            statusLabel.setText("🔴 RED TEAM - Operatives Guessing");
        } else {
            statusLabel.setText("🔵 BLUE TEAM - Operatives Guessing");
        }
    }
    
    private void addLog(String message) {
        gameLogArea.append(message + "\n");
        gameLogArea.setCaretPosition(gameLogArea.getDocument().getLength());
    }
    
    private void saveStateToJSON() {
        try {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"team\": \"").append(currentTeam).append("\",\n");
            json.append("  \"red_words\": ").append(listToJsonArray(gameBoard.getRedWords())).append(",\n");
            json.append("  \"blue_words\": ").append(listToJsonArray(gameBoard.getBlueWords())).append(",\n");
            json.append("  \"neutral_words\": ").append(listToJsonArray(gameBoard.getNeutralWords())).append(",\n");
            json.append("  \"assassin\": \"").append(gameBoard.getAssassin()).append("\",\n");
            json.append("  \"revealed\": ").append(listToJsonArray(new ArrayList<>(gameBoard.getRevealedWords()))).append("\n");
            json.append("}");
            
            FileWriter file = new FileWriter(STATE_FILE);
            file.write(json.toString());
            file.close();
            System.out.println("State saved to " + STATE_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private String listToJsonArray(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(list.get(i)).append("\"");
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private void requestAIHint() {
        waitingForHint = true;
        hintLabel.setText("Getting spymaster clue...");
        numberLabel.setText("?");
        
        new Thread(() -> {
            try {
                // Save current state
                saveStateToJSON();
                
                // Run Python script
                ProcessBuilder pb = new ProcessBuilder("python3", PYTHON_SCRIPT);
                pb.directory(new File(System.getProperty("user.dir")));
                Process process = pb.start();
                
                // Capture any errors
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    System.err.println("Python error: " + errorLine);
                }
                
                int exitCode = process.waitFor();
                System.out.println("Python script exited with code: " + exitCode);
                
                // Read hint from JSON
                Thread.sleep(100); // Give file system time to write
                String hintJson = readFileAsString(HINT_FILE);
                String clue = extractJsonValue(hintJson, "clue");
                int number = Integer.parseInt(extractJsonValue(hintJson, "number"));
                
                SwingUtilities.invokeLater(() -> {
                    hintLabel.setText(clue);
                    numberLabel.setText(String.valueOf(number));
                    guessesRemaining = number + 1; // +1 for the bonus
                    updateGuessCount();
                    updateStatus();
                    addLog("--- SPYMASTER (" + currentTeam + ") gives clue: " + clue + " (" + number + ") ---");
                    waitingForHint = false;
                    startTurnTimer(); // Start timer after hint is received
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    hintLabel.setText("Error loading hint");
                    addLog("ERROR: Could not get AI hint - " + e.getMessage());
                    waitingForHint = false;
                });
            }
        }).start();
    }
    
    private String readFileAsString(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }
    
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        
        // Skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        
        // Check if value is a string (quoted) or number (unquoted)
        if (json.charAt(start) == '"') {
            // String value
            start++;
            int end = json.indexOf("\"", start);
            if (end == -1) return "";
            return json.substring(start, end);
        } else {
            // Number or other unquoted value
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != ']') {
                end++;
            }
            return json.substring(start, end).trim();
        }
    }
    
    private void startNewGame() {
        gameBoard = new CodenamesBoard();
        
        // Randomize which team starts first
        startingTeam = new Random().nextBoolean() ? "RED" : "BLUE";
        currentTeam = startingTeam;
        gameBoard.generateBoard(startingTeam);
        gameState = new GameState();
        
        saveStateToJSON();
        updateBoardDisplay();
        hintLabel.setText("Waiting for spymaster clue...");
        numberLabel.setText("?");
        gameLogArea.setText("=== NEW GAME STARTED ===\n" + currentTeam + " team goes first!\n\n");
        guessesRemaining = 0;
        gameActive = true;
        waitingForHint = false;
        secondsRemaining = 180;
        endTurnButton.setEnabled(true);
        resetGuessButton.setEnabled(true);
        updateScores();
        updateStatus();
        requestAIHint();
    }
    
    private void revealRandomWord() {
        List<String> unrevealed = gameBoard.getUnrevealedWords();
        if (!unrevealed.isEmpty()) {
            String word = unrevealed.get(new Random().nextInt(unrevealed.size()));
            gameBoard.reveal(word);
            updateBoardDisplay();
        }
    }
    
    // Custom Card Button
    static class CardButton extends JButton {
        private String team;
        private boolean revealed;
        
        CardButton(String text, String team, boolean revealed) {
            super(text);
            this.team = team;
            this.revealed = revealed;
            setFont(new Font("Arial", Font.BOLD, 16));
            setFocusPainted(false);
            setMargin(new Insets(10, 10, 10, 10));
            updateAppearance();
        }
        
        void updateAppearance() {
            if (revealed) {
                setForeground(Color.WHITE);
                switch(team) {
                    case "RED":
                        setBackground(new Color(220, 50, 50));
                        break;
                    case "BLUE":
                        setBackground(new Color(50, 120, 220));
                        break;
                    case "ASSASSIN":
                        setBackground(Color.BLACK);
                        break;
                    default: // NEUTRAL
                        setBackground(new Color(180, 180, 180));
                }
            } else {
                setBackground(new Color(240, 220, 180));
                setForeground(new Color(80, 60, 40));
            }
            setOpaque(true);
            setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        }
    }
}

// Board logic
class CodenamesBoard {
    private List<String> allWords = new ArrayList<>();
    private Map<String, String> wordTeamMap = new HashMap<>();
    private Set<String> revealed = new HashSet<>();
    
    private static final List<String> WORD_LIST = Arrays.asList(
        "BRIDGE", "BATTERY", "COMET", "KEY", "OCTOPUS",
        "RIVER", "MOON", "LIGHTNING", "LOCK", "CIRCUIT",
        "TABLE", "PAINT", "BOOK", "APPLE", "PENCIL",
        "SHARK", "CAR", "PLANT", "GLASS", "KNIFE",
        "BEAR", "COMPUTER", "CHAIR", "PAPER", "DRAGON",
        "BALL", "STAR", "CROWN", "PICTURE", "VIOLIN",
        "WINDOW", "GUITAR", "CASTLE", "DIAMOND", "HAMMER",
        "TIGER", "CLOCK", "BOOT", "SHIP", "BICYCLE"
    );
    
    void generateBoard(String startingTeam) {
        allWords.clear();
        wordTeamMap.clear();
        revealed.clear();
        
        // Shuffle and pick 25 words for 5x5 board
        List<String> shuffled = new ArrayList<>(WORD_LIST);
        Collections.shuffle(shuffled);
        List<String> selected = shuffled.subList(0, 25);
        allWords = selected;
        
        // Assign teams - starting team gets 9, other team gets 8
        Collections.shuffle(selected);
        int idx = 0;
        // Distribution: 9 cards to starting team, 8 to other, 7 neutral, 1 assassin
        if (startingTeam.equals("RED")) {
            // RED: 9, BLUE: 8, NEUTRAL: 7, ASSASSIN: 1
            for (int i = 0; i < 9; i++) {
                wordTeamMap.put(selected.get(idx++), "RED");
            }
            for (int i = 0; i < 8; i++) {
                wordTeamMap.put(selected.get(idx++), "BLUE");
            }
        } else {
            // BLUE: 9, RED: 8, NEUTRAL: 7, ASSASSIN: 1
            for (int i = 0; i < 8; i++) {
                wordTeamMap.put(selected.get(idx++), "RED");
            }
            for (int i = 0; i < 9; i++) {
                wordTeamMap.put(selected.get(idx++), "BLUE");
            }
        }
        for (int i = 0; i < 7; i++) {
            wordTeamMap.put(selected.get(idx++), "NEUTRAL");
        }
        wordTeamMap.put(selected.get(idx), "ASSASSIN");
    }
    
    List<String> getAllWords() {
        return new ArrayList<>(allWords);
    }
    
    List<String> getRedWords() {
        return getWordsByTeam("RED");
    }
    
    List<String> getBlueWords() {
        return getWordsByTeam("BLUE");
    }
    
    List<String> getNeutralWords() {
        return getWordsByTeam("NEUTRAL");
    }
    
    private List<String> getWordsByTeam(String team) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : wordTeamMap.entrySet()) {
            if (entry.getValue().equals(team)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    String getAssassin() {
        for (Map.Entry<String, String> entry : wordTeamMap.entrySet()) {
            if (entry.getValue().equals("ASSASSIN")) {
                return entry.getKey();
            }
        }
        return "";
    }
    
    String getCardTeam(String word) {
        return wordTeamMap.getOrDefault(word, "NEUTRAL");
    }
    
    void reveal(String word) {
        revealed.add(word);
    }
    
    boolean isRevealed(String word) {
        return revealed.contains(word);
    }
    
    List<String> getUnrevealedWords() {
        List<String> unrevealed = new ArrayList<>();
        for (String word : allWords) {
            if (!revealed.contains(word)) {
                unrevealed.add(word);
            }
        }
        return unrevealed;
    }
    
    int getRevealedCount(String team) {
        int count = 0;
        for (Map.Entry<String, String> entry : wordTeamMap.entrySet()) {
            if (entry.getValue().equals(team) && revealed.contains(entry.getKey())) {
                count++;
            }
        }
        return count;
    }
    
    Set<String> getRevealedWords() {
        return new HashSet<>(revealed);
    }
}

// Game state tracking
class GameState {
    String currentTeam;
    int redScore;
    int blueScore;
    String status;
    
    GameState() {
        this.currentTeam = "RED";
        this.redScore = 0;
        this.blueScore = 0;
        this.status = "IN_PROGRESS";
    }
}
