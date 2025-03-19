package fr.ksuto.clockwork;

import com.google.inject.Guice;
import com.google.inject.Inject;

public class Runner {
    
    @Inject
    ClockWorkUI ui;
    
    public static void main(String[] args) throws Exception {
        
        Runner runner = Guice.createInjector().getInstance(Runner.class);
        
        runner.ui.initUI();
        runner.ui.initAutomaton();
        runner.ui.castLogTextfield.requestFocus();
        while (runner.ui.getQrCode().getKeys().isEmpty()) {
            
            runner.ui.peripherals.robot.delay(200);
        }
        runner.ui.automaton.play();
        System.exit(0);
    }
}
