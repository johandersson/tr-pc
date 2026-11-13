/*
 * Copyright (C) 2026 Johan Andersson
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package au.com.trgtd.tr.appl;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class InstallerTest {

    @Test
    public void linuxStartupMaximizesAndLaysOutVisibleContentWithoutManualResize() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        JFrame[] frame = new JFrame[1];
        CountDownLatch resized = new CountDownLatch(1);
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new JFrame();
                frame[0].add(new JLabel("Startup content"));
                frame[0].setSize(640, 400);
                frame[0].setVisible(true);
                frame[0].addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent event) {
                        if ((frame[0].getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                            resized.countDown();
                        }
                    }
                });
                Installer.showMaximized(frame[0]);
            });
            assertTrue("Maximization must produce a native resize", resized.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(Frame.MAXIMIZED_BOTH, frame[0].getExtendedState() & Frame.MAXIMIZED_BOTH);
                assertEquals(0, frame[0].getExtendedState() & Frame.ICONIFIED);
                assertTrue(frame[0].getWidth() > 640);
                assertTrue(frame[0].getHeight() > 400);
                assertTrue(frame[0].isValid());
                assertTrue(frame[0].getContentPane().getComponent(0).getWidth() > 0);
                assertTrue(frame[0].getContentPane().getComponent(0).getHeight() > 0);
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame[0] != null) {
                    frame[0].dispose();
                }
            });
        }
    }
}
