package fr.ksuto.clockwork.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fr.ksuto.clockwork.ClockWorkUI;
import fr.ksuto.clockwork.brain.BrainService;
import fr.ksuto.clockwork.brain.data.SpellDatabase;
import fr.ksuto.clockwork.brain.decision.Brain;
import fr.ksuto.clockwork.brain.decision.Rotation;
import fr.ksuto.clockwork.brain.perception.GameState;
import fr.ksuto.clockwork.brain.perception.KeyCombo;
import fr.ksuto.clockwork.brain.perception.QrCodeV2Reader;
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
import java.util.Set;

public class Automaton {
    
    private static final Logger logger = LoggerFactory.getLogger(Automaton.class);
    
    private static final int                   GREY_COLOR            = 100;
    private static final long                  TURN_AROUND_COOL_DOWN = 4000;

    private static final int MODIFIER_CTRL = 1;
    private static final int MODIFIER_ALT = 2;
    private static final int MODIFIER_SHIFT = 4;


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
    private final        HitDetector           hitDetector           = new HitDetector();
    private              int                   lastGridFrame         = -1;
    private              boolean               fishRequested         = false;
    private              long                  invisibleSince        = 0;
    private              long                  lastGridFrameChange   = 0;
    private              long                  lastTab               = 0;
    
    public Automaton(PeripheralRobotHelper peripherals, ClockWorkUI autoHitControl) {
        
        this.peripherals = peripherals;
        this.ui = autoHitControl;
        tomtom = new TomTom(peripherals);
        brain.spellDatabase(); // table des sorts chargée en arrière-plan dès le démarrage
    }
    
