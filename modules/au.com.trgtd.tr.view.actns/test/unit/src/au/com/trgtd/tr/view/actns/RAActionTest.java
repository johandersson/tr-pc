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
package au.com.trgtd.tr.view.actns;

import au.com.trgtd.tr.view.actns.screens.ActionsScreen;
import java.awt.GraphicsEnvironment;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openide.util.NbBundle;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assert.*;

public class RAActionTest {

    @Before
    public void requiresDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless());
    }

    @Test
    public void selectsDoAsapByKeyInsteadOfTabPosition() {
        ActionsScreen today = ActionsScreen.create("screen-today");
        ActionsScreen doAsap = ActionsScreen.create(ActionsScreen.DO_ASAP_KEY);
        ActionsScreen done = ActionsScreen.create("screen-done");

        assertSame(doAsap, RAAction.selectScreen(List.of(today, done, doAsap), ActionsScreen.DO_ASAP_KEY));
        assertSame(doAsap, RAAction.selectScreen(List.of(doAsap, today, done), ActionsScreen.DO_ASAP_KEY));
    }

    @Test
    public void normalReviewActionsStillSelectsFirstTab() {
        ActionsScreen first = ActionsScreen.create("custom");
        ActionsScreen doAsap = ActionsScreen.create(ActionsScreen.DO_ASAP_KEY);

        assertSame(first, RAAction.selectScreen(List.of(first, doAsap), null));
    }

    @Test
    public void selectsDoAsapAfterPreferencesSavedItsDisplayNameAsKey() {
        ActionsScreen today = ActionsScreen.create("screen-today");
        ActionsScreen doAsap = ActionsScreen.create(NbBundle.getMessage(ActionsScreen.class, ActionsScreen.DO_ASAP_KEY));

        assertSame(doAsap, RAAction.selectScreen(List.of(today, doAsap), ActionsScreen.DO_ASAP_KEY));
    }

    @Test
    public void missingRequestedTabFallsBackToFirstConfiguredTab() {
        ActionsScreen first = ActionsScreen.create("custom");

        assertSame(first, RAAction.selectScreen(List.of(first), ActionsScreen.DO_ASAP_KEY));
        assertNull(RAAction.selectScreen(List.of(), ActionsScreen.DO_ASAP_KEY));
        assertNull(RAAction.selectScreen(List.of(), null));
    }
}
