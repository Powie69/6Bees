/*
 * Copyright (C) 2026 Powie69
 * This file is part of 6Bees.
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Please provide attribution when using or redistributing this code.
 * Derivative works must remain under the same license.
 */

package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import powie.sixbees.SixBees;


/**
 * Yeah its a skid of voltrafly.
 * but im learning about its "asymptote growth" type shi, im not gonna blindly ctrl + c -> ctrl + v this.
 * ill give proper attribution once this is released
 *
 * @author Powie69
 */

/**
 * Reduced VolytraFly: only the acceleration ramp (horizontal + vertical) and Overdrive.
 * <p>
 * Removed: Autopilot, Autohighway, Autonav, Mapping, Building, Bypass, Player Avoidance,
 * Anti-Slam, no-crash, max-height, rubberband speed cap, unstick, chest swap / elytra replace,
 * insta-drop, HUD and the assistant.
 */
public class EFly extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgOverdrive = settings.createGroup("Overdrive");

    // ---------------------------------------------------------------- Settings

    public final Setting<Double> startSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("minimum-horizontal-speed")
        .description("The speed you start at when moving horizontally (before acceleration starts)")
        .min(0)
        .defaultValue(2.999)
        .build()
    );

    public final Setting<Double> horizontalSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("maximum-horizontal-speed")
        .description("The fastest horizontal speed will go (blocks per tick)")
        .defaultValue(14.999)
        .min(0)
        .build()
    );

    public final Setting<Double> accelerationPlateau = sgGeneral.add(new DoubleSetting.Builder()
        .name("horizontal-acceleration-plateau")
        .description("The horizontal speed where acceleration will tend to 0")
        .min(0.01)
        .defaultValue(14.999)
        .build()
    );

    public final Setting<Double> accelerationStep = sgGeneral.add(new DoubleSetting.Builder()
        .name("horizontal-acceleration-step")
        .description("How fast horizontal speed ramps up")
        .min(0.01)
        .max(5)
        .defaultValue(0.3)
        .build()
    );

    public final Setting<Double> verticalStartSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("minimum-vertical-speed")
        .description("The speed you start at when moving vertically, (before acceleration starts)")
        .min(0)
        .defaultValue(5.999)
        .build()
    );

    public final Setting<Double> verticalSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("maximum-vertical-speed")
        .description("The fastest vertical speed will go (blocks per tick)")
        .defaultValue(5.999)
        .min(0)
        .build()
    );

    public final Setting<Double> verticalAccelerationPlateau = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical-acceleration-plateau")
        .description("The vertical speed where acceleration will tend to 0")
        .min(0.01)
        .defaultValue(29.999)
        .build()
    );

    public final Setting<Double> verticalAccelerationStep = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical-acceleration-step")
        .description("How fast vertical speed ramps up")
        .min(0.01)
        .max(5)
        .defaultValue(1.0)
        .build()
    );

    public final Setting<Double> fallMultiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-multiplier")
        .description("Multiplier for how fast you fall naturally")
        .defaultValue(0)
        .min(0)
        .build()
    );

    public final Setting<Boolean> accelerateUpward = sgGeneral.add(new BoolSetting.Builder()
        .name("accelerate-upward")
        .description("Also accelerates upwards. Not recommended if vertical speed goes above 5.999")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> overdrive = sgOverdrive.add(new BoolSetting.Builder()
        .name("overdrive")
        .description("Applies dark magic to force near-instant acceleration to max speed")
        .defaultValue(false)
        .build()
    );

    // --------------------------------------------------------------- Constants

    // Ticks to hold the current speed before accelerating again. Applied on take-off,
    // after speed-costing hits and each time the module is switched back on
    private static final int ACCELERATION_DELAY_TICKS = 1;

    // After a rubberband, how many ticks' worth of acceleration to take off the speed
    private static final int RUBBERBAND_REWIND_TICKS = 15;

    // Overdrive runs this many flight ticks per game tick
    private static final double OVERDRIVE_MULTIPLIER = 50.0;
    // The ramp counts as topped out at this fraction of maximum-horizontal-speed (the plateau curve
    // only ever creeps towards the top speed, so waiting for an exact match would never finish)
    private static final double OVERDRIVE_OFF_FRACTION = 0.995;
    // Once Overdrive has used up its extra ticks it stays off until speed is back at or below this
    private static final double OVERDRIVE_REARM_SPEED = 2.999;

    // ------------------------------------------------------------------- State

    // Horizontal: despite the name this holds the CURRENT TARGET SPEED (blocks per tick), not an acceleration
    private double acceleration;
    private boolean atMaxSpeed;
    private int accelerationDelayTicks;

    // Vertical: same idea
    private double verticalAcceleration;
    private boolean atMaxVerticalSpeed;
    private int verticalAccelerationDelayTicks;

    // Per-tick movement being built up in onPlayerMove
    private double velX, velY, velZ;
    private Vec3 forward, right;

    // Where the player was and how fast this module told them to move on the previous glide tick,
    // used to tell whether a horizontal collision actually cost speed (see resetRampOnCollision)
    private Vec3 lastGlidePos;
    private double lastGlideIntendedSpeed;

    // Overdrive
    private double overdriveCarry;          // leftover fraction of an extra tick (so 2.5 averages out)
    private boolean overdriveSpent;         // ramp has topped out; cleared when speed drops back down
    private boolean overdriveExtraTick;     // true only while an Overdrive extra tick is running

    public EFly() {
        super(SixBees.CATEGORY, "EFly", "Elytra fly optimized for 6b6t");
    }

    // --------------------------------------------------------------- Lifecycle

    @Override
    public void onActivate() {
        Player player = mc.player;

        atMaxSpeed = false;
        accelerationDelayTicks = 0;
        lastGlidePos = null;
        lastGlideIntendedSpeed = 0;
        overdriveCarry = 0;
        overdriveSpent = false;
        overdriveExtraTick = false;

        // Carry over real horizontal speed instead of snapping back to the minimum, so turning the
        // module on while already moving picks the ramp up from wherever you actually were
        double currentHorizontalSpeed = player != null
            ? Math.hypot(player.getDeltaMovement().x(), player.getDeltaMovement().z())
            : 0;
        acceleration = Math.max(startSpeed.get(), Math.min(currentHorizontalSpeed, horizontalSpeed.get()));
        if (acceleration >= horizontalSpeed.get()) atMaxSpeed = true;

        resetVerticalAcceleration();
    }

    // ------------------------------------------------------------- Flight tick

    @SuppressWarnings("unused") // called by the event bus
    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER)) return;

        if (!mc.player.isFallFlying()) {
            lastGlidePos = null;
            lastGlideIntendedSpeed = 0;
            return;
        }
        // Vanilla horizontal momentum is discarded; speed is entirely what the ramp says it is
        velX = 0;
        velZ = 0;
        velY = event.movement.y();

        // The * 0.1 here is cancelled by the * 10 in handleHorizontalSpeed()
        forward = Vec3.directionFromRotation(0, mc.player.getYRot()).scale(0.1);
        right = Vec3.directionFromRotation(0, mc.player.getYRot() + 90).scale(0.1);

        handleFallMultiplier();

        resetRampOnCollision(mc.player);
        handleAcceleration();
        handleHorizontalSpeed();

        // Overdrive extra ticks are horizontal-only: the vertical ramp must not advance in them
        if (!overdriveExtraTick) handleVerticalAcceleration();
        handleVerticalSpeed();

        // No vertical movement at all in an Overdrive extra tick, so vertical speed per
        // game tick is exactly what it would be with Overdrive off
        if (overdriveExtraTick) velY = 0;

        ((IVec3) event.movement).meteor$set(velX, velY, velZ);

        lastGlidePos = mc.player.position();
        lastGlideIntendedSpeed = Math.hypot(velX, velZ);

    }

    @SuppressWarnings("unused") // called by the event bus
    @EventHandler
    private void onTick(TickEvent.Post event) {
        runOverdrive();
    }

    @SuppressWarnings("unused") // called by the event bus
    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof ClientboundPlayerPositionPacket)) return;

        if (!mc.player.isFallFlying()) {
            zeroAcceleration();
            return;
        }
        // The correction goes through, but only costs RUBBERBAND_REWIND_TICKS worth of ramp
        double keptSpeed = rewindSpeedFor(acceleration, accelerationStep.get(), accelerationPlateau.get(),
            startSpeed.get(), RUBBERBAND_REWIND_TICKS);

        if (keptSpeed < acceleration) {
            acceleration = keptSpeed;
            atMaxSpeed = false;
            accelerationDelayTicks = 0;

            verticalAcceleration = rewindSpeedFor(verticalAcceleration, verticalAccelerationStep.get(),
                verticalAccelerationPlateau.get(), verticalStartSpeed.get(), RUBBERBAND_REWIND_TICKS);
            atMaxVerticalSpeed = false;
            verticalAccelerationDelayTicks = 0;
        }
        return;

    }

    // ------------------------------------------------- Horizontal acceleration

    private void handleAcceleration() {
        if (noMovementKeyPressed()) {
            // No movement key held: back to the start of the ramp. Nothing is remembered between releases
            resetHorizontalAcceleration();
            return;
        }

        if (atMaxSpeed) return; // already at the top, nothing left to do

        if (accelerationDelayTicks < ACCELERATION_DELAY_TICKS) {
            // Still in the delay window: hold at the current speed, don't ramp yet
            accelerationDelayTicks++;
            return;
        }

        // Gain per tick is steep at low speed and tapers toward zero as speed approaches the plateau.
        // The real top speed is still maximum-horizontal-speed; the curve only shapes how you get there
        double cap = horizontalSpeed.get();
        acceleration = nextSpeedFor(acceleration, accelerationStep.get(), accelerationPlateau.get(), cap);

        if (acceleration >= horizontalSpeed.get()) atMaxSpeed = true;
    }

    /**
     * Turns the current ramp speed into this tick's horizontal velocity from the held keys.
     */
    private void handleHorizontalSpeed() {
        boolean a = false;
        boolean b = false;

        if (mc.options.keyUp.isDown()) {
            velX += forward.x() * acceleration * 10;
            velZ += forward.z() * acceleration * 10;
            a = true;
        } else if (mc.options.keyDown.isDown()) {
            velX -= forward.x() * acceleration * 10;
            velZ -= forward.z() * acceleration * 10;
            a = true;
        }

        if (mc.options.keyRight.isDown()) {
            velX += right.x() * acceleration * 10;
            velZ += right.z() * acceleration * 10;
            b = true;
        } else if (mc.options.keyLeft.isDown()) {
            velX -= right.x() * acceleration * 10;
            velZ -= right.z() * acceleration * 10;
            b = true;
        }

        // Holding two directions at once would be faster than one, so scale it back down
        if (a && b) {
            double diagonal = 1 / Math.sqrt(2);
            velX *= diagonal;
            velZ *= diagonal;
        }
    }

    /**
     * If last tick's move ran into a block and clearly cost horizontal speed, pulls the ramp back
     * down to the speed the player really kept (floored at the minimum) and restarts the delay.
     */
    private void resetRampOnCollision(Player player) {
        if (lastGlidePos == null) return;
        if (!player.horizontalCollision) return;
        if (lastGlideIntendedSpeed < 1.0E-3) return;

        Vec3 now = player.position();
        double moved = Math.hypot(now.x() - lastGlidePos.x(), now.z() - lastGlidePos.z());
        // Any real loss of speed counts (head-on or glancing), only float noise is ignored
        if (moved >= lastGlideIntendedSpeed - 1.0E-3) return;

        double kept = Math.max(startSpeed.get(), moved);
        if (kept < acceleration) acceleration = kept;
        atMaxSpeed = acceleration >= horizontalSpeed.get();
        accelerationDelayTicks = 0;
    }

    // --------------------------------------------------- Vertical acceleration

    /**
     * Mirrors handleAcceleration(). By default, this ramp only applies going downwards.
     */
    private void handleVerticalAcceleration() {
        boolean movingUp = mc.options.keyJump.isDown();
        boolean movingDown = mc.options.keyShift.isDown();

        if (!movingUp && !movingDown) {
            resetVerticalAcceleration();
            return;
        }

        if (movingUp && !accelerateUpward.get()) {
            // Keep resetting so a later downward press starts fresh from the minimum
            resetVerticalAcceleration();
            return;
        }

        if (atMaxVerticalSpeed) return;

        if (verticalAccelerationDelayTicks < ACCELERATION_DELAY_TICKS) {
            verticalAccelerationDelayTicks++;
            return;
        }

        verticalAcceleration = nextSpeedFor(verticalAcceleration, verticalAccelerationStep.get(),
            verticalAccelerationPlateau.get(), verticalSpeed.get());

        if (verticalAcceleration >= verticalSpeed.get()) atMaxVerticalSpeed = true;
    }

    private void handleVerticalSpeed() {
        if (mc.options.keyJump.isDown()) velY += 0.5 * verticalAcceleration;
        else if (mc.options.keyShift.isDown()) velY -= 0.5 * verticalAcceleration;
    }

    private void handleFallMultiplier() {
        if (velY < 0) velY *= fallMultiplier.get();
        else if (velY > 0) velY = 0;
    }

    // ---------------------------------------------------------- The curve maths

    /**
     * The gain for a single tick at currentSpeed: steep at low speed, tapering to zero as
     * currentSpeed approaches plateau. This is an exponential approach (a first-order lag):
     * each tick closes (step / plateau) of the remaining gap.
     */
    private static double curveGain(double currentSpeed, double step, double plateau) {
        if (plateau <= 0) return 0;
        double remainingToPlateau = Math.max(0, plateau - currentSpeed);
        return step * (remainingToPlateau / plateau);
    }

    /**
     * Advances currentSpeed by one tick along the plateau curve, capped at cap.
     */
    private static double nextSpeedFor(double currentSpeed, double step, double plateau, double cap) {
        return Math.min(currentSpeed + curveGain(currentSpeed, step, plateau), cap);
    }

    /**
     * Rewinds currentSpeed back along the curve by a number of ticks. Each tick's loss is
     * recomputed from the speed reached so far, mirroring how it was climbed forward.
     */
    private static double rewindSpeedFor(double currentSpeed, double step, double plateau, double floor, int ticks) {
        double speed = currentSpeed;
        for (int i = 0; i < ticks && speed > floor; i++) {
            speed = Math.max(floor, speed - curveGain(speed, step, plateau));
        }
        return speed;
    }

    // ------------------------------------------------------------------ Resets

    private void zeroAcceleration() {
        resetHorizontalAcceleration();
        resetVerticalAcceleration();
    }

    private void resetHorizontalAcceleration() {
        atMaxSpeed = false;
        accelerationDelayTicks = 0;
        acceleration = startSpeed.get();
    }

    private void resetVerticalAcceleration() {
        atMaxVerticalSpeed = false;
        verticalAccelerationDelayTicks = 0;
        verticalAcceleration = verticalStartSpeed.get();
    }

    private boolean noMovementKeyPressed() {
        return !(mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
            || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown());
    }

    // --------------------------------------------------------------- Overdrive

    /**
     * Runs extra player ticks inside one game tick. Each extra tick advances the ramp once, so the
     * roughly 250 tick climb is squeezed into a handful of game ticks.
     */
    private void runOverdrive() {
        updateOverdriveSpent(mc.player);

        if (!overdrive.get()
            || mc.isPaused()
            || isHorizontalRampStalled(mc.player)) {
            overdriveCarry = 0;
            return;
        }

        // This game tick already ran the player once; the multiplier says how many ticks in total
        overdriveCarry += OVERDRIVE_MULTIPLIER - 1.0;
        int extraTicks = (int) overdriveCarry;
        overdriveCarry -= extraTicks;

        // Real-time speed budget: how far the player has gone this game tick must never pass top speed
        double budget = horizontalSpeed.get();
        double movedThisGameTick = lastGlideIntendedSpeed;

        // Singleplayer: the integrated server kicks the player for sending a second position packet
        // in one client tick, so extra ticks are flown client-side and send no packets of their own
        boolean skipExtraPackets = mc.hasSingleplayerServer();

        for (int i = 0; i < extraTicks; i++) {
            if (isHorizontalRampStalled(mc.player)) {
                overdriveCarry = 0;
                // The ramp topped out during the extra ticks: Overdrive has done all it can
                if (acceleration >= horizontalSpeed.get() * OVERDRIVE_OFF_FRACTION) overdriveSpent = true;
                break;
            }

            double nextTickSpeed = nextSpeedFor(acceleration, accelerationStep.get(), accelerationPlateau.get(), budget);
            if (movedThisGameTick + nextTickSpeed > budget) {
                overdriveCarry = 0;
                break;
            }

            // The world normally counts the player's ticks. oldPos is left alone on purpose, so the
            // camera keeps smoothing across the whole distance flown this game tick
            double savedVelocityY = mc.player.getDeltaMovement().y();
            overdriveExtraTick = true;
            try {
                mc.player.tickCount++;
                mc.player.tick();
                // The position packet is sent from sendChanges(), which the game only calls once per
                // game tick, so each extra tick has to send its own
                if (isOverdrivePacketSane(mc.player)) {
                    if (!skipExtraPackets) mc.player.sendChanges();
                } else {
                    // Invalid state: skip the packet and stop for this tick
                    overdriveCarry = 0;
                    break;
                }
            } finally {
                overdriveExtraTick = false;
            }
            Vec3 after = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(after.x(), savedVelocityY, after.z());
            movedThisGameTick += lastGlideIntendedSpeed;
        }
    }

    /**
     * Tracks whether Overdrive has finished the ramp, and re-arms it once speed drops back down.
     */
    private void updateOverdriveSpent(Player player) {
        if (!player.isFallFlying()) {
            overdriveSpent = false;
            return;
        }

        double top = horizontalSpeed.get();

        if (acceleration <= OVERDRIVE_REARM_SPEED) overdriveSpent = false;
        else if (top > 0 && acceleration >= top * OVERDRIVE_OFF_FRACTION) overdriveSpent = true;
    }

    /**
     * Whether the horizontal ramp is NOT currently on its way up to top speed.
     */
    private boolean isHorizontalRampStalled(Player player) {
        if (!player.isAlive() || !player.isFallFlying()) return true;
        if (noMovementKeyPressed()) return true;
        if (overdriveSpent) return true;
        if (atMaxSpeed) return true;
        if (accelerationDelayTicks < ACCELERATION_DELAY_TICKS) return true;
        return acceleration >= horizontalSpeed.get() * OVERDRIVE_OFF_FRACTION;
    }

    /**
     * The server disconnects on NaN/infinite or huge movement values, so check before sending.
     */
    private static boolean isOverdrivePacketSane(Player p) {
        double x = p.getX(), y = p.getY(), z = p.getZ();
        float yaw = p.getYRot(), pitch = p.getXRot();
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
            && Float.isFinite(yaw) && Float.isFinite(pitch)
            && Math.abs(x) < 3.0E7 && Math.abs(z) < 3.0E7 && Math.abs(y) < 2.0E7;
    }
}
