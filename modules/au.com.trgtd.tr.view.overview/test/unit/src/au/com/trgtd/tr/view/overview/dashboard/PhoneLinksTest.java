/*
 * Copyright (C) 2026 Johan Andersson
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package au.com.trgtd.tr.view.overview.dashboard;

import java.net.URI;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class PhoneLinksTest {

    @Test
    public void findsFormattedNumbersInTitleAndNotesAndDeduplicates() {
        var phones = PhoneLinks.find("Call +46 (70) 123-45-67", "Office: (020) 7946 0958\nAlso +46701234567");
        assertEquals(List.of(URI.create("tel:+46701234567"), URI.create("tel:02079460958")),
                phones.stream().map(PhoneLinks.Phone::uri).toList());
        assertEquals("+46 (70) 123-45-67", phones.getFirst().label());
    }

    @Test
    public void rejectsDatesShortNumbersLongNumbersAndEmbeddedIdentifiers() {
        assertTrue(PhoneLinks.find(null, "", "2026-11-10", "10/11/2026",
                "12345", "+1234567890123456", "ABC123456789XYZ").isEmpty());
    }

    @Test
    public void extractsMultipleNumbersAndProducesOnlySafeTelephoneUris() {
        var phones = PhoneLinks.find("<script>Call +1-212-555-0100</script>; backup: 555.0123");
        assertEquals(List.of("tel:+12125550100", "tel:5550123"),
                phones.stream().map(phone -> phone.uri().toString()).toList());
    }
}
