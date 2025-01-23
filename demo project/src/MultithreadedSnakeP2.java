import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;
import javax.imageio.ImageIO;
import javax.swing.*;
public class MultithreadedSnakeP2 extends JPanel {
    private static final int BOARD_SIZE = 20;
    private static final int TILE_SIZE = 25;
    private static int GAME_SPEED = 200;

    private boolean gameOver = false;
    private String direction = "UP";
    private String player2Direction = "UP";
    private final Lock lock = new ReentrantLock();
    private final Condition directionChanged = lock.newCondition();
    private List<Point> snake = new LinkedList<>();
    private List<Point> player2Snake = new LinkedList<>();
    private Image backgroundImage;
    private Image snakeImage;
    private Image headImage;
    private Image snakeImage2;
    private Image headImage2;
    private BufferedImage foodImage;

    private Point food;
    private int score1 = 0;
    private int score2 = 0;

    public MultithreadedSnakeP2() {
        setPreferredSize(new Dimension(BOARD_SIZE * TILE_SIZE, BOARD_SIZE * TILE_SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                changeDirection(e);
            }
        });

        initializeGame();
    }

    private void initializeGame() {
        snake.clear();
        player2Snake.clear();
        snake.add(new Point(3 * BOARD_SIZE / 4, BOARD_SIZE / 2));
        player2Snake.add(new Point(BOARD_SIZE / 4, BOARD_SIZE / 2));
        direction = "UP";
        player2Direction = "UP";
        gameOver = false;
        score1=0;
        score2=0;
        GAME_SPEED=200;
        spawnFood();
        startThreads();
    }

    private void startThreads() {
        //movement thead
        new Thread(() -> {

            try {
                movementTask();
            }catch (Exception e) {
                e.printStackTrace();
            }


        }).start();

        //score thread
        new Thread(() -> {
            try {
                scoreTask();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        //collision thread
        new Thread(() -> {
            try {
                collisionTask();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    private void changeDirection(KeyEvent e) {
        lock.lock();
        try {
            switch (e.getKeyCode()) {

                case KeyEvent.VK_UP:
                    if (!direction.equals("DOWN")) {
                        direction = "UP";
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    if (!direction.equals("UP")) {
                        direction = "DOWN";
                    }
                    break;
                case KeyEvent.VK_LEFT:
                    if (!direction.equals("RIGHT")) {
                        direction = "LEFT";
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (!direction.equals("LEFT")) {
                        direction = "RIGHT";
                    }
                    break;
                case KeyEvent.VK_W:
                    if (!player2Direction.equals("DOWN")) {
                        player2Direction = "UP";
                    }
                    break;
                case KeyEvent.VK_S:
                    if (!player2Direction.equals("UP")) {
                        player2Direction = "DOWN";
                    }
                    break;
                case KeyEvent.VK_A:
                    if (!player2Direction.equals("RIGHT")) {
                        player2Direction = "LEFT";
                    }
                    break;
                case KeyEvent.VK_D:
                    if (!player2Direction.equals("LEFT")) {
                        player2Direction = "RIGHT";
                    }
                    break;

                case KeyEvent.VK_SPACE:
                    if (gameOver){
                        initializeGame();
                    }
                    break;
            }
            directionChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void movementTask() {
        while (!gameOver) {
            lock.lock();
            try {
                moveSnake(snake, direction);
                moveSnake(player2Snake, player2Direction);
                repaint();
            } finally {
                lock.unlock();
            }
            try {
                Thread.sleep(GAME_SPEED);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }


        }
    }

    private void moveSnake(List<Point> snake, String dir) {
        Point head = snake.get(0);
        Point newHead = switch (dir) {
            case "UP" -> new Point(head.x,head.y-1);
            case "DOWN" -> new Point(head.x, head.y+1);
            case "LEFT" -> new Point(head.x-1, head.y);
            case "RIGHT" -> new Point(head.x + 1, head.y);
            default -> head;
        };

        snake.add(0, newHead);
        if (!newHead.equals(food)) {
            snake.remove(snake.size()-1);
        }
    }

    private void collisionTask() {
        while (!gameOver) {
            lock.lock();
            try {
                checkCollisions(snake);
                checkCollisions(player2Snake);
                checkSnakeCollision();
            } finally {
                lock.unlock();
            }
        }
    }

    private void checkCollisions(List<Point> snake) {
        Point head = snake.get(0);

        if (head.x < 0|| head.x >= BOARD_SIZE || head.y < 0 || head.y >= BOARD_SIZE) {
            gameOver = true;
        }

        for (int i = 1; i < snake.size(); i++) {
            if (head.equals(snake.get(i))) {
                gameOver = true;
                break;
            }
        }
    }

    private void checkSnakeCollision() {
        Point head1 = snake.get(0);
        Point head2 = player2Snake.get(0);

        if (head1.equals(head2)) {
            gameOver = true;
        }

        for (Point point : player2Snake) {
            if (head1.equals(point)) {
                gameOver = true;
                break;
            }
        }
        for (Point point : snake) {
            if (head2.equals(point)) {
                gameOver = true;
                break;
            }
        }
    }

    private void scoreTask() {
        while (!gameOver) {
            lock.lock();
            try {
                Point head1 = snake.get(0);
                Point head2 = player2Snake.get(0);

                if (head1.equals(food)) {
                    score1++;
                    gameSpeed();

                    spawnFood();
                }
                if (head2.equals(food)) {
                    score2++;
                    gameSpeed();
                    spawnFood();
                }
            } finally {
                lock.unlock();
            }
        }
    }
    private void gameSpeed() {
        if (score1 %5== 0 || score2 %5 == 0) {
            GAME_SPEED=Math.max(50,GAME_SPEED-10);
        }
    }

    private void spawnFood() {
        Random random = new Random();
        Point newFood;
        do {
            newFood = new Point(random.nextInt(BOARD_SIZE), random.nextInt(BOARD_SIZE));
        } while (snake.contains(newFood) || player2Snake.contains(newFood));
        food = newFood;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        try {
            backgroundImage = ImageIO.read(getClass().getResource("/background.jpg")); // Adjust path as needed
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }

        try {
            foodImage = ImageIO.read(getClass().getResource("/redapple2.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (foodImage != null && food != null) {
            g.drawImage(foodImage, food.x * TILE_SIZE, food.y * TILE_SIZE, TILE_SIZE, TILE_SIZE, this);
        }



        //snake
        try {
            snakeImage=ImageIO.read(getClass().getResource("/body2.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i=1;i<snake.size();i++) {
            Point body = snake.get(i);
            g.drawImage(snakeImage, body.x * TILE_SIZE, body.y * TILE_SIZE, TILE_SIZE, TILE_SIZE, this);
        }


        try{
            headImage=ImageIO.read(getClass().getResource("/head1.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        Point head = snake.get(0);
        g.drawImage(headImage, head.x * TILE_SIZE, head.y * TILE_SIZE, TILE_SIZE, TILE_SIZE, this);

        //snake2
        try {
            snakeImage2=ImageIO.read(getClass().getResource("/bodyp2.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i=1;i< player2Snake.size();i++) {
            Point body =  player2Snake.get(i);
            g.drawImage(snakeImage2, body.x * TILE_SIZE, body.y * TILE_SIZE, TILE_SIZE, TILE_SIZE, this);
        }


        try{
            headImage2=ImageIO.read(getClass().getResource("/headp2.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        Point head1 = player2Snake.get(0);
        g.drawImage(headImage2, head1.x * TILE_SIZE, head1.y * TILE_SIZE, TILE_SIZE, TILE_SIZE, this);


        // score
        g.setColor(Color.white);
        g.setFont(new Font("Ink Free", Font.BOLD,20));
        g.drawString("P1 Score : " + score1, 370, 20);
        g.drawString("P2 Score: " + score2, 10, 20);



        // game over message
        if (gameOver) {
            g.setColor(Color.red);
            g.setFont(new Font("Ink Free", Font.BOLD, 50));

            g.drawString("Game Over!",BOARD_SIZE*TILE_SIZE/2-150,BOARD_SIZE*TILE_SIZE/2);
        }
    }}