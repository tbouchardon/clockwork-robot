package fr.ksuto.clockwork;

import fr.ksuto.clockwork.activity.Automaton;
import fr.ksuto.clockwork.entities.qrcode.QrCode;
import fr.ksuto.prh.PeripheralRobotHelper;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import javax.imageio.ImageIO;
import javax.swing.*;

import com.google.inject.Inject;

@SuppressWarnings({"Duplicates"})
public class ClockWork_UI {
    
    private static final int BLUE = 250;
    @Inject
    PeripheralRobotHelper peripherals;
    private static final int GREEN = 250;
    private static final int RED   = 250;
    JTextField castLogTextfield;
    private int     iGrey  = 0;
    private JButton autoConfButton;
    private JButton fishButton;
    private QrCode  qrCode = new QrCode();
    Automaton automaton;
    
    public ClockWork_UI() {
    
    }
    
    public void initAutomaton() {
        
        automaton = new Automaton(peripherals, this);
    }
    
    public void dojButtonFishClick() {
        
        fishButton.doClick();
    }
    
    public void initUI() throws IOException {
        
        JDialog ui = new JDialog();
        JPanel panel = new JPanel() {
            
            private final BufferedImage bufferedImage = ImageIO.read(Objects.requireNonNull(getClass().getResource("/Pictures/background.png")));
            
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
        ui.setLocation(peripherals.getScreen().SCREEN_WIDTH - 270, peripherals.getScreen().SCREEN_HEIGHT - 131);
        ui.getContentPane().setBackground(new Color(RED, GREEN, BLUE));
        
        ui.addWindowListener(new WindowAdapter() {
            
            public void windowClosing(WindowEvent evt) {
                
                System.exit(0);
            }
        });
        
        autoConfButton = new JButton();
        initautoConfButton(autoConfButton);
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
            
            SwingUtilities.invokeLater(() -> {
                try {
                    qrCode = new QrCode();
                    qrCode.init(peripherals);
                }
                catch (AWTException | IOException e) {
                    e.printStackTrace();
                }
                if (!qrCode.getKeys().isEmpty()) {
                    setEnabledButtonAutoconf(true);
                    setEnabledButtonFish(true);
                }
                
                autoConfButton.setIcon(getImageIconFromResourse("/Pictures/button.icon.config.up.png"));
            });
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
        catch (IOException ignore) {
        }
        return null;
    }
    
    private void initFishButton(JButton fishButton) {
        
        fishButton.setPreferredSize(new Dimension(25, 30));
        fishButton.setIcon(getImageIconFromResourse("/Pictures/button.icon.fish.up.png"));
        fishButton.setMargin(new Insets(0, 0, 0, 0));
        fishButton.setBorder(null);
        fishButton.setBackground(new Color(255, 255, 255, 0));
        fishButton.setOpaque(false);
        fishButton.setToolTipText("Using (H) to Fish and (W) for Lure shortcuts (Shift W) for Bait");
        fishButton.setEnabled(false);
        fishButton.addActionListener(fishButtonListener());
    }
    
    private void initTextField(JTextField castLogTextfield) {
        
        castLogTextfield.setEditable(false);
        castLogTextfield.setOpaque(false);
        castLogTextfield.setHorizontalAlignment(JTextField.CENTER);
        castLogTextfield.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        castLogTextfield.setPreferredSize(new Dimension(140, 30));
        castLogTextfield.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
    }
    
    private void initautoConfButton(JButton autoConfButton) {
        
        autoConfButton.setPreferredSize(new Dimension(25, 30));
        autoConfButton.setIcon(getImageIconFromResourse("/Pictures/button.icon.config.up.png"));
        autoConfButton.setMargin(new Insets(0, 0, 0, 0));
        autoConfButton.setBorder(null);
        autoConfButton.setBackground(new Color(255, 255, 255, 0));
        autoConfButton.setOpaque(false);
        autoConfButton.setToolTipText("Using (H) to Fish and (W) for Lure shortcuts (Shift W) for Bait");
        autoConfButton.addActionListener(autoConfButtonListener());
    }
    
    public QrCode getQrCode() {
        
        return qrCode;
    }
    
    private void setEnabledButtonAutoconf(boolean b) {
        
        autoConfButton.setEnabled(b);
    }
    
    private void setEnabledButtonFish(boolean b) {
    
        fishButton.setEnabled(b);
    }
    
    public void setTextField(String sKey) {
    
        String sTemp = castLogTextfield.getText() + " " + sKey;
        while (sTemp.length() > 17) {sTemp = sTemp.substring(1);}
        castLogTextfield.setText(sTemp);
    }
    
    public void setiGrey(int iGrey) {
        
        this.iGrey = iGrey;
    }
    
    public enum Status {
        RUN, PAUSE, FISHING, TOMTOM //STOP, CONFIG,
    }
}