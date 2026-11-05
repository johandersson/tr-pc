/*
 * ThinkingRock, a project management tool for Personal Computers.
 * Copyright (C) 2006 Avente Pty Ltd
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
package au.com.trgtd.tr.calendar.ical4j.impl;

import au.com.trgtd.tr.calendar.ical4j.impl.ICalendarImporter.ImportResult;
import au.com.trgtd.tr.calendar.ical4j.impl.prefs.Options;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.fortuna.ical4j.data.ParserException;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.WindowManager;
import tr.model.Data;
import tr.model.DataLookup;

public final class ImportICalendarAction extends CallableSystemAction {

    private static final Logger LOG = Logger.getLogger(ImportICalendarAction.class.getName());
    private final Lookup.Result<Data> dataLookup;

    public ImportICalendarAction() {
        dataLookup = DataLookup.instance().lookupResult(Data.class);
        dataLookup.addLookupListener((LookupEvent event) -> updateEnablement());
        dataLookup.allInstances();
        updateEnablement();
    }

    private void updateEnablement() {
        setEnabled(DataLookup.instance().lookup(Data.class) != null);
    }

    @Override
    public void performAction() {
        Data data = DataLookup.instance().lookup(Data.class);
        if (data == null) {
            return;
        }

        JFileChooser chooser = new JFileChooser(Options.getImportPath());
        chooser.setDialogTitle(message("ImportICalendar.dialog.title"));
        chooser.setFileFilter(new FileNameExtensionFilter(
                message("ImportICalendar.file.description"), "ics", "ifb"));
        if (chooser.showOpenDialog(WindowManager.getDefault().getMainWindow())
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        try {
            ImportResult result = ICalendarImporter.importFile(data, file);
            File parent = file.getParentFile();
            if (parent != null) {
                Options.setImportPath(parent.getAbsolutePath());
            }
            showResult(result);
        } catch (IOException | ParserException ex) {
            LOG.log(Level.WARNING, "Could not import iCalendar file " + file, ex);
            JOptionPane.showMessageDialog(
                    WindowManager.getDefault().getMainWindow(),
                    MessageFormat.format(message("ImportICalendar.error.message"), ex.getMessage()),
                    message("ImportICalendar.result.title"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showResult(ImportResult result) {
        String text = MessageFormat.format(
                message("ImportICalendar.result.message"),
                result.imported(), result.duplicates(), result.skipped());
        JOptionPane.showMessageDialog(
                WindowManager.getDefault().getMainWindow(),
                text,
                message("ImportICalendar.result.title"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private static String message(String key) {
        return NbBundle.getMessage(ImportICalendarAction.class, key);
    }

    @Override
    public String getName() {
        return message("CTL_ImportICalendarAction");
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
}
