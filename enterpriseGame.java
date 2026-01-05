import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;

public class enterpriseGame extends JPanel implements ActionListener, KeyListener {
    // Game constants
    private static int CANVAS_WIDTH = 1200;
    private static int CANVAS_HEIGHT = 800;
    private static int SHIP_WIDTH = 210;
    private static int SHIP_HEIGHT = 210;
    private static int ASTEROID_SIZE = 120;
    
    // Game variables
    private int shipX, shipY;
    private int shipSpeed = 20;
    private boolean[] keys = new boolean[256];
    private ArrayList<Asteroid> asteroids = new ArrayList<>();
    private int score = 0;
    private boolean gameOver = false;
    
    // Difficulty variables
    private double asteroidSpeed = 4.0;
    private int asteroidRate = 2000;
    
    // Images
    private BufferedImage shipImage;
    private BufferedImage asteroidImage;
    private BufferedImage explosionImage;
    
    // Timers
    private Timer gameTimer;
    private Timer asteroidTimer;
    private Timer difficultyTimer;

    // Font
    private Font arcadeFont;
    
    // Random generator
    private Random random = new Random();
    
    // Mouse tracking for restart button
    private Point mousePos = new Point();
    private boolean isHoveringRestart = false;
    
    // Asteroid class
    class Asteroid {
        double x, y;
        double speed;
        
        Asteroid(double x, double y, double speed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
        }
    }
    
