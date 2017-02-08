package net.ddns.ksuto.clockwork;

import net.ddns.ksuto.clockwork.activity.AutoHit;
import net.ddns.ksuto.clockwork.activity.TomTom.Arrows;
import net.ddns.ksuto.clockwork.tellMeWhen.ConfigureTMW;
import net.ddns.ksuto.clockwork.tellMeWhen.TMW;
import net.ddns.ksuto.clockwork.tools.Scanner;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.*;

@SuppressWarnings({"serial", "Duplicates"})
public class AutoHit_UI extends JFrame {
    
    //clockwork_vanilla
    
    private final TBoPeripheralRobotHelper peripherals;
    
    private final int               i_DELAY         = 100;
    private final JTextField        jTextFieldCast  = new JTextField("                    ");
    private final String[]          asComboList     = {"Default Mod", "Damage Mod", "Healer Mod"};//, "Tank Mod"};
    private final JComboBox<String> jComboBox       = new JComboBox<>(asComboList);
    public        boolean           isWindowFocused = false;
    private       int               iRed            = 250, iGreen = 250, iBlue = 250, iGrey = 0;
    private String  sClass                 = "Default";
    private int     iClass                 = 1;
    private int[]   arWait                 = new int[12];
    private boolean bBlinkInc              = false;
    private int     iClipboardNotification = 0;
    private JLabel  jLab                   = new JLabel("/Please Auto Config");
    private JButton  jButtonLeft;
    private JButton  jButtonRight;
    private JButton  autoConfButton;
    private JButton  jButtonTMW;
    private JButton  jButtonPause;
    private JButton  jButtonFish;
    private JButton  jButtonTomtom;
    private int[][]  ariWaitByClass;
    private String[] arsClassName;
    private String[] arsTMWConf;
    private double dTMWButtonSize = 0;
    private TMW    tmw            = new TMW();
    private Timer  clipboardCopyTimer;
    private String sTempString;
    private Arrows eArrow = Arrows.QUEST;
    private AutoHit autoHit;
    
    //    private ButtonGroup rbModGroup;
    //    private JRadioButton rbDefaultMod;
    //    private JRadioButton rbDamageMod;
    //    private JRadioButton rbHealerMod;
    
    @SuppressWarnings("ConstantConditions")
    private AutoHit_UI(TBoPeripheralRobotHelper peripherals) throws IOException {
        
        this.peripherals = peripherals;
        
        addWindowFocusListener(new WindowFocusListener() {
            
            //Status status = autoHit.status;
            
            @Override
            public void windowGainedFocus(WindowEvent e) {
                
                isWindowFocused = true;
            }
            
            @Override
            public void windowLostFocus(WindowEvent e) {
                
                isWindowFocused = false;
            }
        });
        
        getProperties();
        
        //autoHit.status = Status.RUN;
        
        JPanel panel = new JPanel() {
            
            private final BufferedImage buf = ImageIO.read(getClass().getResource("/Pictures/background.png"));
            
            @Override
            protected void paintComponent(Graphics g) {
                
                super.paintComponent(g);
                g.drawImage(buf, 0, 0, null);
            }
        };
        
        initMainPanel(panel);
        
        jLab = new JLabel("Default", SwingConstants.CENTER);
        initLabel();
        add(jLab);
        
        jButtonLeft = new JButton("<=");
        initLeftButton();
        //		add(jButtonLeft);
        
        jButtonRight = new JButton("=>");
        initRightButton();
        //		add(jButtonRight);
        
        autoConfButton = new JButton();
        initautoConfButton();
        add(autoConfButton);
        
        jButtonTMW = new JButton();
        initTmwButton();
        add(jButtonTMW);
        
        jButtonPause = new JButton();
        initPauseButton();
        add(jButtonPause);
        
        jButtonFish = new JButton();
        initFishButton();
        add(jButtonFish);
        
        jButtonTomtom = new JButton();
        initTomTomButton();
        add(jButtonTomtom);
        
        jComboBox.setPreferredSize(new Dimension(140, 22));
        initComboBox();
        add(jComboBox);
        
        jTextFieldCast.setPreferredSize(new Dimension(140, 18));
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
        
        initClipboardCopyTimer();
    }
    
