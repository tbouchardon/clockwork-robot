package fr.ksuto.clockwork.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fr.ksuto.clockwork.brain.perception.QrCodeV2Reader;
import fr.ksuto.clockwork.brain.perception.Route;
import fr.ksuto.clockwork.entities.ClkPosition;
import fr.ksuto.clockwork.entities.qrcode.QrCode;
import fr.ksuto.prh.PeripheralRobotHelper;

import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.Random;

public class TomTom {
    
    private static final Logger logger = LoggerFactory.getLogger(TomTom.class);
    
    private static final int                         KEY_SPACE             = 32;
    private static final int                         KEY_Y                 = 89;
    private static final int                         TURN_BACK_DURATION    = 100;
    private static final int                         TURN_DURATION         = 100;
    private final        Random                      random                = new Random();
    public               boolean                     isRunning             = false;
    private              PeripheralRobotHelper       peripherals;
    private              boolean                     isFlying              = false;
    private              ClkPosition                 playersLastPosition   = null;
    private              Double                      lastRemainingDistance = null;
    private              boolean                     turnedRight           = false;
    private              boolean                     turnedLeft            = false;
    private              int                         pathIndex             = 0;
    private              int                         routeRevision         = -1;
    private              boolean                     resumeFromNearest     = true;
    private              boolean                     finished              = false;
    private              String                      reported              = "";
    private              int                         closeStep             = 0;
    private              double                      angleB;
    
    TomTom(PeripheralRobotHelper peripherals) {
        
        this.peripherals = peripherals;
    }
    
    
    /**
     * Arrête la course automatique : un appui sur la flèche bas (reculer, raccourci par défaut de WoW) la coupe.
     */
    public void runStop() {
    
        if (!isRunning) {return;}
    
        peripherals.robot.keyPress(KeyEvent.VK_DOWN);
        peripherals.robot.delay(100);
        peripherals.robot.keyRelease(KeyEvent.VK_DOWN);
        isRunning = false;
    }
    
    public void stuckProtocol() {
    
        logger.debug("Trying to unstuck...");
    
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
            resumeFromNearest = true;
            finished = false;
            return;
        }
        if (playerHealth < 50) {
            return;
        }
        
        logger.debug("-------------------------------------------------------------------");
        
        // En cas de cible active (actions engagées) il y a moins de 2 secondes
        long lastActionDelay = System.currentTimeMillis() - lastActionTime;
        if (actionPossible || actionEnCours || Boolean.TRUE.equals(inCombat) || lastActionDelay < 2000) {
            // Arréter de courrir et retour en cas de cible active ; on repartira du point le plus proche
            runStop();
            resumeFromNearest = true;
            return;
        }
        
        // Parcours actif, tenu par l'addon : relu à chaque tour
        Route route = QrCodeV2Reader.route(qrCode.getCapturedQrCode());
        if (!route.exists() || route.points().isEmpty()) {
            runStop();
            report("Pilote automatique : aucun parcours actif, ou parcours vide (menu de l'addon)");
            return;
        }
        if (route.player() == null) {
            runStop();
            report("Pilote automatique : le personnage n'est pas sur la carte du parcours");
            return;
        }
        // Nouveau parcours, parcours modifié, démarrage ou reprise après un combat : rejoindre le point le plus proche
        if (route.revision() != routeRevision || resumeFromNearest) {
            pathIndex = route.nearestPoint();
            routeRevision = route.revision();
            resumeFromNearest = false;
            finished = false;
            playersLastPosition = null;
            report("Pilote automatique : vers le point " + (pathIndex + 1) + " sur " + route.points().size());
        }
        if (finished) {
            runStop();
            return;
        }
        
        // Fin du parcours : on recommence s'il boucle, sinon on s'arrête (l'addon garde le pilote allumé)
        if (pathIndex > route.points().size() - 1) {
            if (route.loop()) {
                logger.debug("loop");
                pathIndex = 0;
            }
            else {
                finished = true;
                runStop();
                report("Pilote automatique : parcours terminé");
                return;
            }
        }
        
        // Courrir
        if (!isRunning) {
            runStart();
        }
        
        ClkPosition destination          = position(route.points().get(pathIndex)); //coordonnées destination
        ClkPosition currenPlayerPosition = position(route.player()); //position du personage
        
