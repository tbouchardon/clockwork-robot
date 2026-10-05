package fr.ksuto.clockwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Inject;
import fr.ksuto.clockwork.activity.Automaton;
import fr.ksuto.clockwork.entities.qrcode.QrCode;
import fr.ksuto.prh.PeripheralRobotHelper;
import fr.ksuto.prh.peripherals.Screen;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"Duplicates"})
public class ClockWorkUI {
    
    private static final Logger logger = LoggerFactory.getLogger(ClockWorkUI.class);
    
    private static final int BLUE  = 250;
    private static final int GREEN = 250;
    private static final int RED   = 250;
    @Inject
    PeripheralRobotHelper peripherals;
    JTextField castLogTextfield;
    Automaton  automaton;
    private int     iGrey      = 0;
    private JButton autoConfButton;
    private volatile QrCode  qrCode  = new QrCode();
    private volatile boolean qrFound = false;
    private Thread  automatonThread;
    private boolean shouldExit = false;
    
    public ClockWorkUI() {
        // Constructeur vide nécessaire pour le fonctionnement
        // Son abscence empêche la fermeture correcte de l’application
    }
    
    public void appendLog(String sKey) {
        
        String sTemp = castLogTextfield.getText() + " " + sKey;
        while (sTemp.length() > 17) {sTemp = sTemp.substring(1);}
        castLogTextfield.setText(sTemp);
    }
    
    public void initAutomaton() {
        
        automaton = new Automaton(peripherals, this);
        startQrCodeSearch();
    }

