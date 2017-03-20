package net.ddns.ksuto.clockwork;

import net.ddns.ksuto.clockwork.activity.Automaton;
import net.ddns.ksuto.clockwork.entities.qrcode.QrCode;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;
import net.ddns.ksuto.tools.TboTools_Debug;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.*;

@SuppressWarnings({"serial", "Duplicates"})
public class ClockWork_UI extends JDialog {
    
    private final TBoPeripheralRobotHelper peripherals;
    private final JTextField jTextFieldCast = new JTextField("/Please Auto Config ");
    private       int        iRed           = 250, iGreen = 250, iBlue = 250, iGrey = 0;
    private JButton autoConfButton;
    private JButton jButtonFish;
    private QrCode qrCode = new QrCode();
    private Automaton automaton;
    
    @SuppressWarnings("ConstantConditions")
    private ClockWork_UI(TBoPeripheralRobotHelper peripherals) throws IOException {
        
        this.peripherals = peripherals;
        
        JPanel panel = new JPanel() {
    
            private final BufferedImage bufferedImage = ImageIO.read(getClass().getResource("/Pictures/background.png"));
            
            @Override
            protected void paintComponent(Graphics g) {
                
                super.paintComponent(g);
                g.drawImage(bufferedImage, 0, 0, null);
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
        TboTools_Debug.sout("new UI");
        ui.jTextFieldCast.requestFocus();
        ui.automaton = new Automaton(peripherals, ui);
        TboTools_Debug.sout("new Automaton");
        while (ui.getQrCode().getKeys().isEmpty()) { peripherals.robot.delay(200); }
        ui.automaton.play();
        System.exit(0);
    }
    
    public void dojButtonFishClick() {
        
        jButtonFish.doClick();
    }
    
    private void initMainPanel(JPanel panel) throws IOException {
        
        setContentPane(panel);
        setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
        setTitle("ClockWork");
        URL url = getClass().getResource("/Pictures/icon.Default.jpg");
        if (url != null) { setIconImage(ImageIO.read(url)); }
        setAlwaysOnTop(true);
        setPreferredSize(new Dimension(200, 61));
        setResizable(false);
        setLocation(peripherals.getScreen().i_SCREEN_WIDTH - 270, peripherals.getScreen().i_SCREEN_HEIGHT - 131);
        getContentPane().setBackground(new Color(iRed, iGreen, iBlue));
    
        //        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
    
            public void windowClosing(WindowEvent evt) {
        
                System.exit(0);
            }
        });
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
            if (automaton.status == Status.RUN) {
                jButtonFish.setIcon(getImageIconFromResourse("/Pictures/button.icon.fish.down.png"));
                automaton.status = Status.FISHING;
                
                autoConfButton.setEnabled(false);
                jButtonFish.setEnabled(true);
            }
            else if (automaton.status == Status.FISHING) {
                jButtonFish.setIcon(getImageIconFromResourse("/Pictures/button.icon.fish.up.png"));
    
                automaton.status = Status.RUN;
                
                autoConfButton.setEnabled(true);
                jButtonFish.setEnabled(true);
            }
        };
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
    
    private ImageIcon getImageIconFromResourse(String resource) {
        
        try {
            URL url = getClass().getResource(resource);
            return new ImageIcon(ImageIO.read(url));
        }
        catch (IOException ignore) {
        }
        return null;
    }
    
    public QrCode getQrCode() {
        
        return qrCode;
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