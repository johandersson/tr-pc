/*
 * Copyright (C) 2026 Johan Andersson
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package au.com.trgtd.tr.view.actns.screens;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor;

/**
 * Forwards matcher changes from a delegate editor, but can coalesce a batch of
 * changes into a single change so a filter list is re-evaluated only once.
 */
final class BatchMatcherEditor<E> extends AbstractMatcherEditor<E> implements MatcherEditor.Listener<E> {

    private final MatcherEditor<E> delegate;
    private int batchDepth;
    private boolean pending;

    BatchMatcherEditor(MatcherEditor<E> delegate) {
        this.delegate = delegate;
        fireChanged(delegate.getMatcher());
        delegate.addMatcherEditorListener(this);
    }

    void runBatch(Runnable changes) {
        batchDepth++;
        try {
            changes.run();
        } finally {
            if (--batchDepth == 0 && pending) {
                pending = false;
                fireChanged(delegate.getMatcher());
            }
        }
    }

    @Override
    public void changedMatcher(MatcherEditor.Event<E> event) {
        if (batchDepth > 0) {
            pending = true;
            return;
        }
        switch (event.getType()) {
            case MatcherEditor.Event.MATCH_ALL -> fireMatchAll();
            case MatcherEditor.Event.MATCH_NONE -> fireMatchNone();
            case MatcherEditor.Event.CONSTRAINED -> fireConstrained(event.getMatcher());
            case MatcherEditor.Event.RELAXED -> fireRelaxed(event.getMatcher());
            default -> fireChanged(event.getMatcher());
        }
    }
}