    /**
     * Cherche le QR code de l'addon toutes les 2 s jusqu'à le trouver, puis démarre l'automate. Sans clic ni frappe
     * (contrairement à Auto Config, qui active l'addon) : rien n'est envoyé à une autre fenêtre pendant l'attente.
     */
    private void startQrCodeSearch() {

        ScheduledExecutorService search = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "qr-search");
            thread.setDaemon(true);
            return thread;
        });
        search.scheduleWithFixedDelay(() -> {
            if (qrFound || shouldExit) {
                search.shutdown();
                return;
            }
            try {
                QrCode candidate = new QrCode();
                if (candidate.initIfVisible()) {
                    qrCode = candidate;
                    qrFound = true;
                    logger.info("QR code trouvé : démarrage de l'automate");
                    SwingUtilities.invokeLater(() -> appendLog("QR trouvé"));
                    startAutomaton();
                    search.shutdown();
                }
            }
            catch (RuntimeException e) {
                logger.debug("Recherche du QR code : {}", e.getMessage());
            }
        }, 0, 2, TimeUnit.SECONDS);
    }
    
    public void initUI() throws IOException {
        
        JDialog ui = new JDialog();
        JPanel panel = new JPanel() {

            private final transient BufferedImage bufferedImage = ImageIO.read(Objects.requireNonNull(getClass().getResource("/Pictures/background.png")));
            
            @Override
            protected void paintComponent(Graphics g) {
                
                super.paintComponent(g);
                g.drawImage(bufferedImage, 0, 0, null);
            }
        };
        
        ui.setContentPane(panel);
        ui.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
        ui.setTitle("ClockWork");
        URL url = getClass().getResource("/Pictures/icons/Default.jpg");
        if (url != null) {ui.setIconImage(ImageIO.read(url));}
        ui.setAlwaysOnTop(true);
        ui.setPreferredSize(new Dimension(210, 71));
        ui.setResizable(false);
        ui.setLocation(Screen.SCREEN_WIDTH - 270, Screen.SCREEN_HEIGHT - 95);
        ui.getContentPane().setBackground(new Color(RED, GREEN, BLUE));
        ui.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        ui.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent evt) {

                shouldExit = true;
            }
        });
        
        autoConfButton = new JButton();
        initAutoConfButton(autoConfButton);
        ui.add(autoConfButton);
        
        
        castLogTextfield = new JTextField("/Please Auto Config ");
        initTextField(castLogTextfield);
        ui.add(castLogTextfield);
        
        ui.pack();
        ui.setVisible(true);
        
        ActionListener fadeOutAction = actionEvent -> {
            if (iGrey < 240) {
                iGrey += 10;
                castLogTextfield.setBackground(new Color(iGrey, iGrey, iGrey));
            }
        };
        
        final Timer fadeOutActionTimer = new Timer(30, fadeOutAction);
        fadeOutActionTimer.start();
    }
    
    private ActionListener autoConfButtonListener() {
        
        return actionEvent -> {
            
            autoConfButton.setIcon(getImageIconFromResourse("/Pictures/button.icon.config.down.png"));
            setEnabledButtonAutoconf(false); // Disable the button while working

            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws AWTException, IOException {
                    // Le QR code n'est remplacé qu'une fois initialisé : l'automate en cours lit toujours un QR code complet
                    QrCode candidate = new QrCode();
                    boolean initialized = candidate.init(peripherals);
                    if (initialized) {
                        candidate.ensureAddonActive(peripherals);
                        qrCode = candidate;
                        qrFound = true;
                    }
                    return initialized;
                }

                @Override
                protected void done() {
                    try {
                        boolean initialized = get();
                        if (initialized) {
                            appendLog("      Done      ");
                        }
                        if (initialized && !qrCode.getKeys().isEmpty()) {
                            startAutomaton();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        // Handle exceptions from doInBackground() or get()
                        Throwable cause = e.getCause();
                        logger.debug(cause != null ? cause.getLocalizedMessage() : e.getLocalizedMessage());
                        Thread.currentThread().interrupt();
                    } finally {
                        // This runs whether the background task succeeded or failed
                        autoConfButton.setIcon(getImageIconFromResourse("/Pictures/button.icon.config.up.png"));
                        setEnabledButtonAutoconf(true);
                    }
                }
            };

            worker.execute();
        };
    }
    
    /**
     * Démarre l'automate s'il ne tourne pas déjà : une nouvelle Auto Config ne doit pas en lancer un second.
     */
    private synchronized void startAutomaton() {
        
        if (automatonThread != null && automatonThread.isAlive()) {return;}
        
        automatonThread = new Thread(() -> {
            try {
                automaton.play();
            } catch (Exception e) {
                logger.error("Automate arrêté", e);
            }
        }, "automaton");
        automatonThread.start();
    }
    
    private ImageIcon getImageIconFromResourse(String resource) {

        try {
            URL url = getClass().getResource(resource);
            return new ImageIcon(ImageIO.read(Objects.requireNonNull(url)));
        }
        catch (IOException e) {
            logger.debug("ERROR : Impossible de charger l'icone");
        }
        return null;
    }

    private void initButton(JButton button, String iconPath, String tooltip, ActionListener listener) {
        button.setPreferredSize(new Dimension(25, 30));
        button.setIcon(getImageIconFromResourse(iconPath));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(null);
        // Bouton réduit à son icône, quel que soit le thème Swing
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setToolTipText(tooltip);
        button.addActionListener(listener);
    }
    
    private void initTextField(JTextField castLogTextfield) {
        
        castLogTextfield.setEditable(false);
        castLogTextfield.setOpaque(false);
        castLogTextfield.setHorizontalAlignment(SwingConstants.CENTER);
        castLogTextfield.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        castLogTextfield.setPreferredSize(new Dimension(140, 30));
        castLogTextfield.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
    }

    private void initAutoConfButton(JButton autoConfButton) {
        initButton(autoConfButton, "/Pictures/button.icon.config.up.png", "Auto-configure QR Code positions", autoConfButtonListener());
    }
    
    public QrCode getQrCode() {
        
        return qrCode;
    }
    
    public boolean isShouldExit() {
        
        return shouldExit;
    }
    
    private void setEnabledButtonAutoconf(boolean b) {
        
        autoConfButton.setEnabled(b);
    }
    
    public void setiGrey(int iGrey) {
        
        this.iGrey = iGrey;
    }
}