    public enterpriseGame() {
        setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        
        // Enable double buffering for smoother rendering
        setDoubleBuffered(true);
        
        // Add component listener to handle window resizing
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                CANVAS_WIDTH = getWidth();
                CANVAS_HEIGHT = getHeight();
                // Update ship position when window resizes
                if (!gameOver) {
                    shipY = CANVAS_HEIGHT - SHIP_HEIGHT;
                }
            }
        });
        
        // Mouse listener for restart button
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (gameOver) {
                    Rectangle buttonRect = getRestartButtonRect();
                    if (buttonRect.contains(e.getPoint())) {
                        restartGame();
                    }
                }
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePos = e.getPoint();
                if (gameOver) {
                    Rectangle buttonRect = getRestartButtonRect();
                    isHoveringRestart = buttonRect.contains(mousePos);
                    setCursor(isHoveringRestart ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) 
                                                 : Cursor.getDefaultCursor());
                    repaint();
                }
            }
        });
        
        loadImages();
        initGame();
    }
    
    private void loadImages() {
        try {
            // Try to load images from Images folder
            shipImage = ImageIO.read(new File("Images/enterprise.png"));
            asteroidImage = ImageIO.read(new File("Images/asteroid_cartoon.png"));
	    explosionImage = ImageIO.read(new File("Images/enterprise_explosion.png"));

	    // Load custom arcade font
	    arcadeFont = Font.createFont(Font.TRUETYPE_FONT, 
                                     new File("Fonts/PressStart2P.ttf")).deriveFont(24f);
        } catch (IOException | FontFormatException e) {
            System.err.println("Could not load resources: " + e.getMessage());
            // Create placeholder images if loading fails
            shipImage = createPlaceholderShip();
            asteroidImage = createPlaceholderAsteroid();
	    explosionImage = null; 
	    arcadeFont = new Font("Monospaced", Font.BOLD, 32);
        }
    }
    
    private BufferedImage createPlaceholderShip() {
        BufferedImage img = new BufferedImage(SHIP_WIDTH, SHIP_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.CYAN);
        int[] xPoints = {SHIP_WIDTH/2, 0, SHIP_WIDTH};
        int[] yPoints = {0, SHIP_HEIGHT, SHIP_HEIGHT};
        g.fillPolygon(xPoints, yPoints, 3);
        g.dispose();
        return img;
    }
    
    private BufferedImage createPlaceholderAsteroid() {
        BufferedImage img = new BufferedImage(ASTEROID_SIZE, ASTEROID_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.GRAY);
        g.fillOval(0, 0, ASTEROID_SIZE, ASTEROID_SIZE);
        g.dispose();
        return img;
    }
    
    private void initGame() {
        shipX = CANVAS_WIDTH / 2 - SHIP_WIDTH / 2;
        shipY = CANVAS_HEIGHT - SHIP_HEIGHT;
        score = 0;
        gameOver = false;
        asteroids.clear();
        asteroidSpeed = 4.0;
        asteroidRate = 2000;
        shipSpeed = 20;

	
        // Game loop timer (60 FPS)
        gameTimer = new Timer(16, this);
        gameTimer.start();
        
        // Asteroid spawn timer
        asteroidTimer = new Timer(asteroidRate, e -> createAsteroid());
        asteroidTimer.start();
        
        // Difficulty increase timer
        difficultyTimer = new Timer(3000, e -> increaseDifficulty());
        difficultyTimer.start();
    }
    
    private void restartGame() {
        if (asteroidTimer != null) asteroidTimer.stop();
        if (difficultyTimer != null) difficultyTimer.stop();
        initGame();
        repaint();
    }
    
    private void createAsteroid() {
        double x = random.nextDouble() * (CANVAS_WIDTH - ASTEROID_SIZE);
        asteroids.add(new Asteroid(x, -ASTEROID_SIZE, asteroidSpeed));
    }
    
    private void increaseDifficulty() {
        if (asteroidSpeed < 10) {
            asteroidSpeed += 0.3;
        }
        if (asteroidRate > 400) {
            asteroidRate -= 150;
            asteroidTimer.stop();
            asteroidTimer = new Timer(asteroidRate, e -> createAsteroid());
            asteroidTimer.start();
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            moveShip();
            updateAsteroids();
            checkCollisions();
            repaint();
        }
    }
    
    private void moveShip() {
        if (keys[KeyEvent.VK_LEFT] && shipX > 0) {
            shipX -= shipSpeed;
        }
        if (keys[KeyEvent.VK_RIGHT] && shipX + SHIP_WIDTH < CANVAS_WIDTH) {
            shipX += shipSpeed;
        }
    }
    
    private void updateAsteroids() {
        for (int i = asteroids.size() - 1; i >= 0; i--) {
            Asteroid a = asteroids.get(i);
            a.y += a.speed;
            
            if (a.y > CANVAS_HEIGHT) {
                a.y = -ASTEROID_SIZE;
                a.x = random.nextDouble() * (CANVAS_WIDTH - ASTEROID_SIZE);
                score++;
            }
        }
    }
    
    private void checkCollisions() {
        for (Asteroid a : asteroids) {
            if (a.x < shipX + SHIP_WIDTH &&
                a.x + ASTEROID_SIZE > shipX &&
                a.y < shipY + SHIP_HEIGHT &&
                a.y + ASTEROID_SIZE > shipY) {
                gameOver = true;
                gameTimer.stop();
                asteroidTimer.stop();
                difficultyTimer.stop();
                break;
            }
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Enable anti-aliasing and rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        
        if (!gameOver) {
            // Draw ship
            g2d.drawImage(shipImage, shipX, shipY, SHIP_WIDTH, SHIP_HEIGHT, null);
            
            // Draw asteroids
            for (Asteroid a : asteroids) {
                g2d.drawImage(asteroidImage, (int)a.x, (int)a.y, ASTEROID_SIZE, ASTEROID_SIZE, null);
            }
            
            // Draw score
            g2d.setColor(Color.WHITE);
            g2d.setFont(arcadeFont.deriveFont(48f));
            g2d.drawString("Score: " + score, 20, 70);
        } else {
            // Game over screen
	    if (explosionImage != null) {
		// Draw explosion image 
		g2d.drawImage(explosionImage, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, null);
        
		// semi-transparent overlay 
		g2d.setColor(new Color(0, 0, 0, 100)); 
		g2d.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
	    }
	    
            g2d.setColor(Color.WHITE);
            g2d.setFont(arcadeFont.deriveFont(64f));
            FontMetrics fm = g2d.getFontMetrics();
            String gameOverText = "Game Over";
            int x = (CANVAS_WIDTH - fm.stringWidth(gameOverText)) / 2;
            g2d.drawString(gameOverText, x, CANVAS_HEIGHT / 2);
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(arcadeFont.deriveFont(40f));
            fm = g2d.getFontMetrics();
            String scoreText = "Final Score: " + score;
            x = (CANVAS_WIDTH - fm.stringWidth(scoreText)) / 2;
            g2d.drawString(scoreText, x, CANVAS_HEIGHT / 2 + 80);
            
            // Draw restart button
            Rectangle buttonRect = getRestartButtonRect();
            
            // Button styling with hover effect
            if (isHoveringRestart) {
                g2d.setColor(Color.BLUE);
            } else {
                g2d.setColor(new Color(34, 34, 34));
            }
            g2d.fillRoundRect(buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height, 10, 10);
            
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height, 10, 10);
            
            if (isHoveringRestart) {
                g2d.setColor(Color.BLACK);
            } else {
                g2d.setColor(Color.BLUE);
            }
            g2d.setFont(arcadeFont.deriveFont(32f));
            fm = g2d.getFontMetrics();
            String buttonText = "Restart";
            x = buttonRect.x + (buttonRect.width - fm.stringWidth(buttonText)) / 2;
            int y = buttonRect.y + ((buttonRect.height - fm.getHeight()) / 2) + fm.getAscent();
            g2d.drawString(buttonText, x, y);
        }
    }
    
    private Rectangle getRestartButtonRect() {
        int buttonW = 280;
        int buttonH = 80;
        int buttonX = CANVAS_WIDTH / 2 - buttonW / 2;
        int buttonY = CANVAS_HEIGHT / 2 + 120;
        return new Rectangle(buttonX, buttonY, buttonW, buttonH);
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() < keys.length) {
            keys[e.getKeyCode()] = true;
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < keys.length) {
            keys[e.getKeyCode()] = false;
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("USS Enterprise Asteroid Dodge");
            enterpriseGame game = new enterpriseGame();
            
            frame.add(game);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setUndecorated(false); // Keep window decorations (close button)
            
            // Set to maximized/fullscreen mode while keeping decorations
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            
            frame.setVisible(true);
            
            // Request focus for keyboard input
            game.requestFocusInWindow();
        });
    }
}
