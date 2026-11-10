/*
 * ThinkingRock, a project management tool for Personal Computers.
 * Copyright (C) 2026 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package au.com.trgtd.tr.view.overview.dashboard;

import au.com.trgtd.tr.util.DateUtils;
import au.com.trgtd.tr.view.actns.RAAction;
import au.com.trgtd.tr.view.actns.screens.ActionsScreen;
import au.com.trgtd.tr.view.collect.AddThoughtsAction;
import au.com.trgtd.tr.view.overview.dashboard.DashboardModel.ProjectInfo;
import au.com.trgtd.tr.view.process.ProcessThoughtsAction;
import au.com.trgtd.tr.view.projects.ProjectsAction;
import au.com.trgtd.tr.view.projects.ProjectsTreeTopComponent;
import au.com.trgtd.tr.view.projects.actions.SingleActionsTopComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;
import org.openide.util.actions.SystemAction;
import tr.model.Data;
import tr.model.DataLookup;
import tr.model.action.Action;
import tr.model.action.ActionStateScheduled;
import tr.model.project.Project;

/**
 * GTD dashboard shown next to the overview diagram.
 */
public final class DashboardPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    static final int UPCOMING_DAYS = 2;
    private static final int COLLAPSED_ROWS = 8;

    private final Set<String> expanded = new HashSet<>();
    private final DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
    private final DateFormat dayFormat = new SimpleDateFormat("EEE d MMM");
    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    public DashboardPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    }

    /** Rebuilds the dashboard from the current data. Must be called on the EDT. */
    public void refresh() {
        Data data = DataLookup.instance().lookup(Data.class);
        DashboardModel model = DashboardModel.build(data, new Date(), UPCOMING_DAYS);
        removeAll();
        if (data == null) {
            add(text(msg("dashboard.no.data")));
        } else {
            render(model);
        }
        add(Box.createVerticalGlue());
        revalidate();
        repaint();
    }

    private void render(DashboardModel model) {
        JLabel title = new JLabel(msg("dashboard.title"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 4f));
        title.setAlignmentX(LEFT_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(6));

        JPanel summary = row();
        summary.add(link(msg("dashboard.inbox", model.inboxCount), () -> perform(ProcessThoughtsAction.class)));
        summary.add(text("   "));
        summary.add(link(msg("dashboard.doasap", model.doAsapCount), () -> {
            RAAction action = SystemAction.get(RAAction.class);
            if (action.isEnabled()) {
                action.performAction(ActionsScreen.DO_ASAP_KEY);
            }
        }));
        summary.add(text("   "));
        summary.add(link(msg("dashboard.capture"), () -> perform(AddThoughtsAction.class)));
        summary.add(text("   "));
        summary.add(link(msg("dashboard.refresh"), this::refresh));
        add(summary);

        section("today", msg("dashboard.today"), model.today,
                a -> time(a, false) + escape(a.getDescription()) + projectSuffix(a), this::openAction,
                msg("dashboard.today.none"));
        section("upcoming", msg("dashboard.upcoming", DashboardPanel.UPCOMING_DAYS), model.upcoming,
                a -> time(a, true) + escape(a.getDescription()) + projectSuffix(a), this::openAction,
                msg("dashboard.upcoming.none"));
        section("due", msg("dashboard.due"), model.due,
                a -> "<b>" + dateFormat.format(a.getDueDate()) + "</b> " + escape(a.getDescription()) + projectSuffix(a),
                this::openAction, null);
        section("followups", msg("dashboard.followups"), model.followUps,
                a -> escape(a.getDescription()) + projectSuffix(a), this::openAction, null);
        section("stalled", msg("dashboard.stalled"), model.stalledProjects,
                p -> indent(p) + escape(p.project().getDescription()), p -> openProject(p.project()),
                msg("dashboard.stalled.none"));
        section("projects", msg("dashboard.projects"), model.activeProjects,
                p -> indent(p) + escape(p.project().getDescription()) + " <font color='gray'>("
                        + msg("dashboard.actions", p.activeActions()) + ")</font>",
                p -> openProject(p.project()), msg("dashboard.projects.none"));
    }

    private <T> void section(String key, String heading, List<T> items, Function<T, String> html,
            java.util.function.Consumer<T> onClick, String emptyText) {
        if (items.isEmpty() && emptyText == null) {
            return;
        }
        add(Box.createVerticalStrut(14));
        JLabel header = new JLabel(heading + (items.isEmpty() ? "" : " (" + items.size() + ")"));
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, separatorColor()),
                BorderFactory.createEmptyBorder(0, 0, 3, 0)));
        add(header);
        add(Box.createVerticalStrut(3));
        if (items.isEmpty()) {
            add(text("<html><i>" + escape(emptyText) + "</i></html>"));
            return;
        }
        boolean showAll = expanded.contains(key) || items.size() <= COLLAPSED_ROWS + 1;
        int shown = showAll ? items.size() : COLLAPSED_ROWS;
        for (int i = 0; i < shown; i++) {
            T item = items.get(i);
            add(link("<html>" + html.apply(item) + "</html>", () -> onClick.accept(item)));
        }
        if (items.size() > COLLAPSED_ROWS + 1) {
            String label = showAll ? msg("dashboard.show.less") : msg("dashboard.show.all", items.size());
            add(link("<html><i>" + label + "</i></html>", () -> {
                if (!expanded.remove(key)) {
                    expanded.add(key);
                }
                refresh();
            }));
        }
    }

    private String time(Action action, boolean withDay) {
        Date date = ((ActionStateScheduled) action.getState()).getDate();
        StringBuilder sb = new StringBuilder("<b>");
        if (withDay) {
            sb.append(dayFormat.format(date)).append(' ');
        }
        if (DateUtils.hasTime(date)) {
            sb.append(timeFormat.format(date));
        }
        return sb.append("</b> ").toString();
    }

    private static String indent(ProjectInfo info) {
        return "&nbsp;".repeat(Math.max(0, info.depth()) * 4);
    }

    private static String projectSuffix(Action action) {
        Project project = DashboardModel.getProject(action);
        return project == null ? "" : " <font color='gray'>- " + escape(project.getDescription()) + "</font>";
    }

    private void openProject(Project project) {
        perform(ProjectsAction.class);
        EventQueue.invokeLater(() -> {
            ProjectsTreeTopComponent tc = ProjectsTreeTopComponent.findInstance();
            if (tc != null) {
                tc.select(project);
            }
        });
    }

    private void openAction(Action action) {
        perform(ProjectsAction.class);
        EventQueue.invokeLater(() -> {
            if (action.isSingleAction()) {
                SingleActionsTopComponent tc = SingleActionsTopComponent.findInstance();
                if (tc != null) {
                    tc.select(action);
                }
            } else {
                ProjectsTreeTopComponent tc = ProjectsTreeTopComponent.findInstance();
                if (tc != null) {
                    tc.select(action);
                }
            }
        });
    }

    private static void perform(Class<? extends CallableSystemAction> type) {
        CallableSystemAction action = SystemAction.get(type);
        if (action != null && action.isEnabled()) {
            action.performAction();
        }
    }

    private static JPanel row() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }

    private static JLabel text(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel link(String text, Runnable onClick) {
        JLabel label = text(text);
        label.setForeground(linkColor());
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onClick.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                underline((Component) e.getSource(), true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                underline((Component) e.getSource(), false);
            }
        });
        return label;
    }

    @SuppressWarnings("unchecked")
    private static void underline(Component c, boolean on) {
        Font font = c.getFont();
        java.util.Map<java.awt.font.TextAttribute, Object> attrs
                = (java.util.Map<java.awt.font.TextAttribute, Object>) font.getAttributes();
        attrs.put(java.awt.font.TextAttribute.UNDERLINE, on ? java.awt.font.TextAttribute.UNDERLINE_ON : -1);
        c.setFont(font.deriveFont(attrs));
    }

    private static Color linkColor() {
        Color c = UIManager.getColor("Component.linkColor");
        if (c == null) {
            c = UIManager.getColor("nb.html.link.foreground");
        }
        return c != null ? c : new Color(0x1a, 0x5f, 0xb4);
    }

    private static Color separatorColor() {
        Color c = UIManager.getColor("Separator.foreground");
        return c != null ? c : Color.LIGHT_GRAY;
    }

    static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '"' -> sb.append("&quot;");
                default -> sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String msg(String key, Object... args) {
        return NbBundle.getMessage(DashboardPanel.class, key, args);
    }
}
