/*
 * Copyright (C) 2026 Johan Andersson
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package au.com.trgtd.tr.view.project.chooser;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
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

public class ProjectsPanelTest {

    @Test
    public void searchFindsCollapsedDescendantsAndPreservesAncestorsAndMatchingSubtrees() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Data original = DataLookup.instance().lookup(Data.class);
            try {
                Data data = new Data();
                Project parent = project(data, "Parent");
                Project match = project(data, "Needle");
                Project child = project(data, "Child");
                match.add(child);
                parent.add(match);
                parent.add(project(data, "Unrelated sibling"));
                data.getRootProjects().add(parent);
                data.getRootProjects().add(project(data, "Unrelated root"));
                Project done = project(data, "Needle done");
                done.setDone(true);
                data.getRootProjects().add(done);
                DataLookup.instance().setData(data);
                ProjectsPanel panel = new ProjectsPanel();
                panel.refreshModel();
                JTree tree = components(panel, JTree.class).getFirst();
                JTextField search = components(panel, JTextField.class).getFirst();
                panel.setSelected(parent);
                tree.collapsePath(tree.getSelectionPath());

                search.setText("  nEeDlE  ");

                DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
                assertEquals(1, root.getChildCount());
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) root.getChildAt(0);
                assertSame(parent, parentNode.getUserObject());
                assertEquals(1, parentNode.getChildCount());
                DefaultMutableTreeNode matchNode = (DefaultMutableTreeNode) parentNode.getChildAt(0);
                assertSame(match, matchNode.getUserObject());
                assertSame(child, ((DefaultMutableTreeNode) matchNode.getChildAt(0)).getUserObject());
                assertTrue(tree.isExpanded(new TreePath(parentNode.getPath())));
                tree.setSelectionPath(new TreePath(matchNode.getPath()));
                assertSame(match, panel.getSelected());

                search.setText("");
                assertEquals(2, root.getChildCount());
                assertSame(parent, panel.getSelected());
                assertFalse("Clearing search restores collapsed branches", tree.isExpanded(tree.getSelectionPath()));
            } finally {
                DataLookup.instance().setData(original);
            }
        });
    }

    @Test
    public void noMatchesClearsSelectionAndFutureSearchIsIndependentAndRefreshResetsQueries() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Data original = DataLookup.instance().lookup(Data.class);
            try {
                Data data = new Data();
                Project current = project(data, "Current");
                Project future = project(data, "Future needle");
                data.getRootProjects().add(current);
                data.getRootFutures().add(future);
                DataLookup.instance().setData(data);
                ProjectsPanel panel = new ProjectsPanel();
                panel.refreshModel();
                List<JTextField> fields = components(panel, JTextField.class);
                List<JTree> trees = components(panel, JTree.class);
                panel.setSelected(current);
                fields.getFirst().setText("no match");
                assertNull(panel.getSelected());
                assertEquals(0, trees.getFirst().getModel().getChildCount(trees.getFirst().getModel().getRoot()));
                fields.get(1).setText("needle");
                panel.setSelected(future);
                assertSame(future, panel.getSelected());
                assertEquals("no match", fields.getFirst().getText());

                panel.refreshModel();
                assertTrue(fields.stream().allMatch(f -> f.getText().isEmpty()));
                assertEquals(1, trees.getFirst().getModel().getChildCount(trees.getFirst().getModel().getRoot()));
            } finally {
                DataLookup.instance().setData(original);
            }
        });
    }

    @Test
    public void sortingSurvivesFilteringWithoutReorderingSourceProjects() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Data original = DataLookup.instance().lookup(Data.class);
            try {
                Data data = new Data();
                Project zebra = project(data, "Zebra");
                Project alpha = project(data, "Alpha");
                data.getRootProjects().add(zebra);
                data.getRootProjects().add(alpha);
                DataLookup.instance().setData(data);
                ProjectsPanel panel = new ProjectsPanel();
                panel.refreshModel();
                panel.setSelected(null);
                components(panel, JButton.class).get(2).doClick();
                JTextField search = components(panel, JTextField.class).getFirst();
                search.setText("a");
                search.setText("");
                DefaultMutableTreeNode root = (DefaultMutableTreeNode)
                        components(panel, JTree.class).getFirst().getModel().getRoot();
                assertSame(alpha, ((DefaultMutableTreeNode) root.getChildAt(0)).getUserObject());
                assertEquals(List.of(zebra, alpha), data.getRootProjects().getChildren(Project.class));
            } finally {
                DataLookup.instance().setData(original);
            }
        });
    }

    @Test
    public void currentOnlyModeWorksWithoutDataAndLongTitlesInfluencePreferredWidth() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Data original = DataLookup.instance().lookup(Data.class);
            try {
                DataLookup.instance().setData(null);
                ProjectsPanel panel = new ProjectsPanel(true);
                panel.refreshModel();
                assertEquals(1, components(panel, JTextField.class).size());
                assertNull(panel.getSelected());
                Data data = new Data();
                data.getRootProjects().add(project(data, "Long project title ".repeat(20)));
                DataLookup.instance().setData(data);
                panel.refreshModel();
                assertTrue(panel.getPreferredChooserWidth() > 600);
            } finally {
                DataLookup.instance().setData(original);
            }
        });
    }

    @Test
    public void initialSizeFitsSmallScreensAndUsesSpaceOnLargerScreens() {
        assertEquals(new Dimension(760, 560),
                ProjectChooserDialog.fitSize(new Dimension(2000, 900), new Rectangle(800, 600)));
        assertEquals(new Dimension(1200, 810),
                ProjectChooserDialog.fitSize(new Dimension(1200, 810), new Rectangle(1920, 1080)));
        assertEquals(new Dimension(600, 400),
                ProjectChooserDialog.fitSize(new Dimension(100, 100), new Rectangle(1920, 1080)));
    }

    private static Project project(Data data, String description) {
        Project project = new Project(data);
        project.setDescription(description);
        return project;
    }

    private static <T extends Component> List<T> components(Container container, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                result.add(type.cast(component));
            }
            if (component instanceof Container child) {
                result.addAll(components(child, type));
            }
        }
        return result;
    }
}
