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
            // Arrêter de courir en cas de cible active ; on repartira vers le point visé avant le combat
            runStop();
            playersLastPosition = null;
            lastRemainingDistance = null;
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
        // Nouveau parcours, parcours modifié ou démarrage : rejoindre le point le plus proche, ou le suivant s'il est déjà
        // dépassé. Après un combat, le point visé reste le même
        if (route.revision() != routeRevision || resumeFromNearest) {
            pathIndex = route.startPoint();
            routeRevision = route.revision();
            resumeFromNearest = false;
            finished = false;
            playersLastPosition = null;
            lastRemainingDistance = null;
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
            lastRemainingDistance = Math.hypot((double) destination.xPos - currenPlayerPosition.xPos, (double) destination.yPos - currenPlayerPosition.yPos);
            return;
        }
        
        double remainingDistance = Math.hypot((double) destination.xPos - currenPlayerPosition.xPos, (double) destination.yPos - currenPlayerPosition.yPos);
        double traveledDistance  = Math.hypot((double) playersLastPosition.xPos - currenPlayerPosition.xPos, (double) playersLastPosition.yPos - currenPlayerPosition.yPos);
        // Immobile depuis le tour précédent : pas de direction mesurable, on attend le tour suivant (le blocage est
        // compté plus bas)
        boolean turned = false;
        if (traveledDistance > 0) {
            double error = Steering.headingError(new Point2D.Double(playersLastPosition.xPos, playersLastPosition.yPos),
                                                 new Point2D.Double(currenPlayerPosition.xPos, currenPlayerPosition.yPos),
                                                 new Point2D.Double(destination.xPos, destination.yPos));
            logger.debug("Point {} : distance {}, parcouru {}, écart de cap {}°", pathIndex + 1, Math.round(remainingDistance),
                         Math.round(traveledDistance), Math.round(error));
            if (Steering.reached(remainingDistance, lastRemainingDistance, traveledDistance)) {
                // Point suivant : son cap sera mesuré au prochain tour
                logger.debug("Point {} atteint", pathIndex + 1);
                pathIndex++;
                turned = true;
            }
            else {
                int turnDuration = Steering.turnDuration(error);
                if (turnDuration > 0) {
                    if (error > 0) {turnRight(turnDuration);}
                    else {turnLeft(turnDuration);}
                    turned = true;
                }
            }
        }
        // Après un virage ou un changement de point, la mesure repart de zéro : la direction mesurée sur un tour qui
        // contient un virage mélange l'ancien et le nouveau cap, et ferait trop corriger
        lastRemainingDistance = turned ? null : remainingDistance;
        playersLastPosition = turned ? null : new ClkPosition(currenPlayerPosition);
    
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
        
        
        if (isFlying && !isRunning) {runStart();}
        
        logger.debug("Tourne à Gauche");
        
        peripherals.robot.keyPress(37);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(37);
        
        turnedRight = false;
        turnedLeft = true;
    }
    
    private void turnRight(int iTime) {
        
        
        if (isFlying && !isRunning) {runStart();}
        
        logger.debug("Tourne à Droite");
        
        peripherals.robot.keyPress(39);
        peripherals.robot.delay(iTime);
        peripherals.robot.keyRelease(39);
        
        turnedRight = true;
        turnedLeft = false;
    }
}
