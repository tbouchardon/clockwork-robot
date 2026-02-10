package fr.ksuto.clockwork;

import com.google.inject.Guice;
import com.google.inject.Inject;

import javax.swing.*;
import java.io.IOException;

public class Runner {
    
    @Inject
    ClockWorkUI ui;

    public static void main(String[] args) {
        
        Runner runner = Guice.createInjector().getInstance(Runner.class);

        // Initialize the UI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                runner.ui.initUI();
                runner.ui.initAutomaton();
                runner.ui.castLogTextfield.requestFocus();
            } catch (IOException e) {
                // Or handle it more gracefully
                throw new RuntimeException(e);
            }
        });
        // The main thread will now exit, but the application will keep running
        // because of the Swing EDT. The automaton will be started by the UI.
    }
}
