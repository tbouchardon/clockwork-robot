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
    private boolean  isFlying         = false;
    private boolean  isRunning        = false;
    private double[] lastPlayerCoords = null;
    private Double   lastDistance     = null;
    private int      pathIndex        = 0;
    
    TomTom(TBoPeripheralRobotHelper peripherals) throws AWTException {
        
        this.peripherals = peripherals;
    }
    
    public void addCurrentPositionToPathList(MiddleMan middleMan, TBoPeripheralRobotHelper peripherals) {
        
        getCoordinates(middleMan, peripherals);
    }
    
    public void getCoordinates(MiddleMan middleMan, TBoPeripheralRobotHelper peripherals) {
        
        System.out.println("getCoordinates");
        
        BufferedImage biCapturedScreen = peripherals.robot.createScreenCapture(new Rectangle(0, 0, peripherals.getScreen().i_SCREEN_WIDTH, peripherals.getScreen().i_SCREEN_HEIGHT));
        
        for (int i = 2; i < 12; i++) {
            if (biCapturedScreen.getRGB(middleMan.position.xPosition + i, middleMan.position.yPosition + 5) == RGBConverter.WHITE) {
                middleMan.currenPosition.xPos_Xxxx = i - 2;
                break;
            }
        }
        
        for (int i = 2; i < 12; i++) {
            if (biCapturedScreen.getRGB(middleMan.position.xPosition + i, middleMan.position.yPosition + 6) == RGBConverter.WHITE) {
                middleMan.currenPosition.xPos_xXxx = i - 2;
                break;
            }
        }
        
        for (int i = 2; i < 12; i++) {
            if (biCapturedScreen.getRGB(middleMan.position.xPosition + i, middleMan.position.yPosition + 7) == RGBConverter.WHITE) {
                middleMan.currenPosition.xPos_xxXx = i - 2;
                break;
            }
        }
        
        for (int i = 2; i < 12; i++) {
            if (biCapturedScreen.getRGB(middleMan.position.xPosition + i, middleMan.position.yPosition + 8) == RGBConverter.WHITE) {
                middleMan.currenPosition.xPos_xxxX = i - 2;
                break;
            }
        }
        
        for (int i = 2; i < 12; i++) {
            if (biCapturedScreen.getRGB(middleMan.position.xPosition + i, middleMan.position.yPosition + 9) == RGBConverter.WHITE) {
                middleMan.currenPosition.yPos_Xxxx = i - 2;
                break;
            }
        }
        
        for (int i = 2; i < 12; i++) {
            if (biCapturedScreen.getRGB(middleMan.position.xPosition + i, middleMan.position.yPosition + 10) == RGBConverter.WHITE) {
                middleMan.currenPosition.yPos_xXxx = i - 2;
                break;
            }
        }
        
        for (int i = 2; i < 12; i++) {
            if (biCapturedScreen.getRGB(middleMan.position.xPosition + i, middleMan.position.yPosition + 11) == RGBConverter.WHITE) {
                middleMan.currenPosition.yPos_xxXx = i - 2;
                break;
            }
        }
        
        for (int i = 2; i < 12; i++) {
            if (biCapturedScreen.getRGB(middleMan.position.xPosition + i, middleMan.position.yPosition + 12) == RGBConverter.WHITE) {
                middleMan.currenPosition.yPos_xxxX = i - 2;
                break;
            }
        }
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
        
        double[] playerCoords = middleMan.currenPosition.getCoordinates(); //les coordonnées actuelles (x,y)
        System.out.println("Position courante : playerCoords[0] = " + playerCoords[0] + ", playerCoords[1] = " + playerCoords[1]);
        if (lastPlayerCoords == null) {
            lastPlayerCoords = playerCoords;
            lastDistance = Math.sqrt(Math.pow(path.getCoordinates()[0] - playerCoords[0], 2) + Math.pow(path.getCoordinates()[1] - playerCoords[1], 2));
            return;
        }
        
        // Calcul de l'équation de la droite passant par la position précédente et le point de cheminement actuel
        System.out.println("Destination : path.getCoordinates()[0] = " + path.getCoordinates()[0] + ", path.getCoordinates()[1] = " + path.getCoordinates()[1]);
        System.out.println("Pos. Tour precedent : lastPlayerCoords[0] = " + lastPlayerCoords[0] + ", lastPlayerCoords[1] = " + lastPlayerCoords[1]);
        
        double a = (path.getCoordinates()[1] - lastPlayerCoords[1]) / (path.getCoordinates()[0] - lastPlayerCoords[0]); // a = (yB - yA) / (xB - xA)
        
        //Si les deux points sont trop proches, le déplacement parfaitement vertical, ou horizontal, "a" peut être en erreur. On attend donc la prochaine passe. Retour.
        System.out.println("a = " + a);
        if (Double.isNaN(a) || Double.isInfinite(a)) {
            return;
        }
        double b = path.getCoordinates()[1] - (a * path.getCoordinates()[0]); // b = y - ax
        
        double y = a * playerCoords[0] + b; // y = ax + b
        
        // Regarder la distance restante
        double distance         = Math.sqrt(Math.pow(path.getCoordinates()[0] - playerCoords[0], 2) + Math.pow(path.getCoordinates()[1] - playerCoords[1], 2));
        double traveledDistance = Math.sqrt(Math.pow(lastPlayerCoords[0] - playerCoords[0], 2) + Math.pow(lastPlayerCoords[1] - playerCoords[1], 2));
        
        // puisque :
        //        a² = b² + c² − 2bc.cos(α)
        //        b² = a² + c² − 2ac.cos(β)
        //        c² = a² + b² − 2ab.cos(γ)
        // alors,
        // γ = arccos[(a² + b² − c²) ÷ 2ab]
        // et si c = lastDistance, b = distance et a = traveledDistance alors l'angle C, opposé à c =
        Double angleC = Math.acos((Math.pow(traveledDistance, 2) + Math.pow(distance, 2) - Math.pow(lastDistance, 2)) / (2L * traveledDistance * distance));
        
        System.out.println(y + " = " + a + " * " + playerCoords[0] + " + " + b + "( Actual = " + playerCoords[1] + ")");
        System.out.println("distance = " + distance * 100L);
        System.out.println("traveledDistance = " + traveledDistance * 100L);
        System.out.println("angleC = " + angleC + "°");
        
        // Plus l'angle est grand, moins on doit tourner. Résultat en Milisecondes, partant du principe que 1000ms équivaut à un demi tour.
        int turnDuration = (int) (1000L / 180L * (180L - angleC));
        
        // En fonction de la droite, de la position et de la distance :
        
        // Faire demi tour si l'on s'éloigne
        // if (distance > lastDistance && Math.abs(distance - lastDistance) > 0.01) {
        //        if (distance > lastDistance) {
        //            System.out.println("Last distance = " + lastDistance * 100L + " vs Current distance = " + distance * 100L);
        //            System.out.println("Demi tour");
        //            runStop();
        //            if (playerCoords[0] > path.getCoordinates()[0]) {
        //                // Si yJoueur > yCalculé, le joueur est trop au sud par rapport à la droite
        //                if (playerCoords[1] > y) {
        //                    turnRight(TURN_BACK_DURATION);
        //                }
        //                // sinon le joueur est trop au nord
        //                else {
        //                    turnLeft(TURN_BACK_DURATION);
        //                }
        //            }
        //            // Si à xA < xB, on se déplace d'ouest en est
        //            else {
        //                if (playerCoords[1] > y) {
        //                    turnLeft(TURN_BACK_DURATION);
        //                }
        //                else {
        //                    turnRight(TURN_BACK_DURATION);
        //                }
        //            }
        //            runStart();
        //        }
        // Sinon,
        //        else {
        
        // Tourner de turnDuration (milisecondes) en fonction de la position du personnage par rapport à la droite précédement calculée
        // Si à xA > xB, on se déplace d'est en ouest
        if (playerCoords[0] < path.getCoordinates()[0]) { //TODO : Trouver l'incohérence.
            // Si yJoueur (là où le joueur est) > yCalculé (là où le joueur devrait être), le joueur est trop au sud par rapport à position idéale (les coordonnées en y étant inversées).
            if (playerCoords[1] > y) {
                turnRight(turnDuration);
            }
            // sinon le joueur est trop au nord
            else {
                turnLeft(turnDuration);
            }
        }
        // Si à xA < xB, on se déplace d'ouest en est
        else {
            if (playerCoords[1] > y) {
                turnLeft(turnDuration);
            }
            else {
                turnRight(turnDuration);
            }
        }
        //        }
        
        lastDistance = distance;
        lastPlayerCoords = playerCoords;
        
        // On passe au point de cheminement suivant si le point actuel est atteint
        if (distance <= 0.40) {
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
        
        if (isFlying && !isRunning) {
            peripherals.getKeyboard().typeString("j");
            isRunning = true;
        }
        System.out.println("Tourne à Gauche");
        peripherals.robot.keyPress(37);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(37);
    }
    
    @SuppressWarnings("Duplicates")
    private void turnRight(int iTime) {
        
        if (isFlying && !isRunning) {
            peripherals.getKeyboard().typeString("j");
            isRunning = true;
        }
        System.out.println("Tourne à Droite");
        peripherals.robot.keyPress(39);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(39);
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
