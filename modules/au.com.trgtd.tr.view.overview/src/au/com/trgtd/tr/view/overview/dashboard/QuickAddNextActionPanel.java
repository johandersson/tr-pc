/*
 * Copyright (C) 2026 Johan Andersson
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package au.com.trgtd.tr.view.overview.dashboard;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import org.openide.util.NbBundle;
import tr.model.Data;
import tr.model.DataLookup;
import tr.model.action.Action;
import tr.model.action.ActionStateASAP;
import tr.model.context.Context;

/** Embedded capture for single Do ASAP actions, independent of dashboard refreshes. */
public final class QuickAddNextActionPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Pattern CONTEXT_MARKER = Pattern.compile("(?<!\\S)@");
    private final JTextArea editor = new JTextArea(4, 0);
    private final JTextArea status = new JTextArea(3, 0);
    private final Runnable afterSave;

    public QuickAddNextActionPanel(Runnable afterSave) {
        super(new BorderLayout(0, 6));
        this.afterSave = afterSave;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, javax.swing.UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(10, 12, 12, 12)));
        setMinimumSize(new Dimension(0, 180));

        JLabel title = new JLabel(msg("quickadd.title"));
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        title.setLabelFor(editor);
        add(title, BorderLayout.NORTH);
        editor.setLineWrap(true);
        editor.setWrapStyleWord(true);
        editor.setToolTipText(msg("quickadd.hint"));
        editor.getAccessibleContext().setAccessibleName(msg("quickadd.title"));
        add(new JScrollPane(editor), BorderLayout.CENTER);

        status.setEditable(false);
        status.setFocusable(false);
        status.setOpaque(false);
        status.setLineWrap(true);
        status.setWrapStyleWord(true);
        status.setFont(title.getFont().deriveFont(Font.PLAIN));
        status.setText(msg("quickadd.hint"));
        JButton save = new JButton(msg("quickadd.save"));
        save.addActionListener(e -> save());
        JPanel footer = new JPanel(new BorderLayout(0, 4));
        footer.add(status, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.add(save);
        footer.add(buttons, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "quick-add");
        getActionMap().put("quick-add", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                save();
            }
        });
    }

    private void save() {
        Data data = DataLookup.instance().lookup(Data.class);
        if (data == null) {
            showMessage(msg("quickadd.no.data"));
            return;
        }
        String text = editor.getText().strip();
        Context selected = Context.getDefault();
        List<Context> contexts = data.getContextManager().list();
        StringBuilder description = new StringBuilder();
        Matcher markers = CONTEXT_MARKER.matcher(text);
        int copiedUntil = 0;
        boolean hasContext = false;
        while (markers.find()) {
            if (markers.start() < copiedUntil) {
                continue;
            }
            Context matched = null;
            int matchedEnd = -1;
            for (Context context : contexts) {
                String name = context.getName().strip();
                if (name.startsWith("@")) {
                    name = name.substring(1).strip();
                }
                int start = markers.end();
                int end = start + name.length();
                if (!name.isEmpty() && text.regionMatches(true, start, name, 0, name.length())
                        && (end == text.length() || (end < text.length() && Character.isWhitespace(text.charAt(end))))
                        && end > matchedEnd) {
                    matched = context;
                    matchedEnd = end;
                }
            }
            if (matched == null) {
                int end = markers.end();
                while (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
                    end++;
                }
                showMessage(msg("quickadd.unknown.context", text.substring(markers.start(), end)));
                return;
            }
            if (hasContext && !selected.equals(matched)) {
                showMessage(msg("quickadd.multiple.contexts"));
                return;
            }
            selected = matched;
            hasContext = true;
            description.append(text, copiedUntil, markers.start());
            copiedUntil = matchedEnd;
        }
        description.append(text, copiedUntil, text.length());
        String title = description.toString().strip().replaceAll("\\s+", " ");
        if (title.isEmpty()) {
            showMessage(msg("quickadd.empty"));
            return;
        }
        Action action = new Action(data);
        action.setDescription(title);
        action.setContext(selected);
        action.setState(new ActionStateASAP());
        if (!data.getRootActions().add(action)) {
            showMessage(msg("quickadd.failed"));
            return;
        }
        editor.setText("");
        showMessage(msg("quickadd.saved"));
        afterSave.run();
    }

    private void showMessage(String message) {
        status.setText(message);
        editor.requestFocusInWindow();
    }

    private static String msg(String key, Object... args) {
        return NbBundle.getMessage(QuickAddNextActionPanel.class, key, args);
    }
}
