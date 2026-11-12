/*
 * Copyright (C) 2026 Johan Andersson
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package au.com.trgtd.tr.view.collect.dialog;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.FutureTask;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.junit.Test;
import tr.model.Data;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class ThoughtDialogTest {

    @Test
    public void saveAndCloseAddsThoughtAndClosesDialog() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            Data data = new Data();
            ThoughtDialog dialog = new ThoughtDialog(data);
            try {
                FutureTask<Void> checks = new FutureTask<>(() -> {
                    try {
                        assertEquals("Save and Close (Ctrl+Enter)", button(dialog).getText());
                        panel(dialog).setDescr("Captured thought");
                        saveAndClose(dialog);
                        assertEquals(1, data.getThoughtManager().size());
                        assertEquals("Captured thought",
                                data.getThoughtManager().list().get(0).getDescription());
                        assertFalse(dialog.isShowing());
                    } finally {
                        dialog.dispose();
                    }
                    return null;
                });
                SwingUtilities.invokeLater(checks);
                dialog.showCreateDialog();
                await(checks);
            } finally {
                dialog.dispose();
            }
        });
    }

    private static ThoughtDialogPanel panel(ThoughtDialog dialog) {
        for (Component component : dialog.getContentPane().getComponents()) {
            if (component instanceof ThoughtDialogPanel panel) {
                return panel;
            }
        }
        throw new AssertionError("Missing dialog panel");
    }

    private static JButton button(ThoughtDialog dialog) {
        for (Component component : dialog.getContentPane().getComponents()) {
            if (component instanceof JPanel panel) {
                for (Component child : panel.getComponents()) {
                    if (child instanceof JButton button && button.getText().startsWith("Save and Close")) {
                        return button;
                    }
                }
            }
        }
        throw new AssertionError("Missing Save and Close button");
    }

    private static void saveAndClose(ThoughtDialog dialog) {
        var root = dialog.getRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);
        Object key = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(stroke);
        assertNotNull("Ctrl+Enter must save and close throughout the dialog", key);
        Action action = root.getActionMap().get(key);
        assertNotNull(action);
        action.actionPerformed(new ActionEvent(root, ActionEvent.ACTION_PERFORMED, "save-and-close"));
    }

    private static void await(FutureTask<Void> checks) {
        try {
            checks.get();
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