    public static void main(String[] args) throws InterruptedException, IOException, AWTException {
        
        TBoPeripheralRobotHelper peripherals = new TBoPeripheralRobotHelper();
        
        AutoHit_UI ui = new AutoHit_UI(peripherals);
        System.out.println("new UI");
        ui.jComboBox.requestFocus();
        ui.autoHit = new AutoHit(peripherals, ui);
        System.out.println("new AutoHit");
        while (ui.getTMW().getKeys().isEmpty()) { peripherals.robot.delay(200); }
        ui.autoHit.play();
        System.exit(0);
    }
    
    public void dojButtonFishClick() {
        
        jButtonFish.doClick();
    }
    
    private void initMainPanel(JPanel panel) throws IOException {
        
        setContentPane(panel);
        setLayout(new FlowLayout(FlowLayout.CENTER, 1, 1));
        setTitle("AutoGrind");
        URL url = getClass().getResource("/Pictures/icon.Default.jpg");
        if (url != null) { setIconImage(ImageIO.read(url)); }
        setAlwaysOnTop(true);
        setPreferredSize(new Dimension(148, 119));
        setResizable(false);
        setLocation(peripherals.getScreen().i_SCREEN_WIDTH - 200, peripherals.getScreen().i_SCREEN_HEIGHT - 150);
        setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(iRed, iGreen, iBlue));
    }
    
    private void initClipboardCopyTimer() {
        
        ActionListener clipBoardNotification = new ActionListener() {
            
            private String sText;
            
            public void actionPerformed(ActionEvent actionEvent) {
                //				if (sTempString.equals("Default"))
                //					sText = "                     There are no Default TMW config                           ";
                //				else
                sText = "                     " + sTempString + " TMW Config as been copied to the clipboard                           ";
                jLab.setText(sText.substring(iClipboardNotification, iClipboardNotification + 20));
                iClipboardNotification++;
                if (iClipboardNotification > sText.length() - 20) {
                    iClipboardNotification = 0;
                    clipboardCopyTimer.stop();
                    jLab.setText(sTempString);
                }
            }
        };
        clipboardCopyTimer = new Timer(75, clipBoardNotification);
    }
    
    private void initTextField() {
        
        jTextFieldCast.setEditable(false);
        jTextFieldCast.setOpaque(false);
        jTextFieldCast.setHorizontalAlignment(JTextField.CENTER);
        jTextFieldCast.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        jTextFieldCast.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
    }
    
    private void initComboBox() {
        
        jComboBox.setSelectedIndex(0);
        jComboBox.setOpaque(false);
        jComboBox.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        jComboBox.setRenderer(new DefaultListCellRenderer() {
            
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                
                JComponent result = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                result.setOpaque(false);
                return result;
            }
        });
        ((JLabel) jComboBox.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
    }
    
    private void initTomTomButton() {
        
        jButtonTomtom.setPreferredSize(new Dimension(25, 30));
        jButtonTomtom.setIcon(getImageIconFromResourse("/Pictures/button.icon.tomtom.up.png"));
        jButtonTomtom.setMargin(new Insets(0, 0, 0, 0));
        jButtonTomtom.setBorder(null);
        jButtonTomtom.setBackground(new Color(255, 255, 255, 0));
        jButtonTomtom.setOpaque(false);
        jButtonTomtom.setEnabled(false);
        jButtonTomtom.addActionListener(tomtomButtonListener());
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
    
    private void initPauseButton() {
        
        jButtonPause.setPreferredSize(new Dimension(25, 30));
        jButtonPause.setIcon(getImageIconFromResourse("/Pictures/button.icon.pause.down.png"));
        jButtonPause.setMargin(new Insets(0, 0, 0, 0));
        jButtonPause.setBorder(null);
        jButtonPause.setBackground(new Color(255, 255, 255, 0));
        jButtonPause.setOpaque(false);
        jButtonPause.setEnabled(false);
        jButtonPause.addActionListener(pauseButtonListener());
    }
    
    private void initTmwButton() {
        
        jButtonTMW.setPreferredSize(new Dimension(25, 30));
        jButtonTMW.setIcon(getImageIconFromResourse("/Pictures/button.icon.tmw.up.png"));
        jButtonTMW.setMargin(new Insets(0, 0, 0, 0));
        jButtonTMW.setBorder(null);
        jButtonTMW.setBackground(new Color(255, 255, 255, 0));
        jButtonTMW.setOpaque(false);
        jButtonTMW.addActionListener(tmwButtonListener());
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
    
    private void initRightButton() {
        
        jButtonRight.setMargin(new Insets(0, 0, 0, 0));
        jButtonRight.setPreferredSize(new Dimension(70, 20));
        jButtonRight.setEnabled(false);
        jButtonRight.addActionListener(rightButtonListener());
    }
    
    private void initLeftButton() {
        
        jButtonLeft.setMargin(new Insets(0, 0, 0, 0));
        jButtonLeft.setPreferredSize(new Dimension(69, 20));
        jButtonLeft.setEnabled(false);
        jButtonLeft.addActionListener(leftButtonListener());
    }
    
    private void initLabel() {
        
        jLab.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        jLab.setPreferredSize(new Dimension(140, 15));
        jLab.addMouseListener(new MouseListener() {
            
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                
            }
            
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                
            }
            
            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                
                getCurrentTMWConfig();
            }
            
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                
            }
            
            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                
            }
        });
    }
    
    private ActionListener tomtomButtonListener() {
        
        return actionEvent -> {
            if (autoHit.status == Status.RUN) {
                if (eArrow == Arrows.ARCHEO) { jLab.setText("TomTom Archeo"); }
                else { jLab.setText("TomTom Quest"); }
                autoHit.status = Status.TOMTOM;
                jButtonTMW.setEnabled(false);
                
                jButtonLeft.setEnabled(true);
                jButtonRight.setEnabled(true);
                autoConfButton.setEnabled(false);
                jButtonTMW.setEnabled(false);
                jButtonPause.setEnabled(false);
                jButtonFish.setEnabled(false);
                // jButtonTomtom.setEnabled(false);
            }
            else if (autoHit.status == Status.TOMTOM) {
                jLab.setText(sClass);
                autoHit.status = Status.RUN;
                jButtonTMW.setEnabled(true);
                
                jButtonLeft.setEnabled(true);
                jButtonRight.setEnabled(true);
                autoConfButton.setEnabled(true);
                jButtonTMW.setEnabled(true);
                jButtonPause.setEnabled(true);
                jButtonFish.setEnabled(true);
                // jButtonTomtom.setEnabled(false);
            }
        };
    }
    
    private ActionListener fishButtonListener() {
        
        return actionEvent -> {
            if (autoHit.status == Status.RUN) {
                jButtonFish.setIcon(getImageIconFromResourse("/Pictures/button.icon.fish.down.png"));
                jLab.setText("Fishing");
                autoHit.status = Status.FISHING;
                
                jButtonLeft.setEnabled(false);
                jButtonRight.setEnabled(false);
                autoConfButton.setEnabled(false);
                jButtonTMW.setEnabled(true);
                jButtonPause.setEnabled(false);
                jButtonFish.setEnabled(true);
                jButtonTomtom.setEnabled(false);
            }
            else if (autoHit.status == Status.FISHING) {
                jButtonFish.setIcon(getImageIconFromResourse("/Pictures/button.icon.fish.up.png"));
                
                jLab.setText(sClass);
                autoHit.status = Status.RUN;
                
                jButtonLeft.setEnabled(true);
                jButtonRight.setEnabled(true);
                autoConfButton.setEnabled(true);
                jButtonTMW.setEnabled(true);
                jButtonPause.setEnabled(true);
                jButtonFish.setEnabled(true);
                jButtonTomtom.setEnabled(true);
            }
        };
    }
    
    private ActionListener pauseButtonListener() {
        
        ActionListener blinkBackground = actionEvent -> {
            if (!bBlinkInc) {
                //                iRed += 2;
                iGreen -= 2;
                iBlue -= 2;
            }
            else {
                //                iRed -= 2;
                iGreen += 2;
                iBlue += 2;
            }
            if (iGreen <= 100) { bBlinkInc = true; }
            if (iGreen >= 250) { bBlinkInc = false; }
            getContentPane().setBackground(new Color(250, iGreen, iBlue));
        };
        
        final Timer timerBlink = new Timer(10, blinkBackground);
        
        return actionEvent -> {
            if (autoHit.status == Status.FISHING) { jButtonFish.doClick(); }
            if (autoHit.status == Status.RUN) {
                jButtonPause.setIcon(getImageIconFromResourse("/Pictures/button.icon.play.up.png"));
                
                timerBlink.start();
                //                jButtonPause.setText("Resume");
                jLab.setText("On Hold");
                autoHit.status = Status.PAUSE;
                
                jButtonLeft.setEnabled(false);
                jButtonRight.setEnabled(false);
                autoConfButton.setEnabled(true);
                jButtonTMW.setEnabled(true);
                jButtonPause.setEnabled(true);
                jButtonFish.setEnabled(false);
                jButtonTomtom.setEnabled(false);
            }
            else {
                jButtonPause.setIcon(getImageIconFromResourse("/Pictures/button.icon.pause.down.png"));
                
                timerBlink.stop();
                iRed = 250;
                iGreen = 250;
                iBlue = 250;
                getContentPane().setBackground(new Color(iRed, iGreen, iBlue));
                //                jButtonPause.setText("/Pause");
                
                jLab.setText(sClass);
                autoHit.status = Status.RUN;
                
                jButtonLeft.setEnabled(true);
                jButtonRight.setEnabled(true);
                autoConfButton.setEnabled(true);
                jButtonTMW.setEnabled(true);
                jButtonPause.setEnabled(true);
                jButtonFish.setEnabled(true);
                jButtonTomtom.setEnabled(true);
            }
        };
    }
    
    private ActionListener tmwButtonListener() {
        
        return actionEvent -> {
            
            jButtonTMW.setIcon(getImageIconFromResourse("/Pictures/button.icon.tmw.down.png"));
            
            SwingUtilities.invokeLater(() -> {
                try {
                    Robot   robot;
                    Scanner scan = new Scanner(peripherals);
                    
                    ArrayList<int[]> resultsTMW = scan.searchPicture("/Pictures/scan.TMW.Anchor.png");
                    if (autoHit.status != Status.PAUSE) {
                        jButtonPause.doClick();
                        System.out.println("/Pause AutoHit");
                    }
                    robot = new Robot();
                    robot.mouseMove(peripherals.getScreen().i_SCREEN_WIDTH / 2, peripherals.getScreen().i_SCREEN_HEIGHT / 2);
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                    robot.delay(i_DELAY);
                    robot.keyPress(KeyEvent.VK_ENTER);
                    robot.keyRelease(KeyEvent.VK_ENTER);
                    robot.delay(i_DELAY);
                    peripherals.getKeyboard().typeString("/tmw");
                    robot.delay(i_DELAY);
                    robot.keyPress(KeyEvent.VK_ENTER);
                    robot.keyRelease(KeyEvent.VK_ENTER);
                    if (!resultsTMW.isEmpty() && autoHit.status == Status.PAUSE) {
                        jButtonPause.doClick();
                        System.out.println("Resuming AutoHit");
                    }
                    jButtonTMW.setIcon(getImageIconFromResourse(("/Pictures/button.icon.tmw.up.png")));
                }
                catch (AWTException | IOException e) {
                    e.printStackTrace();
                }
            });
        };
    }
    
    //Test branche
    
    private ActionListener rightButtonListener() {
        
        return actionEvent -> {
            
            if (autoHit.status == Status.TOMTOM) {
                if (eArrow == Arrows.QUEST) {
                    eArrow = Arrows.ARCHEO;
                    jLab.setText("<= Archeology =>");
                }
                else {
                    eArrow = Arrows.QUEST;
                    jLab.setText("<= Questing =>");
                }
            }
            else {
                iClass++;
                if (iClass == ariWaitByClass.length + 1) { iClass = 1; }
                
                sClass = arsClassName[iClass - 1];
                try {
                    URL url = getClass().getClassLoader().getResource("/Pictures/Icon." + sClass.replace(" ", ".") + ".jpg");
                    if (url != null) { setIconImage(ImageIO.read(url)); }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                arWait = ariWaitByClass[iClass - 1];
                
                jLab.setText(sClass);
            }
        };
    }
    
    private ActionListener leftButtonListener() {
        
        return actionEvent -> {
            
            if (autoHit.status == Status.TOMTOM) {
                if (eArrow == Arrows.QUEST) {
                    eArrow = Arrows.ARCHEO;
                    jLab.setText("TomTom Archeo");
                }
                else {
                    eArrow = Arrows.QUEST;
                    jLab.setText("TomTom Quest");
                }
            }
            else {
                iClass--;
                if (iClass == 0) { iClass = ariWaitByClass.length; }
                
                sClass = arsClassName[iClass - 1];
                try {
                    URL url = getClass().getClassLoader().getResource("/Pictures/Icon." + sClass.replace(" ", ".") + ".jpg");
                    if (url != null) { setIconImage(ImageIO.read(url)); }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                arWait = ariWaitByClass[iClass - 1];
                
                jLab.setText(sClass);
            }
        };
    }
    
    private ActionListener autoConfButtonListener() {
        
        return actionEvent -> {
            
            autoConfButton.setIcon(getImageIconFromResourse("/Pictures/button.icon.config.down.png"));
            
            SwingUtilities.invokeLater(() -> {
                if (autoHit.status != Status.PAUSE) { jButtonPause.doClick(); }
                ConfigureTMW conf = null;
                try {
                    conf = new ConfigureTMW();
                    conf.run();
                    tmw = conf.getTMW();
                    dTMWButtonSize = conf.getdTMWButtonSize();
                }
                catch (AWTException | IOException e) {
                    e.printStackTrace();
                }
                if (dTMWButtonSize != 0) {
                    setEnabledButtonLeft(true);
                    setEnabledButtonRight(true);
                    setEnabledButtonAutoconf(true);
                    setEnabledButtonTMW(true);
                    setEnabledButtonPause(true);
                    setEnabledButtonFish(true);
                    setEnabledButtonTomtom(true);
                }
                if (autoHit.status == Status.PAUSE) { jButtonPause.doClick(); }
                
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
    
    private void getCurrentTMWConfig() {
        
        setClipboardContents(arsTMWConf[iClass - 1]);
    }
    
    private void getProperties() throws IOException {
        
        Properties prop            = new Properties();
        String     sPropertiesFile = "autoGrind.properties";
        
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(sPropertiesFile);
        prop.load(inputStream);
        
        int i = 0;
        
        arsClassName = prop.getProperty("Names").split(",");
        ariWaitByClass = new int[arsClassName.length][arWait.length];
        arsTMWConf = new String[arsClassName.length];
        
        for (String sClassName : arsClassName) {
            String[] arsTemp = prop.getProperty("Wait." + sClassName.replace(" ", ".")).split(",");
            arsTMWConf[i] = prop.getProperty("TMW.Conf." + sClassName.replace(" ", "."));
            int j = 0;
            for (String sWait : arsTemp) {
                ariWaitByClass[i][j++] = Integer.parseInt(sWait);
            }
            i++;
        }
    }
    
    public Arrows getDirection() {
        
        return eArrow;
    }
    
    public TMW getTMW() {
        
        return tmw;
    }
    
    private String getjLabText() {
        
        return jLab.getText();
    }
    
    public boolean isAutoCycleOn() {
        
        return (jComboBox.getSelectedIndex() == 1);
    }
    
    public boolean isHealerModOn() {
        
        return (jComboBox.getSelectedIndex() == 2);
    }
    
    private void setClipboardContents(String sTMWConfig) {
        
        StringSelection stringSelection = new StringSelection(sTMWConfig);
        Clipboard       clipboard       = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
        
        sTempString = getjLabText();
        clipboardCopyTimer.start();
    }
    
    private void setEnabledButtonAutoconf(boolean b) {
        
        autoConfButton.setEnabled(b);
    }
    
    private void setEnabledButtonFish(boolean b) {
        
        jButtonFish.setEnabled(b);
    }
    
    private void setEnabledButtonLeft(boolean b) {
        
        jButtonLeft.setEnabled(b);
    }
    
    private void setEnabledButtonPause(boolean b) {
        
        jButtonPause.setEnabled(b);
    }
    
    private void setEnabledButtonRight(boolean b) {
        
        jButtonRight.setEnabled(b);
    }
    
    private void setEnabledButtonTMW(boolean b) {
        
        jButtonTMW.setEnabled(b);
    }
    
    private void setEnabledButtonTomtom(boolean b) {
        
        jButtonTomtom.setEnabled(b);
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