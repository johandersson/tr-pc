/*
 * Copyright (C) 2026 Johan Andersson
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package au.com.trgtd.tr.view;

import java.net.URL;
import java.util.List;
import org.junit.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MultiFileSystem;
import org.openide.filesystems.XMLFileSystem;
import static org.junit.Assert.*;

public class ViewMenuTest {

    @Test
    public void mergedViewMenuExposesOnlyLogAndToolbarControls() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        XMLFileSystem platform = new XMLFileSystem();
        platform.setXmlUrls(new URL[]{
            loader.getResource("org/netbeans/core/ui/resources/layer.xml"),
            loader.getResource("org/netbeans/core/windows/resources/layer.xml")
        });
        XMLFileSystem log = new XMLFileSystem();
        log.setXmlUrls(new URL[]{new URL(loader.getResource("org/netbeans/core/actions/LogAction.class"),
                "../../../../META-INF/generated-layer.xml")});
        XMLFileSystem application = new XMLFileSystem(loader.getResource("au/com/trgtd/tr/view/layer.xml"));
        MultiFileSystem merged = new MultiFileSystem(application, log, platform);
        FileObject view = merged.findResource("Menu/View");
        assertNotNull(view);
        assertEquals("Actions/View/org-netbeans-core-actions-LogAction.instance",
                view.getFileObject("org-netbeans-core-actions-LogAction.shadow").getAttribute("originalFile"));
        assertEquals("Actions/Window/org-netbeans-core-windows-actions-ToolbarsListAction.instance",
                view.getFileObject("org-netbeans-core-windows-actions-ToolbarsListAction.shadow")
                        .getAttribute("originalFile"));
        for (String hidden : List.of("ShowEditorOnlyAction.shadow", "ToggleFullScreenAction.shadow",
                "org-netbeans-core-multiview-EditorsAction.instance",
                "org-netbeans-core-multiview-SplitAction.instance")) {
            assertNotNull("The application must mask " + hidden,
                    application.findResource("Menu/View/" + hidden + "_hidden"));
            assertNull(hidden, view.getFileObject(hidden));
        }
        assertNotNull("Other IDE menus must stay hidden",
                application.findResource("Menu/BuildProject_hidden"));
    }
}
