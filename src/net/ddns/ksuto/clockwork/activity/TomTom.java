package net.ddns.ksuto.clockwork.activity;

import static net.ddns.ksuto.prh.properties.Constants.i_DELAY;

import net.ddns.ksuto.clockwork.entities.Position;
import net.ddns.ksuto.clockwork.entities.middleman.MiddleMan;
import net.ddns.ksuto.clockwork.tools.RGBConverter;
import net.ddns.ksuto.prh.TBoPeripheralRobotHelper;

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
    private double angleB;
    
    TomTom(TBoPeripheralRobotHelper peripherals) throws AWTException {
        
        this.peripherals = peripherals;
    }
    
    public void addCurrentPositionToPathList(MiddleMan middleMan, TBoPeripheralRobotHelper peripherals) {
        
        getCoordinates(middleMan, peripherals);
    }
    
    public void getCoordinates(MiddleMan middleMan, TBoPeripheralRobotHelper peripherals) {
        
        System.out.println("getCoordinates");
        
        BufferedImage biCapturedScreen = peripherals.robot.createScreenCapture(new Rectangle(0, 0, peripherals.getScreen().i_SCREEN_WIDTH, peripherals.getScreen().i_SCREEN_HEIGHT));
    
        int xPos = 0;
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 6, middleMan.position.yPosition + 7) == RGBConverter.WHITE) { xPos += 524288; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 7, middleMan.position.yPosition + 7) == RGBConverter.WHITE) { xPos += 262144; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 8, middleMan.position.yPosition + 7) == RGBConverter.WHITE) { xPos += 131072; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 9, middleMan.position.yPosition + 7) == RGBConverter.WHITE) { xPos += 65536; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 10, middleMan.position.yPosition + 7) == RGBConverter.WHITE) { xPos += 32768; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 11, middleMan.position.yPosition + 7) == RGBConverter.WHITE) { xPos += 16384; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 12, middleMan.position.yPosition + 7) == RGBConverter.WHITE) { xPos += 8192; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 13, middleMan.position.yPosition + 7) == RGBConverter.WHITE) { xPos += 4096; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 5) == RGBConverter.WHITE) { xPos += 2048; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 5) == RGBConverter.WHITE) { xPos += 1024; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 5) == RGBConverter.WHITE) { xPos += 512; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 5) == RGBConverter.WHITE) { xPos += 256; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 5) == RGBConverter.WHITE) { xPos += 128; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 5) == RGBConverter.WHITE) { xPos += 64; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 5) == RGBConverter.WHITE) { xPos += 32; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 5) == RGBConverter.WHITE) { xPos += 16; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 5) == RGBConverter.WHITE) { xPos += 8; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 5) == RGBConverter.WHITE) { xPos += 4; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 5) == RGBConverter.WHITE) { xPos += 2; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 5) == RGBConverter.WHITE) { xPos += 1; }
        middleMan.currenPlayerPosition.xPos = xPos;
    
        int yPos = 0;
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 6, middleMan.position.yPosition + 10) == RGBConverter.WHITE) { yPos += 524288; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 7, middleMan.position.yPosition + 10) == RGBConverter.WHITE) { yPos += 262144; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 8, middleMan.position.yPosition + 10) == RGBConverter.WHITE) { yPos += 131072; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 9, middleMan.position.yPosition + 10) == RGBConverter.WHITE) { yPos += 65536; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 10, middleMan.position.yPosition + 10) == RGBConverter.WHITE) { yPos += 32768; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 11, middleMan.position.yPosition + 10) == RGBConverter.WHITE) { yPos += 16384; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 12, middleMan.position.yPosition + 10) == RGBConverter.WHITE) { yPos += 8192; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 13, middleMan.position.yPosition + 10) == RGBConverter.WHITE) { yPos += 4096; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 11) == RGBConverter.WHITE) { yPos += 2048; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 11) == RGBConverter.WHITE) { yPos += 1024; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 11) == RGBConverter.WHITE) { yPos += 512; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 11) == RGBConverter.WHITE) { yPos += 256; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 11) == RGBConverter.WHITE) { yPos += 128; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 11) == RGBConverter.WHITE) { yPos += 64; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 11) == RGBConverter.WHITE) { yPos += 32; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 11) == RGBConverter.WHITE) { yPos += 16; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 11) == RGBConverter.WHITE) { yPos += 8; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 11) == RGBConverter.WHITE) { yPos += 4; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 11) == RGBConverter.WHITE) { yPos += 2; }
        if (biCapturedScreen.getRGB(middleMan.position.xPosition + 2, middleMan.position.yPosition + 11) == RGBConverter.WHITE) { yPos += 1; }
        middleMan.currenPlayerPosition.yPos = yPos;
    }
    
    void drive(MiddleMan middleMan, TBoPeripheralRobotHelper peripherals, boolean activeTarget) {
        
        System.out.println("--------------------------------- drive ----------------------------------");
        
        // Arréter de courrir et retour en cas de cible active
        if (isRunning && activeTarget) {
            runStop();
            return;
        }
        
        if (!isRunning && activeTarget) {
            return;
        }
        
        // Arrêter de courrir et retour si il n'y a plus de points de cheminement
        if (middleMan.path.isEmpty()) {
            pathIndex = 0;
            if (isRunning) {
                runStop();
            }
            return;
        }
        
        // Courrir
        if (!isRunning) {
            runStart();
        }
        
        // Retourner au point de départ si la fonction LOOP est activée et qu'il n'y a plus de points de cheminement
        if (middleMan.DRIVE_LOOP.active && pathIndex > middleMan.path.size() - 1) {
            System.out.println("loop");
            pathIndex = 0;
        }
        
        // Si l'on est à cours de points de cheminement, on vide la liste, on arrête de courrir et retour
        if (pathIndex > middleMan.path.size() - 1) {
            middleMan.path.clear();
            if (isRunning) {
                runStop();
            }
            pathIndex = 0;
            return;
        }
        
        Position path = middleMan.path.get(pathIndex); //coordonnées destination
        getCoordinates(middleMan, peripherals); //position du personage
    
        System.out.println("Position courante : middleMan.currenPlayerPosition.xPos = " + middleMan.currenPlayerPosition.xPos + ", middleMan.currenPlayerPosition.yPos = " + middleMan
                                                                                                                                                                                     .currenPlayerPosition.yPos);
        if (playersLastPosition == null) {
            playersLastPosition = new Position(middleMan.currenPlayerPosition);
            lastRemainingDistance = Math.sqrt(Math.pow(path.xPos - middleMan.currenPlayerPosition.xPos, 2) + Math.pow(path.yPos - middleMan.currenPlayerPosition.yPos, 2));
            return;
        }
        
        // Calcul de l'équation de la droite passant par la position précédente et le point de cheminement actuel
        System.out.println("Destination : path.xPos = " + path.xPos + ", path.yPos = " + path.yPos);
        System.out.println("Pos. Tour precedent : playersLastPosition.xPos = " + playersLastPosition.xPos + ", playersLastPosition.yPos = " + playersLastPosition.yPos);
    
        double a = (path.yPos - playersLastPosition.yPos) / (path.xPos - playersLastPosition.xPos); // a = (yB - yA) / (xB - xA)
        
        //Si les deux points sont trop proches, le déplacement parfaitement vertical, ou horizontal, "a" peut être en erreur. On attend donc la prochaine passe. Retour.
        System.out.println("a = " + a);
        if (Double.isNaN(a) || Double.isInfinite(a)) {
            return;
        }
        double b = path.yPos - (a * path.xPos); // b = y - ax
    
        double y = a * middleMan.currenPlayerPosition.xPos + b; // y = ax + b
    
        // Regarder la remainingDistance restante
        double remainingDistance = Math.sqrt(Math.pow(path.xPos - middleMan.currenPlayerPosition.xPos, 2) + Math.pow(path.yPos - middleMan.currenPlayerPosition.yPos, 2));
        double traveledDistance  = Math.sqrt(Math.pow(playersLastPosition.xPos - middleMan.currenPlayerPosition.xPos, 2) + Math.pow(playersLastPosition.yPos - middleMan.currenPlayerPosition.yPos, 2));
        
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
    
        System.out.println(y + " = " + a + " * " + middleMan.currenPlayerPosition.xPos + " + " + b + "( Actual = " + middleMan.currenPlayerPosition.yPos + ")");
        System.out.println("lastRemainingDistance = " + lastRemainingDistance * 100d);
        System.out.println("remainingDistance = " + remainingDistance * 100d);
        System.out.println("traveledDistance = " + traveledDistance * 100d);
        System.out.println("angleC = " + angleC + "°");
    
        // Plus l'angle interne est grand, moins on doit tourner. Résultat en Milisecondes, partant du principe que 1000ms équivaut à un demi tour.
        int turnDuration = (int) (1000d / 180d * (180d - angleC));
    
        // En fonction de la droite, de la position et de la remainingDistance :
        
        // Tourner de turnDuration (milisecondes) en fonction de la position du personnage par rapport à la droite précédement calculée
        // Si à xA > xB, on se déplace d'est en ouest
        if (middleMan.currenPlayerPosition.xPos < path.xPos) { //TODO : Trouver l'incohérence.
            // Si yJoueur (là où le joueur est) > yCalculé (là où le joueur devrait être), le joueur est trop au sud par rapport à position idéale (les coordonnées en y étant inversées).
            if (middleMan.currenPlayerPosition.yPos > y) {
                turnRight(turnDuration);
            }
            // sinon le joueur est trop au nord
            else {
                turnLeft(turnDuration);
            }
        }
        // Si à xA < xB, on se déplace d'ouest en est
        else {
            if (middleMan.currenPlayerPosition.yPos > y) {
                turnLeft(turnDuration);
            }
            else {
                turnRight(turnDuration);
            }
        }
    
        lastRemainingDistance = remainingDistance;
        playersLastPosition = new Position(middleMan.currenPlayerPosition);
        
        // On passe au point de cheminement suivant si le point actuel est atteint
        if (remainingDistance <= traveledDistance * 2) {
            pathIndex++;
        }
        
        peripherals.robot.delay(100);
    }
    
    private void runStop() {
        
        peripherals.robot.keyPress(KeyEvent.VK_S);
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.keyRelease(KeyEvent.VK_S);
        isRunning = false;
    }
    
    private void runStart() {
        
        peripherals.robot.keyPress(KEY_J);
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.keyRelease(KEY_J);
        isRunning = true;
    }
    
    @SuppressWarnings("Duplicates")
    private void turnLeft(int iTime) {
    
        System.out.println("angleB (Erreur de rotation au tour précédent) = " + (turnedRight ? "" : "-") + angleB + "°");
        
        if (isFlying && !isRunning) {
            peripherals.getKeyboard().typeString("j");
            isRunning = true;
        }
        System.out.println("Tourne à Gauche");
        peripherals.robot.keyPress(37);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(37);
    
        turnedRight = false;
    }
    
    @SuppressWarnings("Duplicates")
    private void turnRight(int iTime) {
    
        System.out.println("angleB (Erreur de rotation au tour précédent) = " + (turnedRight ? "-" : "") + angleB + "°");
        
        if (isFlying && !isRunning) {
            peripherals.getKeyboard().typeString("j");
            isRunning = true;
        }
        System.out.println("Tourne à Droite");
        peripherals.robot.keyPress(39);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(39);
    
        turnedRight = true;
    }
    
    //	public void land() throws AWTException {}
    
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
