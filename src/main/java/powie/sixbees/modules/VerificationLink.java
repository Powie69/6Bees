/*
 * Copyright (C) 2026 Powie69
 * This file is part of 6Bees.
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Please provide attribution when using or redistributing this code.
 * Derivative works must remain under the same license.
 */

package powie.sixbees.modules;

import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.network.DisconnectionDetails;
import powie.sixbees.SixBees;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VerificationLink extends Module {
    private final Pattern VERIFICATION_LINK_PATTERN = Pattern.compile("https://verify\\.6b6t\\.org/[^\\s`)\\]]+");

    public VerificationLink() {
        super(SixBees.CATEGORY, "verification-link", "Shows a button to quickly open anti-bot verification link");
    }

    public String getVerificationUrl(DisconnectionDetails details) {
        if (!details.reason().getString().startsWith("6b6t Anti Bot Protection")) return "";
        Matcher matcher = VERIFICATION_LINK_PATTERN.matcher(details.reason().getString());
        return matcher.find() ? matcher.group() : "";
    }
}
