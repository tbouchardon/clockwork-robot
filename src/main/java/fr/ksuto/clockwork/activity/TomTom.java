package fr.ksuto.clockwork.activity;

import static fr.ksuto.prh.properties.Constants.i_DELAY;

import fr.ksuto.clockwork.entities.ClkPosition;
import fr.ksuto.clockwork.entities.qrcode.QrCode;
import fr.ksuto.clockwork.tools.RGBConverter;
import fr.ksuto.prh.PeripheralRobotHelper;
import fr.ksuto.tools.Debug;

import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class TomTom {
    
    private static final int                         KEY_J                 = 74;
    private static final int                         KEY_SPACE             = 32;
    private static final int                         KEY_Y                 = 89;
    private static final int                         TURN_BACK_DURATION    = 100;
    private static final int                         TURN_DURATION         = 100;
    private final        Random                      random                = new Random();
    public               boolean                     isRunning             = false;
    public               java.util.List<ClkPosition> path                  = new ArrayList<>();
    private              PeripheralRobotHelper       peripherals;
    private              boolean                     isFlying              = false;
    private              ClkPosition                 playersLastPosition   = null;
    private              Double                      lastRemainingDistance = null;
    private              boolean                     turnedRight           = false;
    private              boolean                     turnedLeft            = false;
    private              int                         pathIndex             = 0;
    private              int                         closeStep             = 0;
    private              double                      angleB;
    
    TomTom(PeripheralRobotHelper peripherals) {
        
        this.peripherals = peripherals;
    }
    
    @SuppressWarnings("unused")
    public void addCurrentPositionToPathList(QrCode qrCode, PeripheralRobotHelper peripherals) {
        
        getCoordinates(qrCode, peripherals);
    }
    
    public void addWayPoint(QrCode qrCode) {
        
        ClkPosition currenPlayerPosition = getCoordinates(qrCode, peripherals);
        path.add(new ClkPosition(currenPlayerPosition));
        String output = "";
        for (ClkPosition position : path) {
            String formattedX = String.format("%06d", position.xPos);
            String formattedY = String.format("%06d", position.yPos);
            output += formattedX.substring(0, 2) + "," + formattedX.substring(2, 4) + "-" +
                      formattedY.substring(0, 2) + "," + formattedY.substring(2, 4) + ";";
        }
        Debug.sout(output);
        QrCode.typeInChat(peripherals, "/clk wpadded");
        peripherals.robot.delay(500);
    }
    
    public void clearWayPoints() {
        
        path.clear();
        pathIndex = 0;
        QrCode.typeInChat(peripherals, "/clk wpcleared");
        peripherals.robot.delay(500);
    }
    
    public ClkPosition getCoordinates(QrCode qrCode, PeripheralRobotHelper peripherals) {
        
        Debug.sout("getCoordinates");
        
        BufferedImage biCapturedScreen     = qrCode.captureQrCode(peripherals);
        ClkPosition   currenPlayerPosition = new ClkPosition();
        
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
        currenPlayerPosition.xPos = xPos;
        
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
        currenPlayerPosition.yPos = yPos;
        
        return currenPlayerPosition;
    }
    
    public void runStop() {
    
        if (!isRunning) {return;}
    
        peripherals.robot.keyPress(KeyEvent.VK_S);
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.keyRelease(KeyEvent.VK_S);
        isRunning = false;
    }
    
    public void stuckProtocol() {
    
        Debug.sout("Trying to unstuck...");
    
        runStop();
    
        stepBackward(500);
    
        peripherals.robot.keyPress(40); // Recule
    
        peripherals.robot.keyPress(32); // Saute
        peripherals.robot.delay(100);
        peripherals.robot.keyRelease(32); // Stop Saute
    
        peripherals.robot.delay(900);
    
        peripherals.robot.keyPress(32); // Saute
        
        if (turnedRight) {turnLeft(500);}
        else {turnRight(500);} // Tourne
        
        peripherals.robot.keyRelease(32); // Stop Saute
        peripherals.robot.keyRelease(40); // Stop Recule
        
        stepForward(2000); // Avance
        
        runStart();
    }
    
    void drive(QrCode qrCode, PeripheralRobotHelper peripherals, boolean actionPossible, boolean actionEnCours, Boolean inCombat, long lastActionTime, double playerHealth) {
        
        if (!qrCode.DRIVE_MOD.active) {
            if (isRunning) {runStop();}
            return;
        }
        if (playerHealth < 50) {
            return;
        }
        
        Debug.sout("-------------------------------------------------------------------");
        
        // En cas de cible active (actions engagées) il y a moins de 2 secondes
        long lastActionDelay = System.currentTimeMillis() - lastActionTime;
        if (actionPossible || actionEnCours || Boolean.TRUE.equals(inCombat) || lastActionDelay < 2000) {
            // Passage en caméra position combat (Pour pouvoir loot plus facilement)
            qrCode.cameraCombat(peripherals);
    
            // Arréter de courrir et retour en cas de cible active
            runStop();
            return;
        }
        
        // Passage en camera position course
        qrCode.cameraDrive(peripherals);
        
        // Arrêter de courrir et retour si il n'y a plus de points de cheminement
        if (path.isEmpty()) {
            pathIndex = 0;
            runStop();
            QrCode.typeInChat(peripherals, "/clk drive");
            return;
        }
        
        // Courrir
        if (!isRunning) {
            runStart();
        }
        
        // Retourner au point de départ si la fonction LOOP est activée et qu'il n'y a plus de points de cheminement
        if (qrCode.DRIVE_LOOP.active && pathIndex > path.size() - 1) {
            Debug.sout("loop");
            pathIndex = 0;
        }
    
        // Si l’on est à court de points de cheminement, on vide la liste, on arrête de courrir et retour
        if (pathIndex > path.size() - 1) {
            path.clear();
            runStop();
            pathIndex = 0;
            return;
        }
        
        ClkPosition destination          = path.get(pathIndex); //coordonnées destination
        ClkPosition currenPlayerPosition = getCoordinates(qrCode, peripherals); //position du personage
        
        Debug.sout("Position courante : currenPlayerPosition.xPos = " + currenPlayerPosition.xPos + ", currenPlayerPosition.yPos = " + currenPlayerPosition.yPos);
        if (playersLastPosition == null) {
            playersLastPosition = new ClkPosition(currenPlayerPosition);
            lastRemainingDistance = Math.sqrt(Math.pow((double) destination.xPos - currenPlayerPosition.xPos, 2) + Math.pow((double) destination.yPos - currenPlayerPosition.yPos, 2));
            return;
        }
        
        // Calcul de l'équation de la droite passant par la position précédente et le point de cheminement actuel
        Debug.sout("Destination : destination.xPos = " + destination.xPos + ", destination.yPos = " + destination.yPos);
        Debug.sout("Pos. Tour precedent : playersLastPosition.xPos = " + playersLastPosition.xPos + ", playersLastPosition.yPos = " + playersLastPosition.yPos);
        
        double a = ((double) destination.yPos - (double) playersLastPosition.yPos) / ((double) destination.xPos - (double) playersLastPosition.xPos); // a = (yB - yA) / (xB - xA)
        
        //Si les deux points sont trop proches, le déplacement parfaitement vertical, ou horizontal, "a" peut être en erreur. On attend donc la prochaine passe. Retour.
        Debug.sout("a = " + a);
        if (Double.isNaN(a) || Double.isInfinite(a)) {
            playersLastPosition = new ClkPosition(currenPlayerPosition);
            return;
        }
        double b = destination.yPos - (a * destination.xPos); // b = y - ax
    
        double y = a * currenPlayerPosition.xPos + b; // y = ax + b
    
        // Regarder la remainingDistance restante
        double remainingDistance = Math.sqrt(Math.pow((double) destination.xPos - currenPlayerPosition.xPos, 2) + Math.pow((double) destination.yPos - currenPlayerPosition.yPos, 2));
        double traveledDistance  = Math.sqrt(Math.pow((double) playersLastPosition.xPos - currenPlayerPosition.xPos, 2) + Math.pow((double) playersLastPosition.yPos - currenPlayerPosition.yPos, 2));
    
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
        
        Debug.sout(y + " = " + a + " * " + currenPlayerPosition.xPos + " + " + b + "( Actual = " + currenPlayerPosition.yPos + ")");
        Debug.sout("lastRemainingDistance = " + lastRemainingDistance);
        Debug.sout("remainingDistance = " + remainingDistance);
        Debug.sout("traveledDistance = " + traveledDistance);
        Debug.sout("angleC = " + angleC + "°");
        
        // Plus l'angle interne est grand, moins on doit tourner. Résultat en Milisecondes, partant du principe que 1000ms équivaut à un demi tour.
        int turnDuration = (int) (1000d / 180d * (180d - angleC));
        
        // En fonction de la droite, de la position et de la remainingDistance :
        
        // Tourner de turnDuration (milisecondes) en fonction de la position du personnage par rapport à la droite précédement calculée
        // Si à xA > xB, on se déplace d'est en ouest
        if (currenPlayerPosition.xPos > destination.xPos) {
            // Si yJoueur (là où le joueur est) > yCalculé (là où le joueur devrait être), le joueur est trop au sud par rapport à position idéale (les coordonnées en y étant inversées).
            if (currenPlayerPosition.yPos > y) {
                turnRight(turnDuration);
            }
            // sinon le joueur est trop au nord
            else {
                turnLeft(turnDuration);
            }
        }
        // Si à xA < xB, on se déplace d'ouest en est
        else {
            if (currenPlayerPosition.yPos > y) {
                turnLeft(turnDuration);
            }
            else {
                turnRight(turnDuration);
            }
        }
    
        lastRemainingDistance = remainingDistance;
    
        playersLastPosition = new ClkPosition(currenPlayerPosition);
    
        // On passe au point de cheminement suivant si le point actuel est atteint
        if (remainingDistance <= traveledDistance * 1.5) {
            pathIndex++;
        }
    
        // Poney mod! xD
        int jump = (random.nextInt() * 25);
        Debug.sout("jump ? " + jump);
        if (jump == 1) {peripherals.getKeyboard().pressKey(KEY_SPACE);}
        if (traveledDistance < 460) { // 469 étant la distance moyenne dans l’eau, on ne peut pas faire plus sans prendre le risque de confondre
            closeStep++;
            Debug.sout("closeStep = " + closeStep);
        }
        else {closeStep = 0;}
        if (closeStep > 10) {stuckProtocol();}
    
        peripherals.robot.delay(100);
    }
    
    private void runStart() {
    
        if (isRunning) {return;}
        Debug.sout("Run Start/Stop");
        peripherals.robot.keyPress(KEY_J);
        peripherals.robot.delay(i_DELAY);
        peripherals.robot.keyRelease(KEY_J);
        isRunning = true;
    }
    
    private void stepBackward(int iTime) {
        
        Debug.sout("Recule");
        
        peripherals.robot.keyPress(40);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(40);
    }
    
    private void stepForward(int iTime) {
        
        Debug.sout("Avance");
        
        peripherals.robot.keyPress(38);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(38);
    }
    
    private void turnLeft(int iTime) {
        
        Debug.sout("angleB (Erreur de rotation au tour précédent) = " + (turnedRight ? "" : "-") + angleB + "°");
        
        if (isFlying && !isRunning) {
            peripherals.getKeyboard().typeString("j");
            isRunning = true;
        }
        
        Debug.sout("Tourne à Gauche");
        
        peripherals.robot.keyPress(37);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(37);
        
        turnedRight = false;
        turnedLeft = true;
    }
    
    private void turnRight(int iTime) {
        
        Debug.sout("angleB (Erreur de rotation au tour précédent) = " + (turnedRight ? "-" : "") + angleB + "°");
        
        if (isFlying && !isRunning) {
            peripherals.getKeyboard().typeString("j");
            isRunning = true;
        }
        
        Debug.sout("Tourne à Droite");
        
        peripherals.robot.keyPress(39);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(39);
        
        turnedRight = true;
        turnedLeft = false;
    }
}
