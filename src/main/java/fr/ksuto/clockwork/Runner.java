package fr.ksuto.clockwork;

import fr.ksuto.prh.tools.Debug;

import java.awt.*;
import java.io.IOException;

import com.google.inject.Guice;
import com.google.inject.Inject;

public class Runner {
    
    @Inject
    ClockWork_UI ui;
    
    public static void main(String[] args) throws AWTException, IOException {
        
        Runner runner = Guice.createInjector().getInstance(Runner.class);
        
        Debug.sout("new UI");
        runner.ui.initUI();
        runner.ui.initAutomaton();
        runner.ui.jTextFieldCast.requestFocus();
        Debug.sout("new Automaton");
        while (runner.ui.getQrCode().getKeys().isEmpty()) {runner.ui.peripherals.robot.delay(200);}
        runner.ui.automaton.play();
        System.exit(0);
    }
}
