/*
 * Copyright (C) 2026 Johan Andersson
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package au.com.trgtd.tr.view.overview.dashboard;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PhoneLinks {

    private static final Pattern NUMBER = Pattern.compile(
            "(?<![\\p{L}\\d+])\\+?(?:\\(\\d{1,4}\\)|\\d)[\\d ()/.-]*\\d(?![\\p{L}\\d])");
    private static final Pattern DATE = Pattern.compile(
            "(?:\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{4})");

    record Phone(String label, URI uri) {}

    static List<Phone> find(String... texts) {
        List<Phone> phones = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String text : texts) {
            if (text == null) {
                continue;
            }
            Matcher matcher = NUMBER.matcher(text);
            while (matcher.find()) {
                String label = matcher.group().strip();
                String digits = label.replaceAll("[^0-9]", "");
                if (digits.length() < 7 || digits.length() > 15 || DATE.matcher(label).matches()) {
                    continue;
                }
                String number = (label.startsWith("+") ? "+" : "") + digits;
                if (seen.add(number)) {
                    phones.add(new Phone(label, URI.create("tel:" + number)));
                }
            }
        }
        return phones;
    }

    private PhoneLinks() {}
}
