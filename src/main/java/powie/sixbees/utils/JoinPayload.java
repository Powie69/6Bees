/*
 * Copyright (C) 2026 Powie69
 * This file is part of 6Bees.
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Please provide attribution when using or redistributing this code.
 * Derivative works must remain under the same license.
 */

package powie.sixbees.utils;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class JoinPayload implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("anarchymod", "join");

    public static final StreamCodec<FriendlyByteBuf, JoinPayload> CODEC =
        StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> new JoinPayload()
        );

    public static final Type<JoinPayload> TYPE = new Type<>(ID);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
