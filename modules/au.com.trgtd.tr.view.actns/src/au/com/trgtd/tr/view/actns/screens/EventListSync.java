/*
 * Copyright (C) 2026 Johan Andersson
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package au.com.trgtd.tr.view.actns.screens;

import ca.odell.glazedlists.EventList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Brings an event list in line with the wanted elements by publishing only the
 * removals and additions, instead of clearing and re-adding every element.
 */
final class EventListSync {

    private EventListSync() {
    }

    /**
     * Makes the list contain the wanted elements (by identity), keeping existing
     * elements in place. Callers must hold the list's write lock.
     */
    static <E> void sync(EventList<E> list, List<? extends E> wanted) {
        Set<Object> wantedSet = identitySet();
        wantedSet.addAll(wanted);
        Set<Object> present = identitySet();
        List<E> removed = new ArrayList<>();
        for (E element : list) {
            if (!wantedSet.contains(element) || !present.add(element)) {
                removed.add(element);
            }
        }
        List<E> added = new ArrayList<>();
        for (E element : wanted) {
            if (present.add(element)) {
                added.add(element);
            }
        }
        if (!removed.isEmpty()) {
            list.removeAll(removed);
        }
        if (!added.isEmpty()) {
            list.addAll(added);
        }
    }

    private static Set<Object> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
