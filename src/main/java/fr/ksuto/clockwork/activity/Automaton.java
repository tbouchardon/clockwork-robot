package fr.ksuto.clockwork.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fr.ksuto.clockwork.ClockWorkUI;
import fr.ksuto.clockwork.brain.BrainService;
import fr.ksuto.clockwork.brain.decision.Brain;
import fr.ksuto.clockwork.brain.perception.GameState;
import fr.ksuto.clockwork.brain.perception.KeyCombo;
import fr.ksuto.clockwork.brain.perception.QrCodeV2Reader;
import fr.ksuto.clockwork.entities.qrcode.ComplexKey;
import fr.ksuto.clockwork.entities.qrcode.Key;
import fr.ksuto.clockwork.entities.qrcode.QrCode;
import fr.ksuto.prh.PeripheralRobotHelper;
import fr.ksuto.prh.capture.Frame;
import fr.ksuto.prh.capture.Rgb;
import fr.ksuto.prh.peripherals.Screen;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Optional;

public class Automaton {
    
    private static final Logger logger = LoggerFactory.getLogger(Automaton.class);
    
    private static final int                   GREY_COLOR            = 100;
    private static final long                  TURN_AROUND_COOL_DOWN = 4000;

    private static final int MODIFIER_CTRL = 1;
    private static final int MODIFIER_ALT = 2;
    private static final int MODIFIER_SHIFT = 4;

    private static final int STANCE_1 = 112;
    private static final int STANCE_2 = 113;
    private static final int STANCE_3 = 114;
    private static final int STANCE_4 = 115;
    private static final int STANCE_5 = 116;

    private final        PeripheralRobotHelper peripherals;
    private final        ClockWorkUI           ui;
    public Status status = Status.RUN;
    private              Robot                 robot;
    private              TomTom                tomtom;
    private final        BrainService          brain                 = new BrainService();
    private              boolean               wasInCombat           = false;
    private              long                  lastActionTime        = 0;
    private              long                  lockTurnAroundUntil   = System.currentTimeMillis();
    private              String                state                 = "";
    private              int                   lastGridFrame         = -1;
    private              long                  lastGridFrameChange   = 0;
    
    public Automaton(PeripheralRobotHelper peripherals, ClockWorkUI autoHitControl) {
        
        this.peripherals = peripherals;
        this.ui = autoHitControl;
        tomtom = new TomTom(peripherals);
    }
    
    public void play() throws Exception {
        
        logger.debug("");
        
        robot = new Robot();
        
        while (!ui.isShouldExit()) {
            
            QrCode qrCode = ui.getQrCode();
            
            robot.delay(100);

            if (status == Status.FISHING) {
                try {
                    fish();
                }
                catch (Exception e) {
                    // Une erreur de pêche ne doit pas arrêter tout l'automate
                    logger.error("Pêche interrompue", e);
                    status = Status.RUN;
                }
            }
            
            searchForSomethingToDo(qrCode);
        }
    }
    
    private void checkParty(Frame capturedQrCode, QrCode qrCode) {
        
        for (ComplexKey raidMember : qrCode.raid) {
            
            raidMember.updateActive(capturedQrCode);
            if (raidMember.getBlue(capturedQrCode) != 0) {
                if (raidMember.index < 5) {targetPartyMember(raidMember);}
                else {targetRaidMember(raidMember);}
                break;
            }
        }
    }
    
    private void fish() throws Exception {
        
        Fisherman natPagle = new Fisherman(ui, peripherals);
        natPagle.setup();
        boolean keepFishing = true;
        peripherals.getScreen().startCapture();
        while (keepFishing) {
            keepFishing = natPagle.fish(peripherals.getScreen().getCaptureScheduler());
        }
        peripherals.getScreen().stopCapture();
        natPagle.leave();
        if (status == Status.FISHING) {
            ui.dojButtonFishClick();
        }
    }
    
    private void hitKey(Key key2hit, boolean altModifier, boolean ctrlModifier, boolean shiftModifier, int duration) {
        
        String key = shiftModifier ? key2hit.key.toUpperCase() : key2hit.key.toLowerCase();
        
        ui.appendLog(key);
        ui.setiGrey(GREY_COLOR);
        
        peripherals.getKeyboard().pressKey(key2hit.hitKey, altModifier, ctrlModifier, shiftModifier, duration);
    }
    
