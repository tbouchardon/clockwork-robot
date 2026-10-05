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
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private volatile QrCode  qrCode  = new QrCode();
    private volatile boolean qrFound = false;
    private final AtomicBoolean searching = new AtomicBoolean(false);
    private Thread  automatonThread;
    private boolean shouldExit = false;
    
    public ClockWorkUI() {
        // Constructeur vide nécessaire pour le fonctionnement
        // Son abscence empêche la fermeture correcte de l’application
    }
    
    public void appendLog(String sKey) {
        
        String sTemp = castLogTextfield.getText() + " " + sKey;
        while (sTemp.length() > 32) {sTemp = sTemp.substring(1);}
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

        if (!searching.compareAndSet(false, true)) {return;}
        ScheduledExecutorService search = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "qr-search");
            thread.setDaemon(true);
            return thread;
        });
        search.scheduleWithFixedDelay(() -> {
            if (qrFound || shouldExit) {
                searching.set(false);
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
                    searching.set(false);
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
        ui.setContentPane(new JPanel(new BorderLayout()));
        ui.setTitle("ClockWork");
        URL url = getClass().getResource("/Pictures/icons/Default.jpg");
        if (url != null) {ui.setIconImage(ImageIO.read(url));}
        ui.setAlwaysOnTop(true);
        ui.setPreferredSize(new Dimension(260, 71));
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
        
        // Le QR code est cherché automatiquement (au démarrage, puis s'il disparaît) : le journal occupe toute la fenêtre
        castLogTextfield = new JTextField("Recherche du QR code...");
        initTextField(castLogTextfield);
        ui.add(castLogTextfield, BorderLayout.CENTER);
        
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
    
    /**
     * Relance la recherche du QR code (fenêtre de WoW déplacée, redimensionnée...) : appelée par l'automate quand le QR
     * code reste invisible.
     */
    public void requestQrCodeSearch() {

        if (searching.get()) {return;}
        logger.info("QR code introuvable à sa position : nouvelle recherche toutes les 2 s");
        qrFound = false;
        startQrCodeSearch();
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
    
    private void initTextField(JTextField castLogTextfield) {
        
        castLogTextfield.setEditable(false);
        castLogTextfield.setOpaque(false);
        castLogTextfield.setHorizontalAlignment(SwingConstants.CENTER);
        castLogTextfield.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        castLogTextfield.setPreferredSize(new Dimension(140, 30));
        castLogTextfield.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
    }

    public QrCode getQrCode() {
        
        return qrCode;
    }
    
    public boolean isShouldExit() {
        
        return shouldExit;
    }
    
    public void setiGrey(int iGrey) {
        
        this.iGrey = iGrey;
    }
}
