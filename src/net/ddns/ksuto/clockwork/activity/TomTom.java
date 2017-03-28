package net.ddns.ksuto.clockwork.activity;

import static net.ddns.ksuto.prh.properties.Constants.i_DELAY;

import net.ddns.ksuto.clockwork.entities.Position;
import net.ddns.ksuto.clockwork.entities.qrcode.QrCode;
import net.ddns.ksuto.clockwork.tools.RGBConverter;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;
import net.ddns.ksuto.tools.TboTools_Debug;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class TomTom {
    
    private static final int KEY_J = 74, KEY_Y = 89, KEY_SPACE = 32;
    private static final int TURN_DURATION      = 100;
    private static final int TURN_BACK_DURATION = 100;
    private TBoPeripheralRobotHelper peripherals;
    private boolean  isFlying              = false;
    private boolean  isRunning             = false;
    private Position playersLastPosition   = null;
    private Double   lastRemainingDistance = null;
    private boolean  turnedRight           = false;
    private int      pathIndex             = 0;
    private boolean  triedRight            = false;
    private int      closeStep             = 0;
    private double angleB;
    
    TomTom(TBoPeripheralRobotHelper peripherals) throws AWTException {
        
        this.peripherals = peripherals;
    }
    
    public void addCurrentPositionToPathList(QrCode qrCode, TBoPeripheralRobotHelper peripherals) {
        
        getCoordinates(qrCode, peripherals);
    }
    
    public void getCoordinates(QrCode qrCode, TBoPeripheralRobotHelper peripherals) {
    
        TboTools_Debug.sout("getCoordinates");
    
        BufferedImage biCapturedScreen = qrCode.captureQrCode(peripherals);
        
        int xPos = 0;
    
        if (biCapturedScreen.getRGB(6, 7) == RGBConverter.WHITE) {
            xPos += 524288;
        }
        if (biCapturedScreen.getRGB(7, 7) == RGBConverter.WHITE) {
            xPos += 262144;
        }
        if (biCapturedScreen.getRGB(8, 7) == RGBConverter.WHITE) {
            xPos += 131072;
        }
        if (biCapturedScreen.getRGB(9, 7) == RGBConverter.WHITE) {
            xPos += 65536;
        }
        if (biCapturedScreen.getRGB(10, 7) == RGBConverter.WHITE) {
            xPos += 32768;
        }
        if (biCapturedScreen.getRGB(11, 7) == RGBConverter.WHITE) {
            xPos += 16384;
        }
        if (biCapturedScreen.getRGB(12, 7) == RGBConverter.WHITE) {
            xPos += 8192;
        }
        if (biCapturedScreen.getRGB(13, 7) == RGBConverter.WHITE) {
            xPos += 4096;
        }
        if (biCapturedScreen.getRGB(2, 8) == RGBConverter.WHITE) {
            xPos += 2048;
        }
        if (biCapturedScreen.getRGB(3, 8) == RGBConverter.WHITE) {
            xPos += 1024;
        }
        if (biCapturedScreen.getRGB(4, 8) == RGBConverter.WHITE) {
            xPos += 512;
        }
        if (biCapturedScreen.getRGB(5, 8) == RGBConverter.WHITE) {
            xPos += 256;
        }
        if (biCapturedScreen.getRGB(6, 8) == RGBConverter.WHITE) {
            xPos += 128;
        }
        if (biCapturedScreen.getRGB(7, 8) == RGBConverter.WHITE) {
            xPos += 64;
        }
        if (biCapturedScreen.getRGB(8, 8) == RGBConverter.WHITE) {
            xPos += 32;
        }
        if (biCapturedScreen.getRGB(9, 8) == RGBConverter.WHITE) {
            xPos += 16;
        }
        if (biCapturedScreen.getRGB(10, 8) == RGBConverter.WHITE) {
            xPos += 8;
        }
        if (biCapturedScreen.getRGB(11, 8) == RGBConverter.WHITE) {
            xPos += 4;
        }
        if (biCapturedScreen.getRGB(12, 8) == RGBConverter.WHITE) {
            xPos += 2;
        }
        if (biCapturedScreen.getRGB(13, 8) == RGBConverter.WHITE) {
            xPos += 1;
        }
        qrCode.currenPlayerPosition.xPos = xPos;
        
        int yPos = 0;
        if (biCapturedScreen.getRGB(6, 10) == RGBConverter.WHITE) {
            yPos += 524288;
        }
        if (biCapturedScreen.getRGB(7, 10) == RGBConverter.WHITE) {
            yPos += 262144;
        }
        if (biCapturedScreen.getRGB(8, 10) == RGBConverter.WHITE) {
            yPos += 131072;
        }
        if (biCapturedScreen.getRGB(9, 10) == RGBConverter.WHITE) {
            yPos += 65536;
        }
        if (biCapturedScreen.getRGB(10, 10) == RGBConverter.WHITE) {
            yPos += 32768;
        }
        if (biCapturedScreen.getRGB(11, 10) == RGBConverter.WHITE) {
            yPos += 16384;
        }
        if (biCapturedScreen.getRGB(12, 10) == RGBConverter.WHITE) {
            yPos += 8192;
        }
        if (biCapturedScreen.getRGB(13, 10) == RGBConverter.WHITE) {
            yPos += 4096;
        }
        if (biCapturedScreen.getRGB(2, 11) == RGBConverter.WHITE) {
            yPos += 2048;
        }
        if (biCapturedScreen.getRGB(3, 11) == RGBConverter.WHITE) {
            yPos += 1024;
        }
        if (biCapturedScreen.getRGB(4, 11) == RGBConverter.WHITE) {
            yPos += 512;
        }
        if (biCapturedScreen.getRGB(5, 11) == RGBConverter.WHITE) {
            yPos += 256;
        }
        if (biCapturedScreen.getRGB(6, 11) == RGBConverter.WHITE) {
            yPos += 128;
        }
        if (biCapturedScreen.getRGB(7, 11) == RGBConverter.WHITE) {
            yPos += 64;
        }
        if (biCapturedScreen.getRGB(8, 11) == RGBConverter.WHITE) {
            yPos += 32;
        }
        if (biCapturedScreen.getRGB(9, 11) == RGBConverter.WHITE) {
            yPos += 16;
        }
        if (biCapturedScreen.getRGB(10, 11) == RGBConverter.WHITE) {
            yPos += 8;
        }
        if (biCapturedScreen.getRGB(11, 11) == RGBConverter.WHITE) {
            yPos += 4;
        }
        if (biCapturedScreen.getRGB(12, 11) == RGBConverter.WHITE) {
            yPos += 2;
        }
        if (biCapturedScreen.getRGB(13, 11) == RGBConverter.WHITE) {
            yPos += 1;
        }
        qrCode.currenPlayerPosition.yPos = yPos;
    }
    
    public void stuckProtocol() {
        
        TboTools_Debug.sout("Trying to unstuck...");
        
        runStop();
        
        peripherals.robot.keyPress(40); // Recule
        
        peripherals.robot.keyPress(32); // Saute
        peripherals.robot.delay(100);
        peripherals.robot.keyRelease(32); // Stop Saute
        
        peripherals.robot.delay(900);
        
        peripherals.robot.keyPress(32); // Saute
        
        if (turnedRight) { turnLeft(500); }
        else { turnRight(500); } // Tourne
        
        peripherals.robot.keyRelease(32); // Stop Saute
        peripherals.robot.keyRelease(40); // Stop Recule
        
        stepForward(2000); // Avance
        
        runStart();
    }
    
    void drive(QrCode qrCode, TBoPeripheralRobotHelper peripherals, boolean actionPossible, Boolean inCombat) {
    
        //        TboTools_Debug.sout("--------------------------------- drive ----------------------------------");
        
        // En cas de cible active (actions engagées)
        if (actionPossible || inCombat) {
            // Passage en camera position combat (Pour pouvoir loot plus facilement)
            qrCode.cameraCombat(peripherals);
            
            // Arréter de courrir et retour en cas de cible active
            if (isRunning) { runStop(); }
            return;
        }
        
        // Passage en camera position course
        qrCode.cameraDrive(peripherals);
        
        // Arrêter de courrir et retour si il n'y a plus de points de cheminement
        if (qrCode.path.isEmpty()) {
            pathIndex = 0;
            if (isRunning) {
                runStop();
            }
            QrCode.typeInChat(peripherals, "/kto drive");
            return;
        }
        
        // Courrir
        if (!isRunning) {
            runStart();
        }
        
        // Retourner au point de départ si la fonction LOOP est activée et qu'il n'y a plus de points de cheminement
        if (qrCode.DRIVE_LOOP.active && pathIndex > qrCode.path.size() - 1) {
            TboTools_Debug.sout("loop");
            pathIndex = 0;
        }
        
        // Si l'on est à cours de points de cheminement, on vide la liste, on arrête de courrir et retour
        if (pathIndex > qrCode.path.size() - 1) {
            qrCode.path.clear();
            if (isRunning) {
                runStop();
            }
            pathIndex = 0;
            return;
        }
        
        Position path = qrCode.path.get(pathIndex); //coordonnées destination
        getCoordinates(qrCode, peripherals); //position du personage
    
        TboTools_Debug.sout("Position courante : qrCode.currenPlayerPosition.xPos = " + qrCode.currenPlayerPosition.xPos + ", qrCode.currenPlayerPosition.yPos = " + qrCode
                                                                                                                                                                             .currenPlayerPosition
                                                                                                                                                                             .yPos);
        if (playersLastPosition == null) {
            playersLastPosition = new Position(qrCode.currenPlayerPosition);
            lastRemainingDistance = Math.sqrt(Math.pow(path.xPos - qrCode.currenPlayerPosition.xPos, 2) + Math.pow(path.yPos - qrCode.currenPlayerPosition.yPos, 2));
            return;
        }
        
        // Calcul de l'équation de la droite passant par la position précédente et le point de cheminement actuel
        TboTools_Debug.sout("Destination : path.xPos = " + path.xPos + ", path.yPos = " + path.yPos);
        TboTools_Debug.sout("Pos. Tour precedent : playersLastPosition.xPos = " + playersLastPosition.xPos + ", playersLastPosition.yPos = " + playersLastPosition.yPos);
        
        double a = ((double) path.yPos - (double) playersLastPosition.yPos) / ((double) path.xPos - (double) playersLastPosition.xPos); // a = (yB - yA) / (xB - xA)
        
        //Si les deux points sont trop proches, le déplacement parfaitement vertical, ou horizontal, "a" peut être en erreur. On attend donc la prochaine passe. Retour.
        TboTools_Debug.sout("a = " + a);
        if (Double.isNaN(a) || Double.isInfinite(a)) {
            playersLastPosition = new Position(qrCode.currenPlayerPosition);
            return;
        }
        double b = path.yPos - (a * path.xPos); // b = y - ax
        
        double y = a * qrCode.currenPlayerPosition.xPos + b; // y = ax + b
        
        // Regarder la remainingDistance restante
        double remainingDistance = Math.sqrt(Math.pow(path.xPos - qrCode.currenPlayerPosition.xPos, 2) + Math.pow(path.yPos - qrCode.currenPlayerPosition.yPos, 2));
        double traveledDistance  = Math.sqrt(Math.pow(playersLastPosition.xPos - qrCode.currenPlayerPosition.xPos, 2) + Math.pow(playersLastPosition.yPos - qrCode.currenPlayerPosition.yPos, 2));
        
        // puisque :
        //        a² = b² + c² − 2bc.cos(α)
        //        b² = a² + c² − 2ac.cos(β)
        //        c² = a² + b² − 2ab.cos(γ)
        // alors,
        // γ = arccos[(a² + b² − c²) ÷ 2ab]
        // et si c = lastRemainingDistance, b = remainingDistance et a = traveledDistance alors l'angle C, opposé à c =
        Double angleC = Math.acos((Math.pow(traveledDistance, 2) + Math.pow(remainingDistance, 2) - Math.pow(lastRemainingDistance, 2)) / (2d * traveledDistance * remainingDistance));
        angleB = Math.acos((Math.pow(traveledDistance, 2) + Math.pow(lastRemainingDistance, 2) - Math.pow(remainingDistance, 2)) / (2d * traveledDistance * lastRemainingDistance));
        
        // conversion de radians en degrés
        angleC = Math.toDegrees(angleC);
        angleB = Math.toDegrees(angleB);
    
        TboTools_Debug.sout(y + " = " + a + " * " + qrCode.currenPlayerPosition.xPos + " + " + b + "( Actual = " + qrCode.currenPlayerPosition.yPos + ")");
        TboTools_Debug.sout("lastRemainingDistance = " + lastRemainingDistance);
        TboTools_Debug.sout("remainingDistance = " + remainingDistance);
        TboTools_Debug.sout("traveledDistance = " + traveledDistance);
        TboTools_Debug.sout("angleC = " + angleC + "°");
        
        // Plus l'angle interne est grand, moins on doit tourner. Résultat en Milisecondes, partant du principe que 1000ms équivaut à un demi tour.
        int turnDuration = (int) (1000d / 180d * (180d - angleC));
        
        // En fonction de la droite, de la position et de la remainingDistance :
        
        // Tourner de turnDuration (milisecondes) en fonction de la position du personnage par rapport à la droite précédement calculée
        // Si à xA > xB, on se déplace d'est en ouest
        if (qrCode.currenPlayerPosition.xPos > path.xPos) {
            // Si yJoueur (là où le joueur est) > yCalculé (là où le joueur devrait être), le joueur est trop au sud par rapport à position idéale (les coordonnées en y étant inversées).
            if (qrCode.currenPlayerPosition.yPos > y) {
                turnRight(turnDuration);
            }
            // sinon le joueur est trop au nord
            else {
                turnLeft(turnDuration);
            }
        }
        // Si à xA < xB, on se déplace d'ouest en est
        else {
            if (qrCode.currenPlayerPosition.yPos > y) {
                turnLeft(turnDuration);
            }
            else {
                turnRight(turnDuration);
            }
        }
        
        lastRemainingDistance = remainingDistance;
        
        playersLastPosition = new Position(qrCode.currenPlayerPosition);
        
        // On passe au point de cheminement suivant si le point actuel est atteint
        if (remainingDistance <= traveledDistance * 1.5) {
            pathIndex++;
        }
        
        // Poney mod! xD
        int jump = (int) (Math.random() * 25);
        TboTools_Debug.sout("jump ? " + jump);
        if (jump == 1) { QrCode.pressKey(peripherals, KEY_SPACE); }
        if (traveledDistance < 460) { // 469 étant la distance moyenne dans l'eau, on ne peut pas faire plus sans prendre le risque de confondre
            closeStep++;
            TboTools_Debug.sout("closeStep = " + closeStep);
        }
        else { closeStep = 0; }
        if (closeStep > 10) { stuckProtocol(); }
        
        peripherals.robot.delay(100);
    }
    
    private void runStop() {
        
        peripherals.robot.keyPress(KeyEvent.VK_S);
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.keyRelease(KeyEvent.VK_S);
        isRunning = false;
    }
    
    private void runStart() {
    
        TboTools_Debug.sout("Run Start/Stop");
        peripherals.robot.keyPress(KEY_J);
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.keyRelease(KEY_J);
        isRunning = true;
    }
    
    private void turnLeft(int iTime) {
    
        TboTools_Debug.sout("angleB (Erreur de rotation au tour précédent) = " + (turnedRight ? "" : "-") + angleB + "°");
        
        if (isFlying && !isRunning) {
            peripherals.getKeyboard().typeString("j");
            isRunning = true;
        }
    
        TboTools_Debug.sout("Tourne à Gauche");
    
        peripherals.robot.keyPress(37);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(37);
    
        turnedRight = false;
    }
    
    private void turnRight(int iTime) {
    
        TboTools_Debug.sout("angleB (Erreur de rotation au tour précédent) = " + (turnedRight ? "-" : "") + angleB + "°");
        
        if (isFlying && !isRunning) {
            peripherals.getKeyboard().typeString("j");
            isRunning = true;
        }
    
        TboTools_Debug.sout("Tourne à Droite");
    
        peripherals.robot.keyPress(39);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(39);
    
        turnedRight = true;
    }
    
    private void stepBackward(int iTime) {
        
        TboTools_Debug.sout("Recule");
        
        peripherals.robot.keyPress(40);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(40);
    }
    
    private void stepForward(int iTime) {
        
        TboTools_Debug.sout("Avance");
        
        peripherals.robot.keyPress(38);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(38);
    }
    
    private void fly() throws AWTException {
        
        peripherals.getKeyboard().typeString("y");
        
        for (int n = 1; n <= 4; n++) {
            peripherals.robot.keyPress(KeyEvent.VK_HOME);
            peripherals.robot.keyRelease(KeyEvent.VK_HOME);
            peripherals.robot.delay(i_DELAY);
        }
        
        peripherals.robot.delay(2000);
        
        peripherals.getMouse().DragLeft2Right(10, InputEvent.BUTTON3_DOWN_MASK);
        
        for (int n = 1; n <= 3; n++) {
            peripherals.robot.keyPress(KeyEvent.VK_END);
            peripherals.robot.keyRelease(KeyEvent.VK_END);
            peripherals.robot.delay(i_DELAY);
        }
        
        peripherals.getKeyboard().typeString("j");
        isRunning = true;
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.keyPress(KEY_SPACE);
        peripherals.robot.delay(10000);
        peripherals.robot.keyRelease(KEY_SPACE);
        isFlying = true;
    }
}
