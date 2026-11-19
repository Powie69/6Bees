/*
 * Copyright (C) 2026 Powie69
 * This file is part of 6Bees.
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Please provide attribution when using or redistributing this code.
 * Derivative works must remain under the same license.
 */

package powie.sixbees.events;

import meteordevelopment.meteorclient.events.Cancellable;

// TODO: Maybe add separate fields for command and arguments
public class SendCommandEvent extends Cancellable {
    private static final SendCommandEvent INSTANCE = new SendCommandEvent();

    public String command;

    public static SendCommandEvent get(String command) {
        INSTANCE.setCancelled(false);
        INSTANCE.command = command;
        return INSTANCE;
    }
}
