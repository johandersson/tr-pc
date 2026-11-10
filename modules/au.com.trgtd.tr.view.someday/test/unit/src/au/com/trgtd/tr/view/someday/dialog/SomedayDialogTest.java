/*
 * Copyright (C) 2026 Johan Andersson
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package au.com.trgtd.tr.view.someday.dialog;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.FutureTask;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.junit.Test;
import tr.model.Data;
import tr.model.future.Future;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class SomedayDialogTest {

    @Test
    public void controlSSavesRepeatedNewItemsAndRejectsEmptyDescription() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            Data data = new Data();
            SomedayDialog dialog = new SomedayDialog(data);
            try {
                FutureTask<Void> checks = new FutureTask<>(() -> {
                    try {
                        assertEquals("Add (Ctrl+S)", dialog.getRootPane().getDefaultButton().getText());
                        save(dialog);
                        assertEquals(0, data.getFutureManager().size());
                        for (int i = 1; i <= 2; i++) {
                            panel(dialog).setDescr("Someday " + i);
                            panel(dialog).setNotes("Notes " + i);
                            save(dialog);
                            assertEquals(i, data.getFutureManager().size());
                            assertEquals("Someday " + i, data.getFutureManager().list().get(i - 1).getDescription());
                            assertEquals("Notes " + i, data.getFutureManager().list().get(i - 1).getNotes());
                            assertTrue(dialog.isShowing());
                            assertEquals("", panel(dialog).getDescr());
                        }
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

    @Test
    public void controlSSavesEditsAndClosesDialog() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            Data data = new Data();
            Future future = new Future(data.getNextID());
            future.setDescription("Original");
            data.getFutureManager().add(future);
            SomedayDialog dialog = new SomedayDialog(data);
            try {
                FutureTask<Void> checks = new FutureTask<>(() -> {
                    try {
                        assertEquals("OK (Ctrl+S)", dialog.getRootPane().getDefaultButton().getText());
                        panel(dialog).setDescr("Edited");
                        save(dialog);
                        assertFalse(dialog.isShowing());
                    } finally {
                        dialog.dispose();
                    }
                    return null;
                });
                SwingUtilities.invokeLater(checks);
                dialog.showModifyDialog(future);
                await(checks);
                assertEquals("Edited", future.getDescription());
                assertEquals(1, data.getFutureManager().size());
            } finally {
                dialog.dispose();
            }
        });
    }

    private static void save(SomedayDialog dialog) {
        JRootPane root = dialog.getRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
        Object key = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(stroke);
        assertNotNull("Ctrl+S must work throughout the dialog", key);
        root.getActionMap().get(key).actionPerformed(new ActionEvent(root, ActionEvent.ACTION_PERFORMED, "save"));
    }

    private static void await(FutureTask<Void> checks) {
        try {
            checks.get();
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private static SomedayDialogPanel panel(SomedayDialog dialog) {
        for (Component component : dialog.getContentPane().getComponents()) {
            if (component instanceof SomedayDialogPanel panel) {
                return panel;
            }
        }
        throw new AssertionError("Missing dialog panel");
    }
}
