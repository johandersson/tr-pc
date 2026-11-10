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
package au.com.trgtd.tr.datastore.xstream;

import au.com.trgtd.tr.datastore.DataStore;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import org.junit.Test;
import tr.model.Data;
import tr.model.action.Action;
import tr.model.context.Context;
import static org.junit.Assert.*;

public class XStreamDataStoreTest {

    @Test
    public void serializedExistingDataGetsMissingContextsAndTheySurviveRoundTrip() {
        DataStore store = XStreamDataStore.instance();
        Data original = store.getData();
        try {
            XStream serializer = new XStream(new StaxDriver());
            serializer.allowTypesByWildcard(new String[]{"tr.model.**"});
            Data legacy = new Data();
            for (Context context : legacy.getContextManager().list()) {
                if (context != Context.getDefault()) {
                    legacy.getContextManager().remove(context);
                }
            }
            Context calls = new Context(legacy.getNextID());
            calls.setName("@cALLs");
            calls.setDescription("Preserve my context");
            legacy.getContextManager().add(calls);
            Action call = new Action(legacy);
            call.setContext(calls);
            call.setDescription("Call supplier +46 70 123 45 67");
            legacy.getRootActions().add(call);
            store.setData((Data) serializer.fromXML(serializer.toXML(legacy)));
            Data migrated = store.getData();
            assertEquals(11, migrated.getContextManager().size());
            assertTrue(migrated.hasChanged());
            Context preserved = migrated.getContextManager().list().stream()
                    .filter(context -> context.hasName("Calls")).findFirst().orElseThrow();
            assertEquals("@cALLs", preserved.getName());
            assertEquals("Preserve my context", preserved.getDescription());
            assertSame(preserved, migrated.getRootActions().getChildren(Action.class).getFirst().getContext());

            store.setData((Data) serializer.fromXML(serializer.toXML(migrated)));
            assertEquals(11, store.getData().getContextManager().size());
            assertFalse("Persisted defaults must not be added again", store.getData().hasChanged());
        } finally {
            store.setData(original);
        }
    }

    @Test
    public void loadWithBlankPathDoesNotTryToReadCurrentDirectory() throws Exception {
        String originalPath = XStreamPrefs.getPath();
        try {
            XStreamPrefs.setPath(null);
            XStreamDataStore.instance().load();
        } finally {
            XStreamPrefs.setPath(originalPath);
        }
    }
}
