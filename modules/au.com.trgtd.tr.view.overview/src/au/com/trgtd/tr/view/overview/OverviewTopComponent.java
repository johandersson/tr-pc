/*
 * ThinkingRock, a project management tool for Personal Computers. 
 * Copyright (C) 2006 Avente Pty Ltd
 * Modifications Copyright (C) 2026 Johan Andersson
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
package au.com.trgtd.tr.view.overview;

import au.com.trgtd.tr.resource.Icons;
import au.com.trgtd.tr.resource.Resource;
import java.awt.BorderLayout;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.apache.batik.swing.JSVGCanvas;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;
import org.openide.windows.TopComponent;
import au.com.trgtd.tr.util.Observable;
import au.com.trgtd.tr.util.Observer;
import au.com.trgtd.tr.util.Utils;
import au.com.trgtd.tr.view.Window;
import au.com.trgtd.tr.view.overview.Overview.Screen;
import au.com.trgtd.tr.view.overview.spi.OverviewSVGProvider;
import au.com.trgtd.tr.view.overview.dashboard.DashboardPanel;
import au.com.trgtd.tr.view.overview.dashboard.QuickAddNextActionPanel;
import java.awt.Dimension;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;

/**
 * Top component which displays the overview screen.
 */
public final class OverviewTopComponent extends Window {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger("tr.view.overview.OverviewTopComponent");
    
    public static final String PREFERRED_ID = "OverviewTopComponent";
    
    private static OverviewTopComponent instance;

    private final Lookup.Result<OverviewSVGProvider> lookup
            = Lookup.getDefault().lookupResult(OverviewSVGProvider.class);
    private final Overview overview;    
    private final JSVGCanvas canvas;
    private final DashboardPanel dashboard;
    private final QuickAddNextActionPanel quickAdd;
    private final DashboardRefresh dashboardRefresh;
    
    private OverviewTopComponent() {
        initComponents();
        setName(NbBundle.getMessage(OverviewTopComponent.class, "CTL_OverviewTopComponent"));
        setToolTipText(NbBundle.getMessage(OverviewTopComponent.class, "HINT_OverviewTopComponent"));        
        setIcon(Icons.Overview.getImage());            
        overview = new Overview();
        canvas = overview.getSVGCanvas();
        dashboard = new DashboardPanel();
        dashboardRefresh = new DashboardRefresh(this::isShowing, dashboard::refresh);
        JScrollPane dashboardScroll = new JScrollPane(dashboard,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        dashboardScroll.setBorder(BorderFactory.createEmptyBorder());
        dashboardScroll.getVerticalScrollBar().setUnitIncrement(16);
        dashboardScroll.setMinimumSize(new Dimension(0, 0));
        quickAdd = new QuickAddNextActionPanel(dashboard::refresh);
        JSplitPane dashboardColumn = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                dashboardScroll, quickAdd);
        dashboardColumn.setResizeWeight(1);
        dashboardColumn.setContinuousLayout(true);
        dashboardColumn.setBorder(BorderFactory.createEmptyBorder());
        dashboardColumn.setMinimumSize(new Dimension(0, 0));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvas, dashboardColumn);
        split.setResizeWeight(0.6);
        split.setBorder(BorderFactory.createEmptyBorder());
        add(split, BorderLayout.CENTER);
//      canvas.setURI(Resource.getOverviewURL().toExternalForm());
        canvas.setURI(getURL().toExternalForm());
        canvas.setSize(1,1); // stops image being positioned at lower right
    }

    private URL getURL() {

        String language = Locale.getDefault().getLanguage();
        String country = Locale.getDefault().getCountry();

        LOG.log(Level.INFO, "Language: {0}", language);
        LOG.log(Level.INFO, "Country: {0}", country);

        OverviewSVGProvider generalLanguageProvider = null;

        for (OverviewSVGProvider provider : lookup.allInstances()) {

            LOG.log(Level.INFO, "Provider, language: {0}, country: {1}", new Object[]{provider.getLanguage(), provider.getCountry()});

            if (Utils.equal(provider.getLanguage(), language)) {
                if (Utils.equal(provider.getCountry(), country)) {
                    LOG.info("Provider found.");
                    return provider.getURL();
                }
                if (provider.getCountry() == null) {
                    generalLanguageProvider = provider;
                }
            }
        }

        if (generalLanguageProvider != null) {
            LOG.info("General language provider found.");
            return generalLanguageProvider.getURL();
        }

        LOG.info("Using default overview SVG.");
        return Resource.getOverviewURL();
    }

    @Override
    public void addNotify() {        
        super.addNotify();        
        addListeners();
    }
            
    @Override
    public void removeNotify() {
        removeListeners();
        super.removeNotify();
    }

    private Observer observer;    
    
    private void addListeners() {        
        observer = (Observable observable, final Object screen) -> {
            if (screen instanceof Overview.Screen scr) {
                SwingUtilities.invokeLater(() -> {
                    switchScreen(scr);
                });
            }
        };
        overview.addObserver(observer);        
        dashboardRefresh.start();
    }

    private void removeListeners() {        
        overview.removeObserver(observer);        
        dashboardRefresh.stop();
    }
    
    private void switchScreen(Screen screen) {        
        SystemAction.get(screen.actionType).actionPerformed(null);
    }

    /**
     * Gets the default instance. Do not use directly: reserved for *.settings
     * files only, i.e. deserialization routines; otherwise you could get a
     * non-deserialized instance.
     * To obtain the singleton instance, use {@link findInstance}.
     */
    public static synchronized OverviewTopComponent getDefault() {
        if (instance == null) {
            instance = new OverviewTopComponent();
        }
        return instance;
    }

    /**
     * Obtain the OverviewTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized OverviewTopComponent findInstance() {
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }

    @Override
    public void componentActivated() {
        refresh();
        SwingUtilities.invokeLater(() -> {
            if (TopComponent.getRegistry().getActivated() == this) {
                takeFocus();
            }
        });
    }

    @Override
    public void takeFocus() {
        quickAdd.focusEditor();
    }

    @Override
    public void componentOpened() {
        refresh();
    }

    @Override
    public void componentShowing() {
        refresh();
    }

    /* Refreshes the window. */
    public void refresh() {
        canvas.setSize(1,1); // stops image being positioned at lower right
        dashboardRefresh.refresh();
    }

    @Override
    public String preferredID() {
        return PREFERRED_ID;
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("tr.view.overview");
    }
    
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());

    }// </editor-fold>//GEN-END:initComponents
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    
}
