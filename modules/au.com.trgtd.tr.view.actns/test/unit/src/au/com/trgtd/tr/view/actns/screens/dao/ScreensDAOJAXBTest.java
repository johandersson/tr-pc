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
package au.com.trgtd.tr.view.actns.screens.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import au.com.thinkingrock.xsd.tr.view.actions.screens.Screens;
import au.com.trgtd.tr.view.actns.screens.ActionsScreen;
import au.com.trgtd.tr.view.actns.screens.ActionsScreens;
import au.com.trgtd.tr.view.actns.screens.columns.ActionsColumn;
import org.junit.Test;

public class ScreensDAOJAXBTest {

    @Test
    public void visibleColumnsOfNeverShownScreenSurviveSaveAndRestore() {
        ActionsScreen screen = ActionsScreen.create("Do ASAP");
        screen.getColumns().get(ActionsColumn.INDEX_DONE).setVisible(true);
        screen.getColumns().get(ActionsColumn.INDEX_DUE_DATE).setVisible(true);
        ActionsScreens screens = new ActionsScreens();
        screens.getScreens().add(screen);

        ScreensDAOJAXB dao = (ScreensDAOJAXB) ScreensDAOJAXB.getInstance();
        Screens xml = dao.toXMLScreens(screens);
        ActionsScreen restored = dao.toActionsScreens(xml).getScreens().list().get(0);

        assertTrue(restored.getColumns().get(ActionsColumn.INDEX_DESCR).isVisible());
        assertTrue(restored.getColumns().get(ActionsColumn.INDEX_DONE).isVisible());
        assertTrue(restored.getColumns().get(ActionsColumn.INDEX_DUE_DATE).isVisible());
        assertFalse(restored.getColumns().get(ActionsColumn.INDEX_TOPIC).isVisible());
    }
}
