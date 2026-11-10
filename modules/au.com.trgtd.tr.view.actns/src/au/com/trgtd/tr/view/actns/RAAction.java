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
package au.com.trgtd.tr.view.actns;

import au.com.trgtd.tr.resource.Resource;
import au.com.trgtd.tr.appl.InitialAction;
import java.awt.EventQueue;
import java.util.List;
import java.util.logging.Logger;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import tr.model.Data;
import tr.model.DataLookup;
import au.com.trgtd.tr.prefs.ui.utils.WindowUtils;
import au.com.trgtd.tr.view.actns.screens.*;
import au.com.trgtd.tr.view.actns.screens.ActionsScreen;
import au.com.trgtd.tr.view.actns.screens.dao.ScreensDAOProvider;
import au.com.trgtd.tr.view.projects.EditorTopComponent;
import au.com.trgtd.tr.view.projects.ProjectsTreeLookup;
import au.com.trgtd.tr.view.projects.ProjectsTreeTopComponent;
import au.com.trgtd.tr.view.projects.actions.SingleActionsLookup;
import au.com.trgtd.tr.view.projects.actions.SingleActionsTopComponent;

/**
 * Action for Review Actions.
 *
 * @author Jeremy Moore
 */
//public class RAAction extends AbstractAction implements InitialAction {
public class RAAction extends CallableSystemAction implements InitialAction {

    private static final Logger LOG = Logger.getLogger("tr.view.actions");
    private static RAAction instance;
    private Lookup.Result<Data> result;

    /* Constructs a new instance. */
    public RAAction() {
//        super(NbBundle.getMessage(RAAction.class, "CTL_RAAction"));
        super();
//        putValue(SMALL_ICON, Resources.ICON_ACTIONS);
        enableDisable();
        result = DataLookup.instance().lookupResult(Data.class);
        result.addLookupListener((LookupEvent lookupEvent) -> {
            LOG.fine("DataLookup Data.class result changed.");
            enableDisable();
        });
        instance = this;
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(getClass(), "CTL_RAAction");
    }

    @Override
    protected String iconResource() {
        return Resource.Actions;
    }

    public static RAAction getInstance() {
        if (instance == null) {
            instance = new RAAction();
        }
        return instance;
    }

    private void enableDisable() {
        Data data = DataLookup.instance().lookup(Data.class);
        setEnabled(data != null);
    }

    /** Gets the initial action identifier. */
    @Override
    public String getID() {
        return "actions";
    }

    /**
     * Sets up windows for TR review actions.
     */
    @Override
    public void performAction() {
        performAction(null);
    }

    /**
     * Opens Review Actions and activates the requested screen, or the first
     * configured screen when no key is supplied.
     * @param screenKey the screen key, or null for the normal Review Actions entry
     */
    public void performAction(String screenKey) {
        EventQueue.invokeLater(() -> {
            Data data = DataLookup.instance().lookup(Data.class);
            if (data == null) {
                return;
            }
            WindowUtils.closeWindows();

            Mode mode = WindowManager.getDefault().findMode("ra-actions");
            if (mode == null) {
                LOG.severe("Mode ra-actions was not found.");
                return;
            }

            TopComponent selectedComponent = null;

            List<ActionsScreen> screens = ScreensDAOProvider.instance().provide().getData().getScreens().list();
            ActionsScreen selectedScreen = selectScreen(screens, screenKey);
            for (ActionsScreen s : screens) {
                TopComponent tc = ReviewActionsTopComponent.createInstance(s);
                mode.dockInto(tc);
                tc.open();
                if (s == selectedScreen) {
                    selectedComponent = tc;
                }
            }

            ProjectsTreeTopComponent tcProjects = RAProjectsTreeTopComponent.findInstance();
            ProjectsTreeLookup.register(tcProjects);

            SingleActionsTopComponent tcSingleActions = RASingleActionsTopComponent.findInstance();
            SingleActionsLookup.register(tcSingleActions);

            TopComponent tcEditor = EditorTopComponent.findInstance();

            mode = WindowManager.getDefault().findMode("ra-projects");
            if (mode == null) {
                LOG.severe("Mode ra-projects was not found.");
            } else {
                mode.dockInto(tcProjects);
                mode.dockInto(tcSingleActions);
            }
            mode = WindowManager.getDefault().findMode("ra-editor");
            if (mode == null) {
                LOG.severe("Mode ra-editor was not found.");
            } else {
                mode.dockInto(tcEditor);
            }

            tcProjects.open();
            tcSingleActions.open();
            tcEditor.open();

            if (selectedComponent != null) {
                selectedComponent.requestActive();
                selectedComponent.requestVisible();
            }
        });
    }

    static ActionsScreen selectScreen(List<ActionsScreen> screens, String screenKey) {
        if (screenKey != null) {
            String displayName = ActionsScreen.DO_ASAP_KEY.equals(screenKey)
                    ? NbBundle.getMessage(ActionsScreen.class, screenKey) : screenKey;
            for (ActionsScreen screen : screens) {
                if (screenKey.equals(screen.getKey()) || displayName.equals(screen.getKey())) {
                    return screen;
                }
            }
            LOG.warning("Requested Review Actions screen was not found: " + screenKey);
        }
        return screens.isEmpty() ? null : screens.get(0);
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("prefs.actions.screens");
    }
}
