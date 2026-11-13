/*
 * Copyright (C) 2026 Johan Andersson
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package au.com.trgtd.tr.view.actns.screens;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.CompositeMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class ActionsListPerformanceTest {

    private static final class TextFilter extends AbstractMatcherEditor<String> {
        void require(String text) {
            fireChanged(item -> item.contains(text));
        }
    }

    @Test
    public void syncPublishesOnlyTheAdditionsAndRemovals() {
        String a = "a", b = "b", c = "c", d = "d";
        EventList<String> list = new BasicEventList<>();
        list.addAll(Arrays.asList(a, b, c));
        AtomicInteger events = new AtomicInteger();
        int[] changes = new int[1];
        list.addListEventListener(event -> {
            events.incrementAndGet();
            while (event.next()) {
                changes[0]++;
            }
        });

        EventListSync.sync(list, Arrays.asList(a, c, d));

        assertEquals(Arrays.asList(a, c, d), list);
        assertEquals("one removal event and one insertion event", 2, events.get());
        assertEquals("only b removed and d added", 2, changes[0]);
    }

    @Test
    public void syncUsesIdentityAndDropsDuplicates() {
        String original = new String("x");
        String replacement = new String("x");
        EventList<String> list = new BasicEventList<>();
        list.addAll(Arrays.asList(original, original));

        EventListSync.sync(list, List.of(replacement));

        assertEquals(1, list.size());
        assertSame(replacement, list.get(0));
    }

    @Test
    public void batchReevaluatesFilterListOnceWithFinalMatcher() {
        TextFilter first = new TextFilter();
        TextFilter second = new TextFilter();
        EventList<MatcherEditor<String>> editors = new BasicEventList<>();
        editors.add(first);
        editors.add(second);
        BatchMatcherEditor<String> batch = new BatchMatcherEditor<>(new CompositeMatcherEditor<>(editors));
        EventList<String> source = new BasicEventList<>();
        source.addAll(Arrays.asList("ab", "a", "b"));
        FilterList<String> filtered = new FilterList<>(source, batch);
        AtomicInteger matcherChanges = new AtomicInteger();
        batch.addMatcherEditorListener(event -> matcherChanges.incrementAndGet());

        batch.runBatch(() -> {
            first.require("a");
            second.require("b");
        });

        assertEquals(1, matcherChanges.get());
        assertEquals(List.of("ab"), filtered);

        first.require("b");
        assertEquals("changes outside a batch are forwarded immediately", 2, matcherChanges.get());
        assertEquals(Arrays.asList("ab", "b"), filtered);
    }
}
