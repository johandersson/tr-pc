/*
 * Copyright (C) 2026 Johan Andersson
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package tr.model;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import tr.model.context.Context;
import static org.junit.Assert.*;

public class DefaultContextsTest {

    @Test
    public void newDataContainsTenStarterContextsAndNoneWithUniqueIds() {
        Data data = new Data();
        Set<String> names = new HashSet<>();
        Set<Integer> ids = new HashSet<>();
        for (Context context : data.getContextManager().list()) {
            names.add(context.getName());
            assertTrue(ids.add(context.getID()));
        }
        assertEquals(11, names.size());
        assertTrue(names.containsAll(Set.of("Calls", "Computer", "Office", "Home", "Errands",
                "Online", "Anywhere", "Agendas", "Read/Review", "Waiting For")));
    }

    @Test
    public void loadingExistingDataPreservesMatchingContextsAndAddsOnlyMissingNames() {
        Data original = DataLookup.instance().lookup(Data.class);
        try {
            Data data = new Data();
            for (Context context : data.getContextManager().list()) {
                if (context != Context.getDefault()) {
                    data.getContextManager().remove(context);
                }
            }
            Context existing = new Context(data.getNextID());
            existing.setName(" @cAlLs ");
            existing.setDescription("Keep this description");
            data.getContextManager().add(existing);
            data.setChanged(false);
            DataLookup.instance().setData(data);
            assertEquals(11, data.getContextManager().size());
            assertSame(existing, data.getContextManager().list().stream()
                    .filter(context -> context.hasName("Calls")).findFirst().orElseThrow());
            assertEquals("Keep this description", existing.getDescription());
            assertTrue(data.hasChanged());
            data.setChanged(false);
            DataLookup.instance().setData(data);
            assertEquals(11, data.getContextManager().size());
            assertFalse("Opening an already seeded file must not mark it changed", data.hasChanged());
        } finally {
            DataLookup.instance().setData(original);
        }
    }
}
