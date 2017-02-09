package net.ddns.ksuto.clockwork;

import net.ddns.ksuto.clockwork.activity.Clockwork;
import net.ddns.ksuto.clockwork.entities.middleman.ConfigureMiddleMan;
import net.ddns.ksuto.clockwork.entities.middleman.MiddleMan;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.*;

@SuppressWarnings({"serial", "Duplicates"})
public class ClockWork_UI extends JFrame {
    
    private final TBoPeripheralRobotHelper peripherals;
    
    private final int        i_DELAY        = 100;
    private final JTextField jTextFieldCast = new JTextField("/Please Auto Config ");
    private       int        iRed           = 250, iGreen = 250, iBlue = 250, iGrey = 0;
    private JButton autoConfButton;
    private JButton jButtonFish;
    private MiddleMan middleMan = new MiddleMan();
    private Clockwork clockwork;
    
    @SuppressWarnings("ConstantConditions")
    private ClockWork_UI(TBoPeripheralRobotHelper peripherals) throws IOException {
        
        this.peripherals = peripherals;
        
        JPanel panel = new JPanel() {
            
            private final BufferedImage buf = ImageIO.read(getClass().getResource("/Pictures/background.png"));
            
            @Override
            protected void paintComponent(Graphics g) {
                
                super.paintComponent(g);
                g.drawImage(buf, 0, 0, null);
            }
        };
        
        initMainPanel(panel);
        
        autoConfButton = new JButton();
        initautoConfButton();
        add(autoConfButton);
        
        jButtonFish = new JButton();
        initFishButton();
        add(jButtonFish);
        
        jTextFieldCast.setPreferredSize(new Dimension(140, 30));
        initTextField();
        add(jTextFieldCast);
        
        pack();
        setVisible(true);
        
        ActionListener fadeOutAction = actionEvent -> {
            if (iGrey < 240) {
                iGrey += 10;
                jTextFieldCast.setBackground(new Color(iGrey, iGrey, iGrey));
            }
        };
        
        final Timer fadeOutActionTimer = new Timer(30, fadeOutAction);
        fadeOutActionTimer.start();
    }
    
    public static void main(String[] args) throws InterruptedException, IOException, AWTException {
        
        TBoPeripheralRobotHelper peripherals = new TBoPeripheralRobotHelper();
        
        ClockWork_UI ui = new ClockWork_UI(peripherals);
        System.out.println("new UI");
        ui.jTextFieldCast.requestFocus();
        ui.clockwork = new Clockwork(peripherals, ui);
        System.out.println("new Clockwork");
        while (ui.getMiddleMan().getKeys().isEmpty()) { peripherals.robot.delay(200); }
        ui.clockwork.play();
        System.exit(0);
    }
    
    public void dojButtonFishClick() {
        
        jButtonFish.doClick();
    }
    
    private void initMainPanel(JPanel panel) throws IOException {
        
        setContentPane(panel);
        setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
        setTitle("AutoGrind");
        URL url = getClass().getResource("/Pictures/icon.Default.jpg");
        if (url != null) { setIconImage(ImageIO.read(url)); }
        setAlwaysOnTop(true);
        setPreferredSize(new Dimension(200, 61));
        setResizable(false);
        setLocation(peripherals.getScreen().i_SCREEN_WIDTH / 2, peripherals.getScreen().i_SCREEN_HEIGHT / 2);
        setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(iRed, iGreen, iBlue));
    }
    
    private void initTextField() {
        
        jTextFieldCast.setEditable(false);
        jTextFieldCast.setOpaque(false);
        jTextFieldCast.setHorizontalAlignment(JTextField.CENTER);
        jTextFieldCast.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        jTextFieldCast.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
    }
    
    private void initFishButton() {
        
        jButtonFish.setPreferredSize(new Dimension(25, 30));
        jButtonFish.setIcon(getImageIconFromResourse("/Pictures/button.icon.fish.up.png"));
        jButtonFish.setMargin(new Insets(0, 0, 0, 0));
        jButtonFish.setBorder(null);
        jButtonFish.setBackground(new Color(255, 255, 255, 0));
        jButtonFish.setOpaque(false);
        jButtonFish.setToolTipText("Using (H) to Fish and (W) for Lure shortcuts (Shift W) for Bait");
        jButtonFish.setEnabled(false);
        jButtonFish.addActionListener(fishButtonListener());
    }
    
    private void initautoConfButton() {
        
        autoConfButton.setPreferredSize(new Dimension(25, 30));
        autoConfButton.setIcon(getImageIconFromResourse("/Pictures/button.icon.config.up.png"));
        autoConfButton.setMargin(new Insets(0, 0, 0, 0));
        autoConfButton.setBorder(null);
        autoConfButton.setBackground(new Color(255, 255, 255, 0));
        autoConfButton.setOpaque(false);
        autoConfButton.addActionListener(autoConfButtonListener());
    }
    
    private ActionListener fishButtonListener() {
        
        return actionEvent -> {
            if (clockwork.status == Status.RUN) {
                jButtonFish.setIcon(getImageIconFromResourse("/Pictures/button.icon.fish.down.png"));
                clockwork.status = Status.FISHING;
                
                autoConfButton.setEnabled(false);
                jButtonFish.setEnabled(true);
            }
            else if (clockwork.status == Status.FISHING) {
                jButtonFish.setIcon(getImageIconFromResourse("/Pictures/button.icon.fish.up.png"));
                
                clockwork.status = Status.RUN;
                
                autoConfButton.setEnabled(true);
                jButtonFish.setEnabled(true);
            }
        };
    }
    
    private ActionListener autoConfButtonListener() {
        
        return actionEvent -> {
            
            autoConfButton.setIcon(getImageIconFromResourse("/Pictures/button.icon.config.down.png"));
            
            SwingUtilities.invokeLater(() -> {
                ConfigureMiddleMan conf;
                try {
                    conf = new ConfigureMiddleMan();
                    conf.run();
                    middleMan = conf.getMiddleMan();
                }
                catch (AWTException | IOException e) {
                    e.printStackTrace();
                }
                if (!middleMan.getKeys().isEmpty()) {
                    setEnabledButtonAutoconf(true);
                    setEnabledButtonFish(true);
                }
                
                autoConfButton.setIcon(getImageIconFromResourse("/Pictures/button.icon.config.up.png"));
            });
        };
    }
    
    private ImageIcon getImageIconFromResourse(String resource) {
        
        try {
            URL url = getClass().getResource(resource);
            return new ImageIcon(ImageIO.read(url));
        }
        catch (IOException ignore) {
        }
        return null;
    }
    
    public MiddleMan getMiddleMan() {
        
        return middleMan;
    }
    
    private void setEnabledButtonAutoconf(boolean b) {
        
        autoConfButton.setEnabled(b);
    }
    
    private void setEnabledButtonFish(boolean b) {
        
        jButtonFish.setEnabled(b);
    }
    
    public void setTextField(String sKey) {
        
        String sTemp = jTextFieldCast.getText() + " " + sKey;
        while (sTemp.length() > 17) { sTemp = sTemp.substring(1); }
        jTextFieldCast.setText(sTemp);
    }
    
    public void setiGrey(int iGrey) {
        
        this.iGrey = iGrey;
    }
    
    public enum Status {
        RUN, PAUSE, FISHING, TOMTOM //STOP, CONFIG,
    }
}