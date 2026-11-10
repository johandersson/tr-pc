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
package au.com.trgtd.tr.view.actns.screens.columns;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ActionsColumnTest {

    @Test
    public void collapsedSavedWidthFallsBackToPreferredWidth() {
        ActionsColumn column = ActionsColumn.createColumn(ActionsColumn.INDEX_DESCR);
        column.setWidth(1);

        assertEquals(column.getPreferredWidth(), column.getWidth());
    }

    @Test
    public void usableSavedWidthIsKept() {
        ActionsColumn column = ActionsColumn.createColumn(ActionsColumn.INDEX_DESCR);
        column.setWidth(308);

        assertEquals(308, column.getWidth());
    }

    @Test
    public void fixedWidthColumnUsesPreferredWidth() {
        ActionsColumn column = ActionsColumn.createColumn(ActionsColumn.INDEX_DONE);
        column.setWidth(1);

        assertEquals(17, column.getWidth());
    }
}