        logger.debug("Position courante : currenPlayerPosition.xPos = " + currenPlayerPosition.xPos + ", currenPlayerPosition.yPos = " + currenPlayerPosition.yPos);
        if (playersLastPosition == null) {
            playersLastPosition = new ClkPosition(currenPlayerPosition);
            lastRemainingDistance = Math.sqrt(Math.pow((double) destination.xPos - currenPlayerPosition.xPos, 2) + Math.pow((double) destination.yPos - currenPlayerPosition.yPos, 2));
            return;
        }
        
        // Calcul de l'équation de la droite passant par la position précédente et le point de cheminement actuel
        logger.debug("Destination : destination.xPos = " + destination.xPos + ", destination.yPos = " + destination.yPos);
        logger.debug("Pos. Tour precedent : playersLastPosition.xPos = " + playersLastPosition.xPos + ", playersLastPosition.yPos = " + playersLastPosition.yPos);
        
        double a = ((double) destination.yPos - (double) playersLastPosition.yPos) / ((double) destination.xPos - (double) playersLastPosition.xPos); // a = (yB - yA) / (xB - xA)
        
        //Si les deux points sont trop proches, le déplacement parfaitement vertical, ou horizontal, "a" peut être en erreur. On attend donc la prochaine passe. Retour.
        logger.debug("a = " + a);
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
        
        logger.debug(y + " = " + a + " * " + currenPlayerPosition.xPos + " + " + b + "( Actual = " + currenPlayerPosition.yPos + ")");
        logger.debug("lastRemainingDistance = " + lastRemainingDistance);
        logger.debug("remainingDistance = " + remainingDistance);
        logger.debug("traveledDistance = " + traveledDistance);
        logger.debug("angleC = " + angleC + "°");
        
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
        int jump = random.nextInt(25);
        logger.debug("jump ? " + jump);
        if (jump == 1) {peripherals.getKeyboard().pressKey(KEY_SPACE);}
        if (traveledDistance < 460) { // 469 étant la distance moyenne dans l’eau, on ne peut pas faire plus sans prendre le risque de confondre
            closeStep++;
            logger.debug("closeStep = " + closeStep);
        }
        else {closeStep = 0;}
        if (closeStep > 10) {stuckProtocol();}
    
        peripherals.robot.delay(100);
    }
    
    /**
     * Position sur la carte en millionièmes (unité des seuils de distance ci-dessus, héritée de l'ancien codage).
     */
    private static ClkPosition position(Point2D.Double fraction) {
        
        ClkPosition position = new ClkPosition();
        position.xPos = (int) Math.round(fraction.x * 1_000_000);
        position.yPos = (int) Math.round(fraction.y * 1_000_000);
        return position;
    }
    
    /**
     * Journalise un état du pilote quand il change.
     */
    private void report(String state) {
        
        if (state.equals(reported)) {return;}
        reported = state;
        logger.info(state);
    }
    
    /**
     * Lance la course automatique : Alt+Maj+V, posé en surcharge par l'addon (bindings.lua, TOGGLEAUTORUN), pour ne pas
     * dépendre des raccourcis du joueur.
     */
    private void runStart() {
    
        if (isRunning) {return;}
        logger.debug("Course automatique");
        peripherals.getKeyboard().pressKey(KeyEvent.VK_V, true, false, true);
        isRunning = true;
    }
    
    private void stepBackward(int iTime) {
        
        logger.debug("Recule");
        
        peripherals.robot.keyPress(40);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(40);
    }
    
    private void stepForward(int iTime) {
        
        logger.debug("Avance");
        
        peripherals.robot.keyPress(38);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(38);
    }
    
    private void turnLeft(int iTime) {
        
        logger.debug("angleB (Erreur de rotation au tour précédent) = " + (turnedRight ? "" : "-") + angleB + "°");
        
        if (isFlying && !isRunning) {runStart();}
        
        logger.debug("Tourne à Gauche");
        
        peripherals.robot.keyPress(37);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(37);
        
        turnedRight = false;
        turnedLeft = true;
    }
    
    private void turnRight(int iTime) {
        
        logger.debug("angleB (Erreur de rotation au tour précédent) = " + (turnedRight ? "-" : "") + angleB + "°");
        
        if (isFlying && !isRunning) {runStart();}
        
        logger.debug("Tourne à Droite");
        
        peripherals.robot.keyPress(39);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(39);
        
        turnedRight = true;
        turnedLeft = false;
    }
}
