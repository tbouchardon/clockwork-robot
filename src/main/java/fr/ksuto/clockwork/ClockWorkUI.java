package fr.ksuto.clockwork;

import com.google.inject.Inject;
import fr.ksuto.clockwork.activity.Automaton;
import fr.ksuto.clockwork.entities.qrcode.QrCode;
import fr.ksuto.prh.PeripheralRobotHelper;
import fr.ksuto.prh.peripherals.Screen;
import fr.ksuto.tools.Debug;

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

@SuppressWarnings({"Duplicates"})
public class ClockWorkUI {
    
    private static final int BLUE  = 250;
    private static final int GREEN = 250;
    private static final int RED   = 250;
    @Inject
    PeripheralRobotHelper peripherals;
    JTextField castLogTextfield;
    Automaton  automaton;
    private int     iGrey      = 0;
    private JButton autoConfButton;
    private JButton fishButton;
    private QrCode  qrCode     = new QrCode();
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
    
    public void dojButtonFishClick() {
        
        fishButton.doClick();
    }
    
    public void initAutomaton() {
        
        automaton = new Automaton(peripherals, this);
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
        
        ui.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent evt) {

                shouldExit = true;
            }
        });
        
        autoConfButton = new JButton();
        initAutoConfButton(autoConfButton);
        ui.add(autoConfButton);
        
        fishButton = new JButton();
        initFishButton(fishButton);
        ui.add(fishButton);
        
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
                    qrCode = new QrCode();
                    return qrCode.init(peripherals);
                }

                @Override
                protected void done() {
                    try {
                        boolean initialized = get();
                        if (initialized) {
                            appendLog("      Done      ");
                        }
                        if (!qrCode.getKeys().isEmpty()) {
                            setEnabledButtonFish(true);
                            // Start the automaton in a new thread
                            new Thread(() -> {
                                try {
                                    automaton.play();
                                } catch (Exception e) {
                                    Debug.sout("Error in automaton: " + e.getLocalizedMessage());
                                    Thread.currentThread().interrupt();
                                }
                            }).start();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        // Handle exceptions from doInBackground() or get()
                        Throwable cause = e.getCause();
                        Debug.sout(cause != null ? cause.getLocalizedMessage() : e.getLocalizedMessage());
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
    
    private ActionListener fishButtonListener() {
        
        return actionEvent -> {
            if (automaton.status == Status.RUN) {
                fishButton.setIcon(getImageIconFromResourse("/Pictures/button.icon.fish.down.png"));
                automaton.status = Status.FISHING;

                autoConfButton.setEnabled(false);
                fishButton.setEnabled(true);
            }
            else if (automaton.status == Status.FISHING) {
                fishButton.setIcon(getImageIconFromResourse("/Pictures/button.icon.fish.up.png"));
                
                automaton.status = Status.RUN;

                autoConfButton.setEnabled(true);
                fishButton.setEnabled(true);
            }
        };
    }
    
    private ImageIcon getImageIconFromResourse(String resource) {

        try {
            URL url = getClass().getResource(resource);
            return new ImageIcon(ImageIO.read(Objects.requireNonNull(url)));
        }
        catch (IOException e) {
            Debug.sout("ERROR : Impossible de charger l'icone");
        }
        return null;
    }

    private void initButton(JButton button, String iconPath, String tooltip, ActionListener listener) {
        button.setPreferredSize(new Dimension(25, 30));
        button.setIcon(getImageIconFromResourse(iconPath));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(null);
        button.setBackground(new Color(255, 255, 255, 0));
        button.setOpaque(false);
        button.setToolTipText(tooltip);
        button.addActionListener(listener);
    }
    
    private void initFishButton(JButton fishButton) {
        initButton(fishButton, "/Pictures/button.icon.fish.up.png", "Using (H) to Fish and (W) for Lure shortcuts (Shift W) for Bait", fishButtonListener());
        fishButton.setEnabled(false);
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
    
    private void setEnabledButtonFish(boolean b) {
        
        fishButton.setEnabled(b);
    }
    
    public void setiGrey(int iGrey) {
        
        this.iGrey = iGrey;
    }
    
    public enum Status {
        RUN, PAUSE, FISHING, TOMTOM //STOP, CONFIG,
    }
}
