package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.annotations.FlowExclude;
import com.heypixel.heypixelmod.obsoverlay.annotations.ParameterObfuscationExclude;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.*;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.RandomUtils;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

@ModuleInfo(
        name = "Scaffold",
        description = "Automatically places blocks under you",
        category = Category.MOVEMENT
)
public class Scaffold extends Module {
    public static final List<Block> blacklistedBlocks = Arrays.asList(
            Blocks.AIR,
            Blocks.WATER,
            Blocks.LAVA,
            Blocks.ENCHANTING_TABLE,
            Blocks.GLASS_PANE,
            Blocks.GLASS_PANE,
            Blocks.IRON_BARS,
            Blocks.SNOW,
            Blocks.COAL_ORE,
            Blocks.DIAMOND_ORE,
            Blocks.EMERALD_ORE,
            Blocks.CHEST,
            Blocks.TRAPPED_CHEST,
            Blocks.TORCH,
            Blocks.ANVIL,
            Blocks.TRAPPED_CHEST,
            Blocks.NOTE_BLOCK,
            Blocks.JUKEBOX,
            Blocks.TNT,
            Blocks.GOLD_ORE,
            Blocks.IRON_ORE,
            Blocks.LAPIS_ORE,
            Blocks.STONE_PRESSURE_PLATE,
            Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE,
            Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Blocks.STONE_BUTTON,
            Blocks.LEVER,
            Blocks.TALL_GRASS,
            Blocks.TRIPWIRE,
            Blocks.TRIPWIRE_HOOK,
            Blocks.RAIL,
            Blocks.CORNFLOWER,
            Blocks.RED_MUSHROOM,
            Blocks.BROWN_MUSHROOM,
            Blocks.VINE,
            Blocks.SUNFLOWER,
            Blocks.LADDER,
            Blocks.FURNACE,
            Blocks.SAND,
            Blocks.CACTUS,
            Blocks.DISPENSER,
            Blocks.DROPPER,
            Blocks.CRAFTING_TABLE,
            Blocks.COBWEB,
            Blocks.PUMPKIN,
            Blocks.COBBLESTONE_WALL,
            Blocks.OAK_FENCE,
            Blocks.REDSTONE_TORCH,
            Blocks.FLOWER_POT
    );
    public Vector2f correctRotation = new Vector2f();
    public Vector2f rots = new Vector2f();
    public Vector2f lastRots = new Vector2f();
    private int offGroundTicks = 0;
    public ModeValue mode = ValueBuilder.create(this, "Mode").setDefaultModeIndex(0).setModes("Normal", "Telly Bridge", "Keep Y").build().getModeValue();
    public BooleanValue eagle = ValueBuilder.create(this, "Eagle")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.mode.isCurrentMode("Normal"))
            .build()
            .getBooleanValue();
    public BooleanValue sneak = ValueBuilder.create(this, "Sneak").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue snap = ValueBuilder.create(this, "Snap")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.mode.isCurrentMode("Normal"))
            .build()
            .getBooleanValue();
    public BooleanValue hideSnap = ValueBuilder.create(this, "Hide Snap Rotation")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.mode.isCurrentMode("Normal") && this.snap.getCurrentValue())
            .build()
            .getBooleanValue();
    public BooleanValue renderItemSpoof = ValueBuilder.create(this, "Render Item Spoof").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue renderBlockCounter = ValueBuilder.create(this, "Render Block Counter").setDefaultBooleanValue(false).build().getBooleanValue();

    public BooleanValue keepFoV = ValueBuilder.create(this, "Keep Fov").setDefaultBooleanValue(true).build().getBooleanValue();
    FloatValue fov = ValueBuilder.create(this, "Fov")
            .setDefaultFloatValue(1.15F)
            .setMaxFloatValue(2.0F)
            .setMinFloatValue(1.0F)
            .setFloatStep(0.05F)
            .setVisibility(() -> this.keepFoV.getCurrentValue())
            .build()
            .getFloatValue();
    int oldSlot;
    private Scaffold.BlockPosWithFacing pos;
    private int lastSneakTicks;
    public int baseY = -1;

    public ModeValue rotationType = ValueBuilder.create(this, "Rotations Type").setModes("None", "Linear", "Normal", "Sigmoid").setDefaultModeIndex(0).build().getModeValue();
    public FloatValue turnSpeedX = ValueBuilder.create(this, "Turn Speed X")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(180.0F)
            .setVisibility(() -> this.rotationType.isCurrentMode("Linear") || this.rotationType.isCurrentMode("Sigmoid"))
            .build()
            .getFloatValue();
    public FloatValue turnSpeedY = ValueBuilder.create(this, "Turn Speed Y")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(180.0F)
            .setVisibility(() -> this.rotationType.isCurrentMode("Linear") || this.rotationType.isCurrentMode("Sigmoid"))
            .build()
            .getFloatValue();

    public FloatValue yawAdjust = ValueBuilder.create(this, "Yaw Adjust")
            .setDefaultFloatValue(0.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(-10.0F)
            .setMaxFloatValue(10.0F)
            .setVisibility(() -> this.rotationType.isCurrentMode("Linear") || this.rotationType.isCurrentMode("Sigmoid") || this.rotationType.isCurrentMode("Normal"))
            .build()
            .getFloatValue();

    public FloatValue pitchAdjust = ValueBuilder.create(this, "Pitch Adjust")
            .setDefaultFloatValue(0.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(-10.0F)
            .setMaxFloatValue(10.0F)
            .setVisibility(() -> this.rotationType.isCurrentMode("Linear") || this.rotationType.isCurrentMode("Sigmoid") || this.rotationType.isCurrentMode("Normal"))
            .build()
            .getFloatValue();

    private float blockCounterWidth;
    private float blockCounterHeight;

    public static boolean isValidStack(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof BlockItem) || stack.getCount() <= 1) {
            return false;
        } else if (!InventoryUtils.isItemValid(stack)) {
            return false;
        } else {
            String string = stack.getDisplayName().getString();
            if (string.contains("Click") || string.contains("点击")) {
                return false;
            } else if (stack.getItem() instanceof ItemNameBlockItem) {
                return false;
            } else {
                Block block = ((BlockItem)stack.getItem()).getBlock();
                if (block instanceof FlowerBlock) {
                    return false;
                } else if (block instanceof BushBlock) {
                    return false;
                } else if (block instanceof FungusBlock) {
                    return false;
                } else if (block instanceof CropBlock) {
                    return false;
                } else {
                    return block instanceof SlabBlock ? false : !blacklistedBlocks.contains(block);
                }
            }
        }
    }

    public static boolean isOnBlockEdge(float sensitivity) {
        return !mc.level
                .getCollisions(mc.player, mc.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate((double)(-sensitivity), 0.0, (double)(-sensitivity)))
                .iterator()
                .hasNext();
    }

    @EventTarget
    public void onFoV(EventUpdateFoV e) {
        if (this.keepFoV.getCurrentValue() && MoveUtils.isMoving()) {
            e.setFov(this.fov.getCurrentValue() + (float)PlayerUtils.getMoveSpeedEffectAmplifier() * 0.13F);
        }
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            this.oldSlot = mc.player.getInventory().selected;
            this.rots.set(mc.player.getYRot(), mc.player.getXRot());
            this.lastRots.set(mc.player.yRotO, mc.player.xRotO);
            this.pos = null;
            this.baseY = 10000;
        }
    }

    @Override
    public void onDisable() {
        boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
        boolean isHoldingShift = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyShift.getKey().getValue());
        mc.options.keyJump.setDown(isHoldingJump);
        mc.options.keyShift.setDown(isHoldingShift);
        mc.options.keyUse.setDown(false);
        mc.player.getInventory().selected = this.oldSlot;
        RotationManager.rotations.set(mc.player.getYRot(), mc.player.getXRot());
    }

    @EventTarget
    public void onUpdateHeldItem(EventUpdateHeldItem e) {
        if (this.renderItemSpoof.getCurrentValue() && e.getHand() == InteractionHand.MAIN_HAND) {
            e.setItem(mc.player.getInventory().getItem(this.oldSlot));
        }
    }

    @EventTarget(1)
    public void onEventEarlyTick(EventRunTicks e) {
        if (e.getType() == EventType.PRE && mc.screen == null && mc.player != null) {
            int slotID = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.getItem() instanceof BlockItem && isValidStack(stack)) {
                    slotID = i;
                    break;
                }
            }

            if (mc.player.onGround()) {
                this.offGroundTicks = 0;
            } else {
                this.offGroundTicks++;
            }

            if (slotID != -1 && mc.player.getInventory().selected != slotID) {
                mc.player.getInventory().selected = slotID;
            }

            boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
            if (this.baseY == -1
                    || this.baseY > (int)Math.floor(mc.player.getY()) - 1
                    || mc.player.onGround()
                    || !PlayerUtils.movementInput()
                    || isHoldingJump
                    || this.mode.isCurrentMode("Normal")) {
                this.baseY = (int)Math.floor(mc.player.getY()) - 1;
            }

            this.getBlockPos();
            if (this.pos != null) {
                this.correctRotation = this.getPlayerYawRotation();
                this.correctRotation.setX(this.correctRotation.getX() + this.yawAdjust.getCurrentValue());
                this.correctRotation.setY(this.correctRotation.getY() + this.pitchAdjust.getCurrentValue());

                if (this.rotationType.isCurrentMode("Linear")) {
                    this.updateLinearRotations(this.correctRotation);
                } else if (this.rotationType.isCurrentMode("Sigmoid")) {
                    this.updateSigmoidRotations(this.correctRotation);
                } else if (this.rotationType.isCurrentMode("Normal")) {
                    if (this.snap.getCurrentValue() && !isHoldingJump) {
                        this.doSnap();
                    } else {
                        this.rots.setX(RotationUtils.rotateToYaw(180.0F, this.rots.getX(), this.correctRotation.getX()));
                        this.rots.setY(this.correctRotation.getY());
                    }
                } else {
                    this.rots.set(mc.player.getYRot(), mc.player.getXRot());
                }
            }

            RotationManager.rotations.set(this.rots);

            if (this.sneak.getCurrentValue()) {
                this.lastSneakTicks++;
                if (this.lastSneakTicks == 18) {
                    if (mc.player.isSprinting()) {
                        mc.options.keySprint.setDown(false);
                        mc.player.setSprinting(false);
                    }
                    mc.options.keyShift.setDown(true);
                } else if (this.lastSneakTicks >= 21) {
                    mc.options.keyShift.setDown(false);
                    this.lastSneakTicks = 0;
                }
            }

            if (this.mode.isCurrentMode("Telly Bridge")) {
                mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);
                if (this.offGroundTicks < 1 && PlayerUtils.movementInput()) {
                    this.rots.setX(RotationUtils.rotateToYaw(180.0F, this.rots.getX(), mc.player.getYRot()));
                    this.lastRots.set(this.rots.getX(), this.rots.getY());
                    return;
                }
            } else if (this.mode.isCurrentMode("Keep Y")) {
                mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);
            } else {
                if (this.eagle.getCurrentValue()) {
                    mc.options.keyShift.setDown(mc.player.onGround() && isOnBlockEdge(0.3F));
                }
            }
            this.lastRots.set(this.rots.getX(), this.rots.getY());
        }
    }

    private void updateLinearRotations(Vector2f targetRotations) {
        float targetYaw = targetRotations.x;
        float targetPitch = targetRotations.y;
        float maxSpeedX = this.turnSpeedX.getCurrentValue();
        float maxSpeedY = this.turnSpeedY.getCurrentValue();

        float deltaYaw = RotationUtils.normalizeAngle(targetYaw - this.rots.x);
        float deltaPitch = RotationUtils.normalizeAngle(targetPitch - this.rots.y);

        float newYaw = this.rots.x + Math.min(Math.abs(deltaYaw), maxSpeedX) * Math.signum(deltaYaw);
        float newPitch = this.rots.y + Math.min(Math.abs(deltaPitch), maxSpeedY) * Math.signum(deltaPitch);

        this.rots.set(newYaw, newPitch);
    }

    private void updateSigmoidRotations(Vector2f targetRotations) {
        float targetYaw = targetRotations.x;
        float targetPitch = targetRotations.y;
        float maxSpeedX = this.turnSpeedX.getCurrentValue();
        float maxSpeedY = this.turnSpeedY.getCurrentValue();
        float smoothingFactor = 0.5f;

        float deltaYaw = RotationUtils.normalizeAngle(targetYaw - this.rots.x);
        float deltaPitch = RotationUtils.normalizeAngle(targetPitch - this.rots.y);

        float sigmoidX = (float) (1.0 / (1.0 + Math.exp(-Math.abs(deltaYaw) * smoothingFactor)));
        float sigmoidY = (float) (1.0 / (1.0 + Math.exp(-Math.abs(deltaPitch) * smoothingFactor)));

        float turnRateX = sigmoidX * maxSpeedX;
        float turnRateY = sigmoidY * maxSpeedY;

        float newYaw = this.rots.x + Math.min(Math.abs(deltaYaw), turnRateX) * Math.signum(deltaYaw);
        float newPitch = this.rots.y + Math.min(Math.abs(deltaPitch), turnRateY) * Math.signum(deltaPitch);

        this.rots.set(newYaw, newPitch);
    }

    private void doSnap() {
        boolean shouldPlaceBlock = false;
        HitResult objectPosition = RayTraceUtils.rayCast(1.0F, this.rots);
        if (objectPosition.getType() == Type.BLOCK) {
            BlockHitResult position = (BlockHitResult)objectPosition;
            if (position.getBlockPos().equals(this.pos.position) && position.getDirection() != Direction.UP) {
                shouldPlaceBlock = true;
            }
        }

        if (!shouldPlaceBlock) {
            this.rots.setX(mc.player.getYRot() + RandomUtils.nextFloat(0.0F, 0.5F) - 0.25F);
        }
    }

    @EventTarget
    public void onClick(EventClick e) {
        e.setCancelled(true);
        if (mc.screen == null && mc.player != null && this.pos != null && (!this.mode.isCurrentMode("Telly Bridge") || this.offGroundTicks >= 1)) {
            if (this.checkPlace(this.pos)) {
                this.placeBlock();
            }
        }
    }

    private boolean checkPlace(Scaffold.BlockPosWithFacing data) {
        Vec3 center = new Vec3((double)data.position.getX() + 0.5, (double)((float)data.position.getY() + 0.5F), (double)data.position.getZ() + 0.5);
        Vec3 hit = center.add(
                new Vec3((double)data.facing.getNormal().getX() * 0.5, (double)data.facing.getNormal().getY() * 0.5, (double)data.facing.getNormal().getZ() * 0.5)
        );
        Vec3 relevant = hit.subtract(mc.player.getEyePosition());
        return relevant.lengthSqr() <= 20.25 && relevant.normalize().dot(Vec3.atLowerCornerOf(data.facing.getNormal().multiply(-1)).normalize()) >= 0.0;
    }

    private void placeBlock() {
        if (this.pos != null && isValidStack(mc.player.getMainHandItem())) {
            Direction sbFace = this.pos.facing();
            boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
            if (sbFace != null
                    && (sbFace != Direction.UP || mc.player.onGround() || !PlayerUtils.movementInput() || isHoldingJump || this.mode.isCurrentMode("Normal"))
                    && this.shouldBuild()) {
                InteractionResult result = mc.gameMode
                        .useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(getVec3(this.pos.position(), sbFace), sbFace, this.pos.position(), false));
                if (result == InteractionResult.SUCCESS) {
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    this.pos = null;
                }
            }
        }
    }

    @FlowExclude
    @ParameterObfuscationExclude
    private Vector2f getPlayerYawRotation() {
        return mc.player != null && this.pos != null
                ? new Vector2f(RotationUtils.getRotations(this.pos.position(), 0.0F).getYaw(), RotationUtils.getRotations(this.pos.position(), 0.0F).getPitch())
                : new Vector2f(0.0F, 0.0F);
    }

    private boolean shouldBuild() {
        BlockPos playerPos = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.5, mc.player.getZ());
        return mc.level.isEmptyBlock(playerPos) && isValidStack(mc.player.getMainHandItem());
    }

    @FlowExclude
    @ParameterObfuscationExclude
    private void getBlockPos() {
        Vec3 baseVec = mc.player.getEyePosition().add(mc.player.getDeltaMovement().multiply(2.0, 2.0, 2.0));
        if (mc.player.getDeltaMovement().y < 0.01) {
            FallingPlayer fallingPlayer = new FallingPlayer(mc.player);
            fallingPlayer.calculate(2);
            baseVec = new Vec3(baseVec.x, Math.max(fallingPlayer.y + (double)mc.player.getEyeHeight(), baseVec.y), baseVec.z);
        }

        BlockPos base = BlockPos.containing(baseVec.x, (double)((float)this.baseY + 0.1F), baseVec.z);
        int baseX = base.getX();
        int baseZ = base.getZ();
        if (!mc.level.getBlockState(base).entityCanStandOn(mc.level, base, mc.player)) {
            if (!this.checkBlock(baseVec, base)) {
                for (int d = 1; d <= 6; d++) {
                    if (this.checkBlock(baseVec, new BlockPos(baseX, this.baseY - d, baseZ))) {
                        return;
                    }

                    for (int x = 1; x <= d; x++) {
                        for (int z = 0; z <= d - x; z++) {
                            int y = d - x - z;

                            for (int rev1 = 0; rev1 <= 1; rev1++) {
                                for (int rev2 = 0; rev2 <= 1; rev2++) {
                                    if (this.checkBlock(baseVec, new BlockPos(baseX + (rev1 == 0 ? x : -x), this.baseY - y, baseZ + (rev2 == 0 ? z : -z)))) {
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean checkBlock(Vec3 baseVec, BlockPos bp) {
        if (!(mc.level.getBlockState(bp).getBlock() instanceof AirBlock)) {
            return false;
        } else {
            Vec3 center = new Vec3((double)bp.getX() + 0.5, (double)((float)bp.getY() + 0.5F), (double)bp.getZ() + 0.5);

            for (Direction sbface : Direction.values()) {
                Vec3 hit = center.add(
                        new Vec3((double)sbface.getNormal().getX() * 0.5, (double)sbface.getNormal().getY() * 0.5, (double)sbface.getNormal().getZ() * 0.5)
                );
                Vec3i baseBlock = bp.offset(sbface.getNormal());
                BlockPos po = new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ());
                if (mc.level.getBlockState(po).entityCanStandOnFace(mc.level, po, mc.player, sbface)) {
                    Vec3 relevant = hit.subtract(baseVec);
                    if (relevant.lengthSqr() <= 20.25 && relevant.normalize().dot(Vec3.atLowerCornerOf(sbface.getNormal()).normalize()) >= 0.0) {
                        this.pos = new Scaffold.BlockPosWithFacing(new BlockPos(baseBlock), sbface.getOpposite());
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @FlowExclude
    @ParameterObfuscationExclude
    public static Vec3 getVec3(BlockPos pos, Direction face) {
        double x = (double)pos.getX() + 0.5;
        double y = (double)pos.getY() + 0.5;
        double z = (double)pos.getZ() + 0.5;
        if (face != Direction.UP && face != Direction.DOWN) {
            y += 0.08;
        } else {
            x += MathUtils.getRandomDoubleInRange(0.3, -0.3);
            z += MathUtils.getRandomDoubleInRange(0.3, -0.3);
        }
        if (face == Direction.WEST || face == Direction.EAST) {
            z += MathUtils.getRandomDoubleInRange(0.3, -0.3);
        }
        if (face == Direction.SOUTH || face == Direction.NORTH) {
            x += MathUtils.getRandomDoubleInRange(0.3, -0.3);
        }
        return new Vec3(x, y, z);
    }

    @EventTarget
    public void onShader(EventShader e) {
        if (this.renderBlockCounter.getCurrentValue() && mc.player != null) {
            float screenWidth = (float) mc.getWindow().getGuiScaledWidth();
            float screenHeight = (float) mc.getWindow().getGuiScaledHeight();
            float x = (screenWidth - this.blockCounterWidth) / 2.0F - 3.0F;
            float y = screenHeight / 2.0F + 15.0F;
            RenderUtils.drawRoundedRect(e.getStack(), x, y, this.blockCounterWidth + 6.0F, this.blockCounterHeight + 8.0F, 5.0F, Integer.MIN_VALUE);
        }
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        if (this.renderBlockCounter.getCurrentValue() && mc.player != null) {
            int blockCount = 0;
            for (ItemStack itemStack : mc.player.getInventory().items) {
                if (itemStack.getItem() instanceof BlockItem) {
                    blockCount += itemStack.getCount();
                }
            }
            String text = "Blocks: " + blockCount;
            double backgroundScale = 0.4;
            double textScale = 0.35;

            this.blockCounterWidth = Fonts.opensans.getWidth(text, backgroundScale);
            this.blockCounterHeight = (float) Fonts.opensans.getHeight(true, backgroundScale);

            float screenWidth = (float) mc.getWindow().getGuiScaledWidth();
            float screenHeight = (float) mc.getWindow().getGuiScaledHeight();

            float backgroundX = (screenWidth - this.blockCounterWidth) / 2.0F - 3.0F;
            float backgroundY = screenHeight / 2.0F + 15.0F;

            float textWidth = Fonts.opensans.getWidth(text, textScale);
            float textHeight = (float) Fonts.opensans.getHeight(true, textScale);

            float textX = backgroundX + (this.blockCounterWidth + 6.0F - textWidth) / 2.0F;
            float textY = backgroundY + 4.0F + (this.blockCounterHeight + 4.0F) / 2.0F - textHeight / 2.0F - 2.0F;

            e.getStack().pushPose();

            StencilUtils.write(false);
            RenderUtils.drawRoundedRect(e.getStack(), backgroundX, backgroundY, this.blockCounterWidth + 6.0F, this.blockCounterHeight + 8.0F, 5.0F, Integer.MIN_VALUE);
            StencilUtils.erase(true);
            int headerColor = new Color(150, 45, 45, 255).getRGB();
            RenderUtils.fill(e.getStack(), backgroundX, backgroundY, backgroundX + this.blockCounterWidth + 6.0F, backgroundY + 3.0F, headerColor);

            int bodyColor = new Color(0, 0, 0, 120).getRGB();
            RenderUtils.fill(e.getStack(), backgroundX, backgroundY + 3.0F, backgroundX + this.blockCounterWidth + 6.0F, backgroundY + this.blockCounterHeight + 8.0F, bodyColor);

            Fonts.opensans.render(e.getStack(), text, textX, textY, Color.WHITE, true, textScale);
            StencilUtils.dispose();
            e.getStack().popPose();
        }
    }

    public static record BlockPosWithFacing(BlockPos position, Direction facing) {
    }
}