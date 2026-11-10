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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package au.com.trgtd.tr.view.overview;

import au.com.trgtd.tr.util.Observer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import tr.model.Data;
import tr.model.DataLookup;

/** Event-driven refresh; lifecycle methods run on the EDT. */
final class DashboardRefresh {

    private final Lookup.Result<Data> dataResult = DataLookup.instance().lookupResult(Data.class);
    private final AtomicBoolean pending = new AtomicBoolean();
    private final Observer observer = (observable, argument) -> requestRefresh();
    private final LookupListener dataListener = event -> requestRefresh();
    private final BooleanSupplier showing;
    private final Runnable render;
    private final Timer timer;
    private Data observedData;
    private boolean started;

    DashboardRefresh(BooleanSupplier showing, Runnable render) {
        this.showing = showing;
        this.render = render;
        timer = new Timer(150, event -> refresh());
        timer.setRepeats(false);
    }

    void start() {
        if (!started) {
            started = true;
            dataResult.addLookupListener(dataListener);
            refresh();
        }
    }

    void stop() {
        started = false;
        timer.stop();
        dataResult.removeLookupListener(dataListener);
        observe(null);
    }

    void refresh() {
        timer.stop();
        if (started) {
            observe(DataLookup.instance().lookup(Data.class));
            if (showing.getAsBoolean()) {
                render.run();
            }
        }
    }

    private void observe(Data data) {
        if (observedData == data) {
            return;
        }
        if (observedData != null) {
            observedData.getRootAll().removeObserver(observer);
            observedData.getThoughtManager().removeObserver(observer);
        }
        observedData = data;
        if (observedData != null) {
            observedData.getRootAll().addObserver(observer);
            observedData.getThoughtManager().addObserver(observer);
        }
    }

    private void requestRefresh() {
        if (pending.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(() -> {
                pending.set(false);
                if (started) {
                    observe(DataLookup.instance().lookup(Data.class));
                    // Do not restart the timer: continuous edits must not starve refreshes.
                    if (showing.getAsBoolean() && !timer.isRunning()) {
                        timer.start();
                    }
                }
            });
        }
    }
}