    private void searchForSomethingToDo(QrCode qrCode) {
        
        qrCode.captureQrCode(peripherals);
        
        // On ne fait rien si l’addon n’est pas visible
        if (qrCode.getCapturedQrCode().rgb(0, 0) != Rgb.ARGB_GREEN) {
            reportState("En attente : QR code invisible (WoW masqué, interface cachée ou addon non chargé)");
            return;
        }
        
        qrCode.update();
        
        if (!qrCode.TOGGLE_ON_OFF.active) {
            reportState("En attente : addon désactivé (/clk toggle en jeu)");
            return;
        }
        
        reportState("Actif");
        
        if (qrCode.ADD_WAYPOINT.active) {
            tomtom.addWayPoint(qrCode);
        }
        
        if (qrCode.CLEAR_WAYPOINTS.active) {
            tomtom.clearWayPoints();
        }
        
        if (wasInCombat && !qrCode.inCombat.active && qrCode.DRIVE_MOD.active) {
            tryToLoot();
        }
        
        Key     key2hit              = null;
        int     bestPriority         = -1;
        int     bestPriorityDuration = 0;
        boolean shiftModifier        = false;
        boolean ctrlModifier         = false;
        boolean altModifier          = false;
        
        checkParty(qrCode.getCapturedQrCode(), qrCode);
        
        // Check Stance
        Frame capturedQrCode = qrCode.getCapturedQrCode();
        if (qrCode.stance.getBlue(capturedQrCode) != 0) {
            bestPriority = qrCode.stance.getGreen(capturedQrCode);
            int stance = qrCode.stance.getBlue(capturedQrCode);
            
            switch (stance) {
                case STANCE_1:
                    key2hit = new Key(KeyEvent.VK_F1, "F1");
                    break;
                case STANCE_2:
                    key2hit = new Key(KeyEvent.VK_F2, "F2");
                    break;
                case STANCE_3:
                    key2hit = new Key(KeyEvent.VK_F3, "F3");
                    break;
                case STANCE_4:
                    key2hit = new Key(KeyEvent.VK_F4, "F4");
                    break;
                case STANCE_5:
                    key2hit = new Key(KeyEvent.VK_F5, "F5");
                    break;
                default:
                    key2hit = new Key(0x0, "");
            }
        }
        
        if (key2hit != null) {logger.debug("key2hit = " + key2hit.key + ", bestPriority = " + bestPriority);}
        
        String keyStatus = "";
        for (Key key : qrCode.getKeys()) {
            int     keyMod   = key.getRed(capturedQrCode);
            int     priority = key.getGreen(capturedQrCode);
            int     duration = (int) (key.getBlue(capturedQrCode) / 255.0 * 30.0 * 1000.0);
            boolean active   = priority != 0;
            keyStatus += key.key + " : " + active + " | ";
            
            if (active && priority > bestPriority) {
                
                key2hit = key;
                ctrlModifier = keyMod == MODIFIER_CTRL;
                altModifier = keyMod == MODIFIER_ALT;
                shiftModifier = keyMod == MODIFIER_SHIFT;
                bestPriority = priority;
                bestPriorityDuration = duration;
                
                logger.debug("key2hit = " + key2hit.key + ", mod = " + keyMod + ", bestPriority = " + bestPriority);
            }
        }
        logger.debug(keyStatus);
        
        // Cerveau Java : avec une rotation YAML pour ce personnage et une grille v2 ou v3, il choisit la touche à la place
        // de l'addon
        if (brain.hasRotations()) {
            Optional<GameState> state = QrCodeV2Reader.read(qrCode.getCapturedQrCode());
            if (state.isPresent() && brain.handles(state.get())) {
                Optional<Brain.Decision> decision = gridFrozen(state.get()) ? Optional.empty() : brain.decide(state.get());
                KeyCombo combo = decision.map(d -> KeyCombo.parse(d.key())).orElse(null);
                key2hit = combo == null ? null : keyNamed(qrCode, combo.key()).orElse(null);
                altModifier = combo != null && combo.alt();
                ctrlModifier = combo != null && combo.ctrl();
                shiftModifier = combo != null && combo.shift();
                bestPriorityDuration = 0;
                decision.ifPresent(d -> logger.debug("Cerveau : touche {} ({}, priorité {})", d.key(), d.reason(), d.priority()));
            }
            else if (state.isEmpty()) {
                reportState("Cerveau inactif : grille v1, l'addon décide seul (addon à mettre à jour)");
            }
        }
        
        if (key2hit != null) {logger.debug("key2hit = " + key2hit.key + ", bestPriority = " + bestPriority + ", bestPriorityDuration = " + bestPriorityDuration);}
        
        if (key2hit != null) {
            lastActionTime = System.currentTimeMillis();
            hitKey(key2hit, altModifier, ctrlModifier, shiftModifier, bestPriorityDuration);
        }
        
        if (qrCode.TARGET_NEAREST_ENEMY.active && key2hit == null && !qrCode.casting.active) {
            peripherals.getKeyboard().pressKey(KeyEvent.VK_TAB);
        }
        
        tomtom.drive(qrCode, peripherals, key2hit != null, qrCode.casting.active, qrCode.inCombat.active, lastActionTime, qrCode.getPlayerHealth());
        
        if (qrCode.turnAround.active && System.currentTimeMillis() > lockTurnAroundUntil && qrCode.DRIVE_MOD.active) {
            lockTurnAroundUntil = System.currentTimeMillis() + TURN_AROUND_COOL_DOWN;
            turnAround();
        }
        
        wasInCombat = qrCode.inCombat.active;
        
        if (key2hit == null) {peripherals.robot.delay(200);}
        else {peripherals.robot.delay(750);}
        
        logger.debug("Key2hit is null");
    }
    