    public void play() throws Exception {
        
        logger.debug("");
        
        robot = new Robot();
        
        while (!ui.isShouldExit()) {
            
            QrCode qrCode = ui.getQrCode();
            
            robot.delay(100);

            if (status == Status.FISHING) {
                try {
                    fish(qrCode);
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
    
    /**
     * Pêche tant que l'addon la demande et que la souris ne bouge pas. Le sort Pêche est cherché sur les touches
     * décrites par la grille (raccourci et modificateur), à défaut sur H.
     */
    private void fish(QrCode qrCode) throws Exception {
        
        Fisherman natPagle = new Fisherman(ui, peripherals, () -> castKeyFor(qrCode, "Pêche"), () -> fishingPreparation(qrCode),
                                           () -> QrCodeV2Reader.read(qrCode.captureQrCode(peripherals)).map(GameState::casting).orElse(false),
                                           () -> QrCodeV2Reader.fishingResult(qrCode.captureQrCode(peripherals)));
        natPagle.setup();
        boolean keepFishing = true;
        while (keepFishing) {
            keepFishing = natPagle.fish();
            qrCode.captureQrCode(peripherals);
            qrCode.update();
            if (!qrCode.FISH_MOD.active) {
                logger.info("Pêche arrêtée par l'addon");
                keepFishing = false;
            }
        }
        natPagle.leave();
        status = Status.RUN;
    }

    /**
     * Touche portant ce sort d'après la grille (sorts de chaque touche) et la table des sorts du jeu.
     */
    private Optional<Fisherman.CastKey> castKeyFor(QrCode qrCode, String spell) {

        Optional<GameState>     state    = QrCodeV2Reader.read(qrCode.captureQrCode(peripherals));
        Optional<SpellDatabase> database = brain.spellDatabase();
        if (state.isEmpty() || database.isEmpty()) {return Optional.empty();}
        Set<Integer> ids = database.get().idsFor(spell);
        return state.get().keys().values().stream()
                    .filter(key -> ids.contains(key.spellId()))
                    .findFirst()
                    .flatMap(key -> castKey(qrCode, key.key()));
    }

    /**
     * Objet à utiliser avant un lancer (leurre, appât...) selon rotations/peche.yaml, s'il y a lieu.
     */
    private Optional<Fisherman.CastKey> fishingPreparation(QrCode qrCode) {

        return QrCodeV2Reader.read(qrCode.captureQrCode(peripherals))
                             .flatMap(brain::prepareFishing)
                             .flatMap(decision -> {
                                 logger.info("Pêche : {}", decision.reason());
                                 return castKey(qrCode, decision.key());
                             });
    }

    /**
     * Touche physique d'une combinaison de la grille ("SHIFT-R"...).
     */
    private static Optional<Fisherman.CastKey> castKey(QrCode qrCode, String combination) {

        KeyCombo combo = KeyCombo.parse(combination);
        return keyNamed(qrCode, combo.key()).map(physical -> new Fisherman.CastKey(physical.hitKey, combo.alt(), combo.ctrl(), combo.shift(),
                                                                                   combination));
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
            // Invisible depuis 5 s : la fenêtre a peut-être bougé, on recherche le QR code sur tout l'écran
            long now = System.currentTimeMillis();
            if (invisibleSince == 0) {invisibleSince = now;}
            else if (now - invisibleSince > 5000) {
                ui.requestQrCodeSearch();
                invisibleSince = now;
            }
            return;
        }
        invisibleSince = 0;
        
        qrCode.update();
        
        // Pêche demandée depuis WoW (/clk fish, menu de l'addon) : elle démarre quand la case s'allume
        if (qrCode.FISH_MOD.active && !fishRequested && status == Status.RUN) {
            logger.info("Pêche demandée par l'addon");
            status = Status.FISHING;
        }
        fishRequested = qrCode.FISH_MOD.active;
        
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
        Rotation.StopCasting stopFirst = Rotation.StopCasting.NONE;
        int     member               = 0;
        boolean tabbed               = false;
        boolean returnToTarget       = false;
        
        Frame capturedQrCode = qrCode.getCapturedQrCode();
        
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
                // Cible suivante (répartition des DoT) : Tab, rien d'autre à ce tour
                if (decision.isPresent() && decision.get().key().equals(Brain.NEXT_TARGET)) {
                    logger.debug("Cerveau : cible suivante ({}, priorité {})", decision.get().reason(), decision.get().priority());
                    peripherals.getKeyboard().pressKey(KeyEvent.VK_TAB);
                    lastTab = System.currentTimeMillis();
                    tabbed = true;
                    decision = Optional.empty();
                }
                KeyCombo combo = decision.map(d -> KeyCombo.parse(d.key())).orElse(null);
                key2hit = combo == null ? null : keyNamed(qrCode, combo.key()).orElse(null);
                altModifier = combo != null && combo.alt();
                ctrlModifier = combo != null && combo.ctrl();
                shiftModifier = combo != null && combo.shift();
                bestPriorityDuration = 0;
                stopFirst = decision.map(Brain.Decision::stopFirst).orElse(Rotation.StopCasting.NONE);
                member = decision.map(Brain.Decision::member).orElse(0);
                returnToTarget = decision.map(Brain.Decision::returnToTarget).orElse(false);
                decision.ifPresent(d -> logger.debug("Cerveau : touche {} ({}, priorité {})", d.key(), d.reason(), d.priority()));
            }
            else if (state.isEmpty()) {
                reportState("Cerveau inactif : grille v1, l'addon décide seul (addon à mettre à jour)");
            }
        }
        
        if (key2hit != null) {logger.debug("key2hit = " + key2hit.key + ", bestPriority = " + bestPriority + ", bestPriorityDuration = " + bestPriorityDuration);}
        
        if (key2hit != null) {
            lastActionTime = System.currentTimeMillis();
            stopCasting(stopFirst);
            if (member > 0) {press(GroupTargeting.member(member));}
            hitKey(key2hit, altModifier, ctrlModifier, shiftModifier, bestPriorityDuration);
            if (member > 0 && returnToTarget) {press(GroupTargeting.LAST_TARGET);}
        }
        
        // Ciblage auto : pas plus d'un Tab par délai de lecture de la grille, sinon une cible valable serait sautée avant
        // d'avoir été vue
        if (qrCode.TARGET_NEAREST_ENEMY.active && key2hit == null && !tabbed && !qrCode.casting.active
            && System.currentTimeMillis() - lastTab >= Brain.NEXT_TARGET_DELAY) {
            peripherals.getKeyboard().pressKey(KeyEvent.VK_TAB);
            lastTab = System.currentTimeMillis();
        }
        
        tomtom.drive(qrCode, peripherals, key2hit != null, qrCode.casting.active, qrCode.inCombat.active, lastActionTime, qrCode.getPlayerHealth());
        
        // Frappé sans riposter (vie en baisse, aucune touche depuis 6 s) : un monstre non ciblé, souvent dans le dos
        long now = System.currentTimeMillis();
        if (hitDetector.hitWithoutRetaliating(now, qrCode.getPlayerHealth(), lastActionTime) && now > lockTurnAroundUntil && qrCode.DRIVE_MOD.active) {
            logger.info("Frappé sans riposter : demi-tour");
            lockTurnAroundUntil = now + TURN_AROUND_COOL_DOWN;
            hitDetector.reset();
            turnAround();
        }
        
        wasInCombat = qrCode.inCombat.active;
        
        if (key2hit == null) {peripherals.robot.delay(200);}
        else {peripherals.robot.delay(750);}
        
        logger.debug("Key2hit is null");
    }
    
    /**
     * Interrompt sa propre incantation avant un sort plus prioritaire (WoW refuserait le sort) : se déplacer la coupe.
     */
    private void stopCasting(Rotation.StopCasting mode) {

        int key = switch (mode) {
            case JUMP -> KeyEvent.VK_SPACE;
            case BACK -> KeyEvent.VK_DOWN; // recul, comme TomTom
            case NONE -> 0;
        };
        if (key == 0) {return;}
        logger.debug("Cerveau : incantation interrompue ({})", mode);
        peripherals.robot.keyPress(key);
        peripherals.robot.delay(60);
        peripherals.robot.keyRelease(key);
        peripherals.robot.delay(60);
    }

    /**
     * Raccourci de ciblage d'un membre du groupe (boutons sécurisés de l'addon).
     */
    private void press(GroupTargeting.Shortcut shortcut) {

        peripherals.getKeyboard().pressKey(shortcut.key(), shortcut.alt(), shortcut.ctrl(), shortcut.shift());
        peripherals.robot.delay(30);
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
