package fr.ksuto.clockwork.activity;

import java.awt.event.KeyEvent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroupTargetingTest {

    @Test
    void shortcutsMatchTheAddon() {

        // group.lua : 1..20 sur Alt+Maj+A..T, 21..40 sur Alt+Ctrl+A..T, retour sur Alt+Maj+U
        assertEquals(new GroupTargeting.Shortcut(KeyEvent.VK_A, true, false, true), GroupTargeting.member(1));
        assertEquals(new GroupTargeting.Shortcut(KeyEvent.VK_T, true, false, true), GroupTargeting.member(20));
        assertEquals(new GroupTargeting.Shortcut(KeyEvent.VK_A, true, true, false), GroupTargeting.member(21));
        assertEquals(new GroupTargeting.Shortcut(KeyEvent.VK_T, true, true, false), GroupTargeting.member(40));
        assertEquals(new GroupTargeting.Shortcut(KeyEvent.VK_U, true, false, true), GroupTargeting.LAST_TARGET);
        assertThrows(IllegalArgumentException.class, () -> GroupTargeting.member(41));
    }
}
