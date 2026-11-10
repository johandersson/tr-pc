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
package au.com.trgtd.tr.view.collect;

import static org.junit.Assert.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.openide.util.Utilities;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class AddThoughtsActionTest {

    @Test
    public void testCaptureShortcuts() throws Exception {
        String action = "Actions/Collect/au-com-trgtd-tr-view-collect-AddThoughtsAction.instance";
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        try (var stream = AddThoughtsAction.class.getResourceAsStream("layer.xml")) {
            assertNotNull(stream);
            var document = factory.newDocumentBuilder().parse(stream);
            NodeList files = document.getElementsByTagName("file");
            boolean functionKey = false;
            boolean alternative = false;
            for (int i = 0; i < files.getLength(); i++) {
                Element file = (Element) files.item(i);
                Element folder = (Element) file.getParentNode();
                if (!"Shortcuts".equals(folder.getAttribute("name"))) {
                    continue;
                }
                Element attr = (Element) file.getElementsByTagName("attr").item(0);
                if (!action.equals(attr.getAttribute("stringvalue"))) {
                    continue;
                }
                String name = file.getAttribute("name");
                KeyStroke key = Utilities.stringToKey(name.substring(0, name.indexOf('.')));
                functionKey |= Utilities.stringToKey("F6").equals(key);
                int modifier = Utilities.isMac() ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;
                if (KeyStroke.getKeyStroke(KeyEvent.VK_N, modifier | InputEvent.SHIFT_DOWN_MASK).equals(key)) {
                    assertEquals("DS-N.shadow", name);
                    alternative = true;
                }
            }
            assertTrue("F6 must capture a thought", functionKey);
            assertTrue("Ctrl+Shift+N must capture a thought", alternative);
        }
    }

    @Test
    public void testIconResource() {
        AddThoughtsAction instance = new AddThoughtsAction();
        assertEquals("au/com/trgtd/tr/resource/icons/ThoughtAdd.png", instance.iconResource());
    }
}
