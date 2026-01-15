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
    private static final int BIRD_SPAWN_DELAY_FRAMES = 45;
    
    // Game variables
    private int shipX, shipY;
    private int shipSpeed = 20;
    private int birdSpawnDelay = 0;
    private boolean[] keys = new boolean[256];
    private ArrayList<Asteroid> asteroids = new ArrayList<>();
    private ArrayList<Explosion> explosions = new ArrayList<>();
    private int score = 0;
    private boolean initialized = false;
    private boolean gameOver = false;

    // star field variables
    private ArrayList<Star> stars = new ArrayList<>();
    private static final int NUM_STARS = 150;
    
    // Difficulty variables
    private double asteroidSpeed = 2.5;
    private int asteroidRate = 3000;
    private int maxAsteroidsOnScreen = 2; // start with max 2 on screen
    
    // Images
    private BufferedImage shipImage;
    private BufferedImage asteroidImage;
    private BufferedImage explosionImage;
    private BufferedImage birdOfPreyImage;
    
    // Timers
    private Timer gameTimer;
    private Timer asteroidTimer;
    private Timer difficultyTimer;
    private Timer birdOfPreyTimer;

    // Font
    private Font arcadeFont;
    
    // Random generator
    private Random random = new Random();
    
    // Mouse tracking for restart button
    private Point mousePos = new Point();
    private boolean isHoveringRestart = false;

    // Bird of Prey enemy ship
    private static final int BIRD_WIDTH = 180;
    private static final int BIRD_HEIGHT = 180;
    private int birdX, birdY;
    private boolean birdActive = false;
    private double birdSpeed = 15.0; // Horizontal tracking speed
    private ArrayList<Photon> enemyPhotons = new ArrayList<>();
    private int photonCooldown = 0;
    private static final int PHOTON_FIRE_RATE = 60;

    // Enterprise photon torpedoes
    private ArrayList<Photon> enterprisePhotons = new ArrayList<>();
    private int enterprisePhotonCooldown = 0;
    private static final int ENTERPRISE_FIRE_RATE = 20;

   
    
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

    // Star class
    class Star {
	double x, y;
	double speed;
	int brightness;
    
	Star(double x, double y, double speed, int brightness) {
	    this.x = x;
	    this.y = y;
	    this.speed = speed;
	    this.brightness = brightness;
	}
    }

    // photon torpedo class
    class Photon {
	double x, y;
	double speed = 8.0;
	static final int SIZE = 15;
	boolean fromEnterprise;
	
	Photon(double x, double y, boolean fromEnterprise) {
	    this.x = x;
	    this.y = y;
	    this.fromEnterprise = fromEnterprise;
	    this.speed = fromEnterprise ? -8.0 : 8.0;
	}
    }


    // Particle class for firework explosions
    class Particle {
        double x, y;
        double vx, vy;
	double fadeRate;
        int life;
        int maxLife;
        Color color;
        
        Particle(double x, double y, double vx, double vy, int maxLife, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = 0;
            this.maxLife = maxLife;
            this.color = color;
	    this.fadeRate = 20;
        }
        
        void update() {
            x += vx;
            y += vy;
            life++;
        }
        
        boolean isDead() {
            return life >= maxLife;
        }
        
        float getAlpha() {
            float progress = (float)life / maxLife;
	    float fadedProgress = (float)Math.pow(progress, fadeRate);
            return 1.0f - fadedProgress;
        }
    }


    // explosion class
    class Explosion {
        ArrayList<Particle> particles;
        int frame;
        static final int MAX_FRAMES = 15; // Short overall duration
        
        Explosion(double x, double y) {
            this.frame = 0;
            this.particles = new ArrayList<>();
            
            // Create firework particles with higher density
            int numParticles = 60 + random.nextInt(30);
            for (int i = 0; i < numParticles; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                
                double speed;
                if (random.nextDouble() < 0.3) {
                    speed = 0.5 + random.nextDouble() * 1.5; // 30% slow particles near center
                } else {
                    speed = 3.0 + random.nextDouble() * 2.0; // 70% fast particles
                }
                
                double vx = Math.cos(angle) * speed;
                double vy = Math.sin(angle) * speed;
                
                int maxLife = 8 + random.nextInt(5);
                
                // Create color variations (orange, yellow, red)
                Color color;
                int colorChoice = random.nextInt(3);
                if (colorChoice == 0) {
                    color = new Color(255, 150, 0); // Orange
                } else if (colorChoice == 1) {
                    color = new Color(255, 200, 0); // Yellow
                } else {
                    color = new Color(255, 80, 0); // Red-orange
                }
                
                particles.add(new Particle(x, y, vx, vy, maxLife, color));
            }
        }
        
        boolean isFinished() {
            return frame >= MAX_FRAMES;
        }
        
        void update() {
            frame++;
            for (Particle p : particles) {
                p.update();
            }
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

		// Re-initialize stars with new canvas dimensions
		stars.clear();
		for (int i = 0; i < NUM_STARS; i++) {
		    double x = random.nextDouble() * CANVAS_WIDTH;
		    double y = random.nextDouble() * CANVAS_HEIGHT;
		    double speed = 1.5;
		    int brightness = 150 + random.nextInt(106);
		    stars.add(new Star(x, y, speed, brightness));
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
    }
    
    private void loadImages() {
        try {
            // Try to load images from Images folder
            shipImage = ImageIO.read(new File("Images/enterprise.png"));
            asteroidImage = ImageIO.read(new File("Images/asteroid_cartoon.png"));
	    explosionImage = ImageIO.read(new File("Images/enterprise_explosion.png"));
	    birdOfPreyImage = ImageIO.read(new File("Images/bird_of_prey.png"));

	    // Load custom arcade font
	    arcadeFont = Font.createFont(Font.TRUETYPE_FONT, new File("Fonts/PressStart2P.ttf")).deriveFont(24f);
        } catch (IOException | FontFormatException e) {
            System.err.println("Could not load resources: " + e.getMessage());
            // Create placeholder images if loading fails
            shipImage = createPlaceholderShip();
            asteroidImage = createPlaceholderAsteroid();
	    explosionImage = null;
	    birdOfPreyImage = createPlaceholderBird();
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

    private BufferedImage createPlaceholderBird() {
        BufferedImage img = new BufferedImage(BIRD_WIDTH, BIRD_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        int[] xPoints = {BIRD_WIDTH/2, 0, BIRD_WIDTH};
        int[] yPoints = {BIRD_HEIGHT, 0, 0};
        g.fillPolygon(xPoints, yPoints, 3);
        g.dispose();
        return img;
    }
    
   public void initGame() {
       
       // Get current actual dimensions
       CANVAS_WIDTH = getWidth();
       CANVAS_HEIGHT = getHeight();
    
       shipX = CANVAS_WIDTH / 2 - SHIP_WIDTH / 2;
       shipY = CANVAS_HEIGHT - SHIP_HEIGHT;
       score = 0;
       gameOver = false;
       explosions.clear();
       asteroids.clear();
       asteroidSpeed = 2.5;
       asteroidRate = 3000;
       maxAsteroidsOnScreen = 2;
       shipSpeed = 20;

       // Reset Bird of Prey
       birdActive = false;
       enemyPhotons.clear();
       enterprisePhotons.clear();
       photonCooldown = 0;
       enterprisePhotonCooldown = 0;
       birdSpawnDelay = 0;

       // Initialize stars
       stars.clear();
       for (int i = 0; i < NUM_STARS; i++) {
	   double x = random.nextDouble() * CANVAS_WIDTH;
	   double y = random.nextDouble() * CANVAS_HEIGHT;
	   double speed = 1.5; // Constant speed all stars
	   int brightness = 150 + random.nextInt(106); // 150-255
	   stars.add(new Star(x, y, speed, brightness));
       }

       // Start with 1-2 asteroids visible and separated
       double firstX = random.nextDouble() * (CANVAS_WIDTH / 2 - ASTEROID_SIZE);
       double firstY = random.nextDouble() * CANVAS_HEIGHT * 0.3; 
       double secondX = (CANVAS_WIDTH / 2) + random.nextDouble() * (CANVAS_WIDTH / 2 - ASTEROID_SIZE);
       double secondY = (CANVAS_HEIGHT * 0.2) + random.nextDouble() * CANVAS_HEIGHT * 0.3;
       asteroids.add(new Asteroid(firstX, firstY, asteroidSpeed));
       asteroids.add(new Asteroid(secondX, secondY, asteroidSpeed));

    
       // Stop any existing timers first
       if (gameTimer != null) gameTimer.stop();
       if (asteroidTimer != null) asteroidTimer.stop();
       if (difficultyTimer != null) difficultyTimer.stop();
       if (birdOfPreyTimer != null) birdOfPreyTimer.stop();
    
       // Game loop timer (60 FPS)
       gameTimer = new Timer(16, this);
       gameTimer.start();
    
       // Asteroid spawn timer
       asteroidTimer = new Timer(asteroidRate, e -> createAsteroid());
       asteroidTimer.start();
    
       // Difficulty increase timer
       difficultyTimer = new Timer(3000, e -> increaseDifficulty());
       difficultyTimer.start();

       // Bird of Prey spawn timer - appears after 3 seconds
       birdOfPreyTimer = new Timer(3000, e -> {
	       spawnBirdOfPrey();
	       ((Timer)e.getSource()).stop(); // Stop timer after spawning once
       });
       birdOfPreyTimer.setRepeats(false); // Only fire once
       birdOfPreyTimer.start();

       initialized = true;
   }

    private void spawnBirdOfPrey() {
        birdActive = true;
        birdX = CANVAS_WIDTH / 2 - BIRD_WIDTH / 2; // Center horizontally
        birdY = 100; 
        photonCooldown = PHOTON_FIRE_RATE;
        enemyPhotons.clear(); 
	birdSpawnDelay = BIRD_SPAWN_DELAY_FRAMES;
    }
    
    private void restartGame() {
        if (asteroidTimer != null) asteroidTimer.stop();
        if (difficultyTimer != null) difficultyTimer.stop();
	if (birdOfPreyTimer != null) birdOfPreyTimer.stop();
        initGame();
        repaint();
    }
    
    private void createAsteroid() {
	if (asteroids.size() < maxAsteroidsOnScreen) {
	    double x = random.nextDouble() * (CANVAS_WIDTH - ASTEROID_SIZE);
	    asteroids.add(new Asteroid(x, -ASTEROID_SIZE, asteroidSpeed));
	}
    }
    
    private void increaseDifficulty() {
        // Very gradual increase in asteroid density
        // Increase max asteroids on screen every 10 seconds, capping at 8
        if (maxAsteroidsOnScreen < 8) {
            maxAsteroidsOnScreen++;
        }
        
        // relatively sparse spawn interval (minimum 1 second)
        if (asteroidRate > 1000) {
            asteroidRate -= 200;
            asteroidTimer.stop();
            asteroidTimer = new Timer(asteroidRate, e -> createAsteroid());
            asteroidTimer.start();
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            moveShip();
	    updateStars();
            updateAsteroids();
	    updateBirdOfPrey();
            updatePhotons();
	    updateEnterprisePhotons();
	    updateExplosions();
            checkCollisions();
	    if (enterprisePhotonCooldown > 0) {
                enterprisePhotonCooldown--;
            }
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
	if (keys[KeyEvent.VK_SPACE] && enterprisePhotonCooldown <= 0) {
            fireEnterprisePhoton();
            enterprisePhotonCooldown = ENTERPRISE_FIRE_RATE;
        }
    }

    private void fireEnterprisePhoton() {
        double photonX = shipX + SHIP_WIDTH / 2 - Photon.SIZE / 2;
        double photonY = shipY;
        enterprisePhotons.add(new Photon(photonX, photonY, true));
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

    private void updateStars() {
	for (Star star : stars) {
	    star.y += star.speed;
        
	    // Wrap around when star goes off screen
	    if (star.y > CANVAS_HEIGHT) {
		star.y = 0;
		star.x = random.nextDouble() * CANVAS_WIDTH;
	    }
        
	    // Twinkling effect
	    if (random.nextDouble() < 0.02) { // 2% chance per frame
		star.brightness = 150 + random.nextInt(106);
	    }
	}
    }

    private void updateBirdOfPrey() {
        if (!birdActive) return;

	// bird of prey does not instantly move on first appearance
	if (birdSpawnDelay > 0) {
            birdSpawnDelay--;
            return;
        }
        
        // Track Enterprise horizontally
        int enterpriseCenter = shipX + SHIP_WIDTH / 2;
        int birdCenter = birdX + BIRD_WIDTH / 2;
        
        if (birdCenter < enterpriseCenter - 5) {
            birdX += birdSpeed;
            if (birdX + BIRD_WIDTH > CANVAS_WIDTH) {
                birdX = CANVAS_WIDTH - BIRD_WIDTH;
            }
        } else if (birdCenter > enterpriseCenter + 5) {
            birdX -= birdSpeed;
            if (birdX < 0) {
                birdX = 0;
            }
        }
        
        // Fire photon torpedoes
        photonCooldown--;
        if (photonCooldown <= 0) {
            enemyPhotons.add(new Photon(birdX + BIRD_WIDTH / 2 - Photon.SIZE / 2, 
					birdY + BIRD_HEIGHT, false));
            photonCooldown = PHOTON_FIRE_RATE;
        }
    }
    
    private void updatePhotons() {
        for (int i = enemyPhotons.size() - 1; i >= 0; i--) {
            Photon p = enemyPhotons.get(i);
            p.y += p.speed;
            
            // Remove photons that go off screen
            if (p.y > CANVAS_HEIGHT) {
                enemyPhotons.remove(i);
            }
        }
    }

    private void updateEnterprisePhotons() {
        for (int i = enterprisePhotons.size() - 1; i >= 0; i--) {
            Photon p = enterprisePhotons.get(i);
            p.y += p.speed; // Negative speed moves upward
            
            // Remove photons that go off screen
            if (p.y < -Photon.SIZE) {
                enterprisePhotons.remove(i);
            }
        }
    }
    
    private void checkCollisions() {

	// check asteroid collisions
	int hitboxTopOffset = (int)(SHIP_HEIGHT * 0.2);
        int hitboxHeight = SHIP_HEIGHT - hitboxTopOffset;
	
        for (int i = asteroids.size() - 1; i >= 0; i--) {
	    Asteroid a = asteroids.get(i);
            if (a.x < shipX + SHIP_WIDTH &&
                a.x + ASTEROID_SIZE > shipX &&
                a.y < shipY + hitboxTopOffset + hitboxHeight &&
                a.y + ASTEROID_SIZE > shipY + hitboxTopOffset) {
                gameOver = true;
                gameTimer.stop();
                asteroidTimer.stop();
                difficultyTimer.stop();
		if (birdOfPreyTimer != null) birdOfPreyTimer.stop();
                break;
            }
        }

	// Check photon torpedo collisions with enterprise
        for (Photon p : enemyPhotons) {
            if (p.x < shipX + SHIP_WIDTH &&
                p.x + Photon.SIZE > shipX &&
                p.y < shipY + SHIP_HEIGHT &&
                p.y + Photon.SIZE > shipY) {
                gameOver = true;
                gameTimer.stop();
                asteroidTimer.stop();
                difficultyTimer.stop();
                if (birdOfPreyTimer != null) birdOfPreyTimer.stop();
                break;
            }
        }

	// Check Enterprise photon collisions with asteroids
        for (int i = enterprisePhotons.size() - 1; i >= 0; i--) {
            Photon p = enterprisePhotons.get(i);
            for (int j = asteroids.size() - 1; j >= 0; j--) {
                Asteroid a = asteroids.get(j);
                if (p.x < a.x + ASTEROID_SIZE &&
                    p.x + Photon.SIZE > a.x &&
                    p.y < a.y + ASTEROID_SIZE &&
                    p.y + Photon.SIZE > a.y) {

		    // add explosion
		    explosions.add(new Explosion(a.x + ASTEROID_SIZE / 2, a.y + ASTEROID_SIZE / 2));

		    // Remove both if hit
		    enterprisePhotons.remove(i);
                    asteroids.remove(j);
                    score += 10; // Bonus points for destroying asteroid
                    break;
                }
            }
        }

	// Check enterprise photon collisions with bird of prey
        if (birdActive) {
            for (int i = enterprisePhotons.size() - 1; i >= 0; i--) {
                Photon p = enterprisePhotons.get(i);
                if (p.x < birdX + BIRD_WIDTH &&
                    p.x + Photon.SIZE > birdX &&
                    p.y < birdY + BIRD_HEIGHT &&
                    p.y + Photon.SIZE > birdY) {
                    // If hit, remove photon and bird of prey
                    enterprisePhotons.remove(i);
                    birdActive = false;
                    score += 50; // bonus for destroying bird of prey
                    break;
                }
            }
        }

    }

    private void updateExplosions() {
	    for (int i = explosions.size() - 1; i >= 0; i--) {
		Explosion exp = explosions.get(i);
		exp.update();
		if (exp.isFinished()) {
		    explosions.remove(i);
		}
	    }
	}
    
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

	if (!initialized) {
	    return; // Don't paint anything until initialized
	}
	
        Graphics2D g2d = (Graphics2D) g;
        
        // Enable anti-aliasing and rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

	// Draw stars (background)
	for (Star star : stars) {
	    g2d.setColor(new Color(255, 255, 255, star.brightness));
	    g2d.fillRect((int)star.x, (int)star.y, 1, 1);
	}
        
        if (!gameOver) {
            // Draw ship
            g2d.drawImage(shipImage, shipX, shipY, SHIP_WIDTH, SHIP_HEIGHT, null);
            
            // Draw asteroids
            for (Asteroid a : asteroids) {
                g2d.drawImage(asteroidImage, (int)a.x, (int)a.y, ASTEROID_SIZE, ASTEROID_SIZE, null);
            }

	    // Draw explosions
	    for (Explosion exp : explosions) {
		drawExplosion(g2d, exp);
	    }

	    // Draw Bird of Prey
            if (birdActive) {
                g2d.drawImage(birdOfPreyImage, birdX, birdY, BIRD_WIDTH, BIRD_HEIGHT, null);
            }
            
            // Draw enemy photon torpedoes
            for (Photon p : enemyPhotons) {
                int px = (int)p.x;
                int py = (int)p.y;
                
                // Outer glow layers
                g2d.setColor(new Color(255, 0, 0, 30));
                g2d.fillOval(px - 12, py - 12, Photon.SIZE + 24, Photon.SIZE + 24);
                
                g2d.setColor(new Color(255, 50, 0, 60));
                g2d.fillOval(px - 8, py - 8, Photon.SIZE + 16, Photon.SIZE + 16);
                
                g2d.setColor(new Color(255, 100, 0, 100));
                g2d.fillOval(px - 4, py - 4, Photon.SIZE + 8, Photon.SIZE + 8);
                
                // Bright core
                g2d.setColor(new Color(255, 150, 0));
                g2d.fillOval(px, py, Photon.SIZE, Photon.SIZE);
                
                // White hot center
                g2d.setColor(Color.WHITE);
                g2d.fillOval(px + 2, py + 2, Photon.SIZE - 4, Photon.SIZE - 4);
            }

	    // Draw enterprise photon torpedoes (blue)
            for (Photon p : enterprisePhotons) {
                int px = (int)p.x;
                int py = (int)p.y;
                
                // Outer glow layers (blue)
                g2d.setColor(new Color(0, 100, 255, 30));
                g2d.fillOval(px - 12, py - 12, Photon.SIZE + 24, Photon.SIZE + 24);
                
                g2d.setColor(new Color(0, 150, 255, 60));
                g2d.fillOval(px - 8, py - 8, Photon.SIZE + 16, Photon.SIZE + 16);
                
                g2d.setColor(new Color(50, 200, 255, 100));
                g2d.fillOval(px - 4, py - 4, Photon.SIZE + 8, Photon.SIZE + 8);
                
                // Bright core (cyan/blue)
                g2d.setColor(new Color(100, 220, 255));
                g2d.fillOval(px, py, Photon.SIZE, Photon.SIZE);
                
                // White hot center
                g2d.setColor(Color.WHITE);
                g2d.fillOval(px + 2, py + 2, Photon.SIZE - 4, Photon.SIZE - 4);
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


    // draw explosions method
    private void drawExplosion(Graphics2D g2d, Explosion exp) {
        for (Particle p : exp.particles) {
            if (p.isDead()) continue;
            
            float alpha = p.getAlpha();
            int alphaValue = (int)(alpha * 255);
            
            // Draw particle with glow
            int px = (int)p.x;
            int py = (int)p.y;
            
            // Outer glow
            Color glowColor = new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), 
                                       Math.min(alphaValue / 3, 80));
            g2d.setColor(glowColor);
            g2d.fillOval(px - 4, py - 4, 12, 12);
            
            // Main particle
            Color mainColor = new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), 
                                       alphaValue);
            g2d.setColor(mainColor);
            g2d.fillOval(px - 1, py - 1, 6, 6);
            
            // Bright center
            if (alpha > 0.5f) {
                g2d.setColor(new Color(255, 255, 200, (int)(alphaValue * 0.8f)));
                g2d.fillOval(px, py, 3, 3);
            }
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
            frame.setUndecorated(false); // Keep window decorations 
            
            // Set to maximized/fullscreen mode while keeping decorations
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            
            frame.setVisible(true);
            
            // Wait for the window to be fully sized, then initialize the game
            Timer initTimer = new Timer(100, e -> {
		    game.initGame();
		    game.requestFocusInWindow();
		    ((Timer)e.getSource()).stop();
	    });
	    initTimer.setRepeats(false);
	    initTimer.start();
        });
    }
}
