/*
 * Copyright (C) 2026 Johan Andersson
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package au.com.trgtd.tr.view.project.chooser;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.concurrent.FutureTask;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.junit.Test;
import tr.model.Data;
import tr.model.DataLookup;
import tr.model.project.Project;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class ProjectChooserDialogTest {

    @Test
    public void searchSelectsNestedProjectAndInitialDialogFitsScreenAndKeepsManualResize() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            Data original = DataLookup.instance().lookup(Data.class);
            JFrame owner = new JFrame();
            ProjectChooserDialog dialog = null;
            try {
                Data data = new Data();
                Project parent = new Project(data);
                parent.setDescription("Parent");
                Project target = new Project(data);
                target.setDescription("Nested search target");
                parent.add(target);
                data.getRootProjects().add(parent);
                DataLookup.instance().setData(data);
                owner.setSize(1000, 700);
                dialog = new ProjectChooserDialog(owner);
                ProjectChooserDialog chooser = dialog;
                FutureTask<Void> choose = new FutureTask<>(() -> {
                    try {
                        Rectangle bounds = new Rectangle(chooser.getGraphicsConfiguration().getBounds());
                        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(chooser.getGraphicsConfiguration());
                        bounds.x += insets.left;
                        bounds.y += insets.top;
                        bounds.width -= insets.left + insets.right;
                        bounds.height -= insets.top + insets.bottom;
                        assertTrue(bounds.contains(chooser.getBounds()));
                        assertEquals(Math.min(1000, bounds.width - 40), chooser.getWidth());
                        find(chooser, JTextField.class).setText("TARGET");
                        JTree tree = find(chooser, JTree.class);
                        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
                        DefaultMutableTreeNode child = (DefaultMutableTreeNode)
                                ((DefaultMutableTreeNode) root.getChildAt(0)).getChildAt(0);
                        tree.setSelectionPath(new TreePath(child.getPath()));
                        chooser.getRootPane().getDefaultButton().doClick();
                    } finally {
                        chooser.dispose();
                    }
                    return null;
                });
                SwingUtilities.invokeLater(choose);
                assertSame(target, chooser.select(null, owner));
                await(choose);

                Dimension resized = new Dimension(chooser.getWidth() - 10, chooser.getHeight() - 10);
                chooser.setSize(resized);
                FutureTask<Void> reopen = new FutureTask<>(() -> {
                    try {
                        assertEquals(resized, chooser.getSize());
                        assertEquals("", find(chooser, JTextField.class).getText());
                    } finally {
                        chooser.dispose();
                    }
                    return null;
                });
                SwingUtilities.invokeLater(reopen);
                chooser.select(target, owner);
                await(reopen);
            } finally {
                if (dialog != null) {
                    dialog.dispose();
                }
                owner.dispose();
                DataLookup.instance().setData(original);
            }
        });
    }

    private static void await(FutureTask<Void> checks) {
        try {
            checks.get();
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private static <T extends Component> T find(Container container, Class<T> type) {
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T found = find(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
