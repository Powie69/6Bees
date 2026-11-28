/*
 * Copyright (C) 2026 Powie69
 * This file is part of 6Bees.
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Please provide attribution when using or redistributing this code.
 * Derivative works must remain under the same license.
 */

package powie.sixbees.modules;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.BlockBehaviourAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.ChestSwap;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import powie.sixbees.SixBees;


/**
 * Yeah its a skid of voltrafly.
 * but im learning about its "asymptote growth" type shi, im not gonna blindly ctrl + c -> ctrl + v this.
 * ill give proper attribution once this is released
 *
 * @author Powie69
 */

public class EFly extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpeed = settings.createGroup("Speed");

    public final Setting<Boolean> autoTakeOff = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-take-off")
        .description("Automatically takes off when you hold jump without needing to double jump.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> deployInWater = sgGeneral.add(new BoolSetting.Builder()
        .name("deploy-in-water")
        .description("gives you the ability to deploy your elytra while swimming")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> stopInWater = sgGeneral.add(new BoolSetting.Builder()
        .name("stop-in-water")
        .description("Stops flying in water.")
        .defaultValue(false)
        .visible(() -> !deployInWater.get())
        .build()
    );

    public final Setting<Double> fallMultiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-multiplier")
        .description("Controls how fast will you go down naturally.")
        .defaultValue(0)
        .min(0)
        .build()
    );

    public final Setting<Boolean> noUnloadedChunks = sgGeneral.add(new BoolSetting.Builder()
        .name("no-unloaded-chunks")
        .description("Stops you from going into unloaded chunks.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> autoHover = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-hover")
        .description("Automatically hover .3 blocks off ground when holding shift.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> noCrash = sgGeneral.add(new BoolSetting.Builder()
        .name("no-crash")
        .description("Stops you from going into walls.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> crashLookAhead = sgGeneral.add(new IntSetting.Builder()
        .name("crash-look-ahead")
        .description("Distance to look ahead when flying.")
        .defaultValue(5)
        .range(1, 15)
        .sliderMin(1)
        .visible(noCrash::get)
        .build()
    );

    public final Setting<Boolean> instaDrop = sgGeneral.add(new BoolSetting.Builder()
        .name("insta-drop")
        .description("Makes you drop out of flight instantly.")
        .defaultValue(true)
        .build()
    );

    public final Setting<ChestSwapMode> chestSwap = sgGeneral.add(new EnumSetting.Builder<ChestSwapMode>()
        .name("chest-swap")
        .description("Enables ChestSwap when toggling this module.")
        .defaultValue(ChestSwapMode.Always)
        .build()
    );

    public final Setting<Boolean> replace = sgGeneral.add(new BoolSetting.Builder()
        .name("elytra-replace")
        .description("Replaces broken elytra with a new elytra.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> replaceDurability = sgGeneral.add(new IntSetting.Builder()
        .name("replace-durability")
        .description("The durability threshold your elytra will be replaced at.")
        .defaultValue(2)
        .sliderRange(1, 500)
        .visible(replace::get)
        .build()
    );

    public final Setting<Boolean> overdrive = sgGeneral.add(new BoolSetting.Builder()
        .name("overdrive")
        .description("Applies dark magic to force near-instant acceleration to max speed")
        .defaultValue(false)
        .build()
    );

    public final Setting<Double> minimumHorizontalSpeed = sgSpeed.add(new DoubleSetting.Builder()
        .name("minimum-horizontal-speed")
        .description("The speed you start at when moving horizontally (before acceleration starts)")
        .min(0)
        .defaultValue(2.999)
        .build()
    );

    public final Setting<Double> maximumHorizontalSpeed = sgSpeed.add(new DoubleSetting.Builder()
        .name("maximum-horizontal-speed")
        .description("The fastest horizontal speed will go (blocks per tick)")
        .defaultValue(14.999)
        .min(0)
        .build()
    );

    public final Setting<Double> accelerationPlateau = sgSpeed.add(new DoubleSetting.Builder()
        .name("horizontal-acceleration-plateau")
        .description("The horizontal speed where acceleration will tend to 0")
        .min(0.01)
        .defaultValue(14.999)
        .build()
    );

    public final Setting<Double> accelerationStep = sgSpeed.add(new DoubleSetting.Builder()
        .name("horizontal-acceleration-step")
        .description("How fast horizontal speed ramps up")
        .min(0.01)
        .max(5)
        .defaultValue(0.3)
        .build()
    );

    public final Setting<Boolean> accelerateUpward = sgSpeed.add(new BoolSetting.Builder()
        .name("accelerate-upward")
        .description("Also accelerates upwards. Not recommended if vertical speed goes above 5.999")
        .defaultValue(false)
        .build()
    );

    public final Setting<Double> minimumVerticalSpeed = sgSpeed.add(new DoubleSetting.Builder()
        .name("minimum-vertical-speed")
        .description("The speed you start at when moving vertically, (before acceleration starts)")
        .min(0)
        .defaultValue(5.999)
        .build()
    );

    public final Setting<Double> maximumVerticalSpeed = sgSpeed.add(new DoubleSetting.Builder()
        .name("maximum-vertical-speed")
        .description("The fastest vertical speed will go (blocks per tick)")
        .defaultValue(5.999)
        .min(0)
        .build()
    );

    public final Setting<Double> verticalAccelerationPlateau = sgSpeed.add(new DoubleSetting.Builder()
        .name("vertical-acceleration-plateau")
        .description("The vertical speed where acceleration will tend to 0")
        .min(0.01)
        .defaultValue(29.999)
        .build()
    );

    public final Setting<Double> verticalAccelerationStep = sgSpeed.add(new DoubleSetting.Builder()
        .name("vertical-acceleration-step")
        .description("How fast vertical speed ramps up")
        .min(0.01)
        .max(5)
        .defaultValue(1.0)
        .build()
    );

    // constants
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

    // states
    private double currentHorizontalSpeed;
    private boolean atMaxSpeed;
    private int accelerationDelayTicks;

    private double currentVerticalSpeed;
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

    // Auto take-off (hold jump for 8 ticks to launch)
    private boolean lastJumpPressed;
    private boolean incrementJumpTimer;
    private int jumpTimer;

    private final StaticInstaDropListener staticInstaDropListener = new StaticInstaDropListener();
    private final StaticGroundListener staticGroundListener = new StaticGroundListener();

    public EFly() {
        super(SixBees.CATEGORY, "EFly", "Elytra fly optimized for 6b6t");
    }

    @Override
    public void onActivate() {
        atMaxSpeed = false;
        accelerationDelayTicks = 0;
        lastGlidePos = null;
        lastGlideIntendedSpeed = 0;
        overdriveCarry = 0;
        overdriveSpent = false;
        overdriveExtraTick = false;
        lastJumpPressed = false;
        incrementJumpTimer = false;
        jumpTimer = 0;
        MeteorClient.EVENT_BUS.unsubscribe(staticInstaDropListener);
        MeteorClient.EVENT_BUS.unsubscribe(staticGroundListener);

        // Carry over real horizontal speed instead of snapping back to the minimum, so turning the
        // module on while already moving picks the ramp up from wherever you actually were
        double measuredSpeed = Math.hypot(mc.player.getDeltaMovement().x(), mc.player.getDeltaMovement().z());
        currentHorizontalSpeed = Math.clamp(measuredSpeed, minimumHorizontalSpeed.get(), maximumHorizontalSpeed.get());
        if (currentHorizontalSpeed >= maximumHorizontalSpeed.get()) atMaxSpeed = true;

        resetVerticalAcceleration();

        if ((chestSwap.get() == ChestSwapMode.Always || chestSwap.get() == ChestSwapMode.WaitForGround)
            && mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() != Items.ELYTRA && isActive()) {
            Modules.get().get(ChestSwap.class).swap();
        }
    }

    @Override
    public void onDeactivate() {
        if (chestSwap.get() == ChestSwapMode.Always && mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            Modules.get().get(ChestSwap.class).swap();
        } else if (chestSwap.get() == ChestSwapMode.WaitForGround) {
            MeteorClient.EVENT_BUS.subscribe(staticGroundListener);
        }

        if (instaDrop.get() && mc.player.isFallFlying()) MeteorClient.EVENT_BUS.subscribe(staticInstaDropListener);
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER)) return;

        autoTakeoff();

        if (deployInWater.get()
            && mc.options.keyJump.isDown()
            && !mc.player.isFallFlying()
            && mc.player.isInLiquid()) {
            mc.player.startFallFlying();
        }

        if (!mc.player.isFallFlying()) {
            lastGlidePos = null;
            lastGlideIntendedSpeed = 0;
            return;
        }
        // Vanilla horizontal momentum is discarded; speed is entirely what the ramp says it is
        velX = 0;
        velZ = 0;
        velY = event.movement.y();

        // The * 0.1 here is canceled by the * 10 in handleHorizontalSpeed()
        forward = Vec3.directionFromRotation(0, mc.player.getYRot()).scale(0.1);
        right = Vec3.directionFromRotation(0, mc.player.getYRot() + 90).scale(0.1);

        if (mc.player.isInWater() && stopInWater.get()) {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
            return;
        }

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

        if (noUnloadedChunks.get()) {
            int chunkX = (int) ((mc.player.getX() + velX) / 16.0);
            int chunkZ = (int) ((mc.player.getZ() + velZ) / 16.0);
            if (mc.level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                ((IVec3) event.movement).meteor$set(velX, velY, velZ);
            } else {
                zeroAcceleration();
                ((IVec3) event.movement).meteor$set(0.0, velY, 0.0);
            }
        } else {
            ((IVec3) event.movement).meteor$set(velX, velY, velZ);
        }

        if (noCrash.get() && mc.player.isFallFlying()) {
            Vec3 lookAheadPos = mc.player.position().add(mc.player.getDeltaMovement().normalize().scale(crashLookAhead.get()));
            ClipContext clipContext = new ClipContext(
                mc.player.position(),
                new Vec3(lookAheadPos.x(),
                    mc.player.getY(),
                    lookAheadPos.z()),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, mc.player
            );
            BlockHitResult hitResult = mc.level.clip(clipContext);
            if (hitResult != null && hitResult.getType() == Type.BLOCK) {
                ((IVec3) event.movement).meteor$set(0.0, velY, 0.0);
            }
        }

        if (autoHover.get() && mc.player.input.keyPresses.shift() && !Modules.get().get(Freecam.class).isActive() && mc.player.isFallFlying()) {
            BlockState underState = mc.level.getBlockState(mc.player.blockPosition().below());
            Block under = underState.getBlock();
            BlockState under2State = mc.level.getBlockState(mc.player.blockPosition().below().below());
            Block under2 = under2State.getBlock();

            final boolean underCollidable = ((BlockBehaviourAccessor) under).meteor$isHasCollision() || !underState.getFluidState().isEmpty();
            final boolean under2Collidable = ((BlockBehaviourAccessor) under2).meteor$isHasCollision() || !under2State.getFluidState().isEmpty();

            if (!underCollidable && under2Collidable) {
                ((IVec3) event.movement).meteor$set(event.movement.x, -0.1f, event.movement.z);

                mc.player.setXRot(Mth.clamp(mc.player.getXRot(0), -50.f, 20.f)); // clamp between -50 and 20 (>= 30 will pop you off, but lag makes that threshold lower)
            }

            if (underCollidable) {
                ((IVec3) event.movement).meteor$set(event.movement.x, -0.03f, event.movement.z);

                mc.player.setXRot(Mth.clamp(mc.player.getXRot(0), -50.f, 20.f));

                if (mc.player.position().y <= mc.player.blockPosition().below().getY() + 1.34f) {
                    ((IVec3) event.movement).meteor$set(event.movement.x, 0, event.movement.z);
                    mc.player.setShiftKeyDown(false);
                }
            }
        }

        lastGlidePos = mc.player.position();
        lastGlideIntendedSpeed = Math.hypot(velX, velZ);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        handleElytraReplace();
        runOverdrive();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof ClientboundPlayerPositionPacket)) return;

        if (!mc.player.isFallFlying()) {
            zeroAcceleration();
            return;
        }
        // The correction goes through, but only costs RUBBERBAND_REWIND_TICKS worth of ramp
        double keptSpeed = rewindSpeedFor(
            currentHorizontalSpeed,
            accelerationStep.get(),
            accelerationPlateau.get(),
            minimumHorizontalSpeed.get());

        if (keptSpeed < currentHorizontalSpeed) {
            currentHorizontalSpeed = keptSpeed;
            atMaxSpeed = false;
            accelerationDelayTicks = 0;

            currentVerticalSpeed = rewindSpeedFor(
                currentVerticalSpeed,
                verticalAccelerationStep.get(),
                verticalAccelerationPlateau.get(),
                minimumVerticalSpeed.get()
            );
            atMaxVerticalSpeed = false;
            verticalAccelerationDelayTicks = 0;
        }
    }

    // Horizontal acceleration

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
        double cap = maximumHorizontalSpeed.get();
        currentHorizontalSpeed = nextSpeedFor(
            currentHorizontalSpeed,
            accelerationStep.get(),
            accelerationPlateau.get(),
            cap);

        if (currentHorizontalSpeed >= maximumHorizontalSpeed.get()) atMaxSpeed = true;
    }

    /**
     * Turns the current ramp speed into this tick's horizontal velocity from the held keys.
     */
    private void handleHorizontalSpeed() {
        int forwardAmt = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
        int rightAmt = (mc.options.keyRight.isDown() ? 1 : 0) - (mc.options.keyLeft.isDown() ? 1 : 0);

        if (forwardAmt != 0) {
            velX += forward.x() * currentHorizontalSpeed * 10 * forwardAmt;
            velZ += forward.z() * currentHorizontalSpeed * 10 * forwardAmt;
        }
        if (rightAmt != 0) {
            velX += right.x() * currentHorizontalSpeed * 10 * rightAmt;
            velZ += right.z() * currentHorizontalSpeed * 10 * rightAmt;
        }

        if (forwardAmt != 0 && rightAmt != 0) {
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

        double kept = Math.max(minimumHorizontalSpeed.get(), moved);
        if (kept < currentHorizontalSpeed) currentHorizontalSpeed = kept;
        atMaxSpeed = currentHorizontalSpeed >= maximumHorizontalSpeed.get();
        accelerationDelayTicks = 0;
    }

    // Vertical acceleration

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

        currentVerticalSpeed = nextSpeedFor(currentVerticalSpeed, verticalAccelerationStep.get(),
            verticalAccelerationPlateau.get(), maximumVerticalSpeed.get());

        if (currentVerticalSpeed >= maximumVerticalSpeed.get()) atMaxVerticalSpeed = true;
    }

    private void handleVerticalSpeed() {
        if (mc.options.keyJump.isDown()) velY += 0.5 * currentVerticalSpeed;
        else if (mc.options.keyShift.isDown()) velY -= 0.5 * currentVerticalSpeed;
    }

    private void handleFallMultiplier() {
        if (velY < 0) velY *= fallMultiplier.get();
        else if (velY > 0) velY = 0;
    }

    // curve maths

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
    private static double rewindSpeedFor(double currentSpeed, double step, double plateau, double floor) {
        double speed = currentSpeed;
        for (int i = 0; i < EFly.RUBBERBAND_REWIND_TICKS && speed > floor; i++) {
            speed = Math.max(floor, speed - curveGain(speed, step, plateau));
        }
        return speed;
    }

    // Overdrive

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
        double budget = maximumHorizontalSpeed.get();
        double movedThisGameTick = lastGlideIntendedSpeed;

        // Singleplayer: the integrated server kicks the player for sending a second position packet
        // in one client tick, so extra ticks are flown client-side and send no packets of their own
        boolean skipExtraPackets = mc.hasSingleplayerServer();

        for (int i = 0; i < extraTicks; i++) {
            if (isHorizontalRampStalled(mc.player)) {
                overdriveCarry = 0;
                // The ramp topped out during the extra ticks: Overdrive has done all it can
                if (currentHorizontalSpeed >= maximumHorizontalSpeed.get() * OVERDRIVE_OFF_FRACTION)
                    overdriveSpent = true;
                break;
            }

            double nextTickSpeed = nextSpeedFor(currentHorizontalSpeed, accelerationStep.get(), accelerationPlateau.get(), budget);
            if (movedThisGameTick + nextTickSpeed > budget) {
                overdriveCarry = 0;
                break;
            }

            // The world normally counts the player's ticks. oldPos is left alone on purpose, so the
            // camera keeps smoothing across the whole distance flown this game tick
            double savedVelocityY = mc.player.getDeltaMovement().y();
            overdriveExtraTick = true;
            try { // why is this inside a try catch?
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

        double top = maximumHorizontalSpeed.get();

        if (currentHorizontalSpeed <= OVERDRIVE_REARM_SPEED) overdriveSpent = false;
        else if (top > 0 && currentHorizontalSpeed >= top * OVERDRIVE_OFF_FRACTION) overdriveSpent = true;
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
        return currentHorizontalSpeed >= maximumHorizontalSpeed.get() * OVERDRIVE_OFF_FRACTION;
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

    private void handleElytraReplace() {
        if (!replace.get()) return;

        ItemStack chestStack = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestStack.getItem() != Items.ELYTRA) return;
        if (chestStack.getMaxDamage() - chestStack.getDamageValue() > replaceDurability.get()) return;

        FindItemResult elytra = InvUtils.find(stack -> stack.getMaxDamage() - stack.getDamageValue() > replaceDurability.get()
            && stack.getItem() == Items.ELYTRA);
        if (elytra.found()) {
            InvUtils.move().from(elytra.slot()).toArmor(2);
        }
    }

    private void autoTakeoff() {
        if (incrementJumpTimer) jumpTimer++;

        boolean jumpPressed = mc.options.keyJump.isDown();
        if (autoTakeOff.get()) {
            if (!lastJumpPressed && !mc.player.isFallFlying()) {
                jumpTimer = 0;
                incrementJumpTimer = true;
            }

            if (jumpTimer >= 8) {
                jumpTimer = 0;
                incrementJumpTimer = false;
                mc.player.setJumping(false);
                mc.player.setSprinting(true);
                mc.player.jumpFromGround();
                mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
            }
        }

        lastJumpPressed = jumpPressed;
    }


    // Resets

    private void zeroAcceleration() {
        resetHorizontalAcceleration();
        resetVerticalAcceleration();
    }

    private void resetHorizontalAcceleration() {
        atMaxSpeed = false;
        accelerationDelayTicks = 0;
        currentHorizontalSpeed = minimumHorizontalSpeed.get();
    }

    private void resetVerticalAcceleration() {
        atMaxVerticalSpeed = false;
        verticalAccelerationDelayTicks = 0;
        currentVerticalSpeed = minimumVerticalSpeed.get();
    }

    private boolean noMovementKeyPressed() {
        boolean forward = mc.options.keyUp.isDown() != mc.options.keyDown.isDown();
        boolean right = mc.options.keyRight.isDown() != mc.options.keyLeft.isDown();
        return !forward && !right;
    }

    public enum ChestSwapMode {
        Always,
        Never,
        WaitForGround
    }

    // static listeners
    private class StaticInstaDropListener {
        @EventHandler
        private void onInstadropTick(TickEvent.Post event) {
            if (mc.player != null && mc.player.isFallFlying()) {
                mc.player.setDeltaMovement(0.0, 0.0, 0.0);
                mc.player.connection.send(new StatusOnly(true, mc.player.horizontalCollision));
            } else {
                MeteorClient.EVENT_BUS.unsubscribe(staticInstaDropListener);
            }
        }
    }

    private class StaticGroundListener {
        @EventHandler
        private void chestSwapGroundListener(PlayerMoveEvent event) {
            if (mc.player != null
                && mc.player.onGround()
                && mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                Modules.get().get(ChestSwap.class).swap();
                MeteorClient.EVENT_BUS.unsubscribe(staticGroundListener);
            }
        }
    }
}