    /**
     * Journalise l'état de l'automate quand il change, pour savoir pourquoi il ne fait rien.
     */
    private void reportState(String newState) {
        
        if (newState.equals(state)) {return;}
        state = newState;
        logger.info(newState);
    }
    
    /**
     * La grille v3 porte un compteur que l'addon incrémente à chaque mise à jour : s'il ne bouge plus depuis
     * 1,5 s, WoW est figé (écran de chargement, fenêtre en arrière-plan...) et l'état lu est périmé.
     */
    private boolean gridFrozen(GameState gameState) {
        
        if (gameState.frame() == 0) {return false;} // grille v2 : pas de compteur
        
        long now = System.currentTimeMillis();
        if (gameState.frame() != lastGridFrame) {
            lastGridFrame = gameState.frame();
            lastGridFrameChange = now;
            return false;
        }
        if (now - lastGridFrameChange < 1500) {return false;}
        
        reportState("En attente : grille figée (l'addon ne se met plus à jour)");
        return true;
    }
    
    private static Optional<Key> keyNamed(QrCode qrCode, String name) {
        
        return qrCode.getKeys().stream().filter(key -> key.key.equals(name)).findFirst();
    }
    
    private void targetPartyMember(ComplexKey raidMember) {
        
        logger.debug("Target party member " + raidMember.index);
        
        switch (raidMember.index) {
            
            case 1:
                peripherals.getKeyboard().pressKey(KeyEvent.VK_F2, false, false, true);
                break;
            case 2:
                peripherals.getKeyboard().pressKey(KeyEvent.VK_F3, false, false, true);
                break;
            case 3:
                peripherals.getKeyboard().pressKey(KeyEvent.VK_F4, false, false, true);
                break;
            case 4:
                peripherals.getKeyboard().pressKey(KeyEvent.VK_F5, false, false, true);
                break;
            default:
                logger.debug("ERROR : Unexpected raidMember.index value: " + raidMember.index);
        }
    }
    
    private void targetRaidMember(ComplexKey raidMember) {
        
        logger.debug("Target raid member " + raidMember.index);
        
        peripherals.getKeyboard().pressKey(raidMember.hitKey, raidMember.alt, raidMember.ctrl, raidMember.shift);
    }
    
    private void tryToLoot() {
        
        peripherals.robot.delay(500);
        
        int hitZoneX = Screen.SCREEN_WIDTH / 2 + (int) (Screen.SCREEN_WIDTH / 100d * 4.6875);
        int hitZoneY = Screen.SCREEN_HEIGHT / 2 + (int) (Screen.SCREEN_HEIGHT / 100d * 14.8148);
        
        peripherals.robot.keyPress(KeyEvent.VK_SHIFT);
        peripherals.getMouse().clickRight(hitZoneX, hitZoneY);
        peripherals.getMouse().clickRight(hitZoneX, hitZoneY - 100);
        peripherals.getMouse().clickRight(hitZoneX, hitZoneY + 100);
        peripherals.getMouse().clickRight(hitZoneX - 100, hitZoneY);
        peripherals.getMouse().clickRight(hitZoneX + 100, hitZoneY);
        peripherals.robot.keyRelease(KeyEvent.VK_SHIFT);
    }
    
    private void turnAround() {
        
        robot.mouseMove(Screen.X_START, Screen.Y_START);
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        
        for (int iLR = Screen.X_START; iLR < Screen.X_START + 800; iLR += 10) {
            robot.mouseMove(iLR, Screen.Y_START);
            robot.delay(2);
        }
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
    }

    public enum Status {
        RUN, PAUSE, FISHING, TOMTOM //STOP, CONFIG,
    }
}
