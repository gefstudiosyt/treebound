package dev.treebound;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** The tree race: saved as growth points on the player (-1 = not a tree). Kept after death. */
public final class TreeRace {
    public static AttachmentType<Integer> XP;

    private static final Identifier HEALTH_ID = Identifier.of(Treebound.MOD_ID, "tree_health");
    private static final Identifier ARMOR_ID = Identifier.of(Treebound.MOD_ID, "tree_bark");
    private static final Identifier SPEED_ID = Identifier.of(Treebound.MOD_ID, "tree_slow");
    private static final String[] STAGE_NAMES = {"Sapling", "Young Tree", "Great Tree", "World Tree"};

    private TreeRace() {}

    public static void register() {
        XP = AttachmentRegistry.create(Identifier.of(Treebound.MOD_ID, "growth"), builder -> builder
                .persistent(Codec.INT)
                .copyOnDeath()
                .initializer(() -> -1));
    }

    public static int getXp(ServerPlayerEntity player) {
        Integer v = player.getAttachedOrElse(XP, -1);
        return v == null ? -1 : v;
    }

    public static void setXp(ServerPlayerEntity player, int xp) {
        player.setAttached(XP, xp);
    }

    public static boolean isTree(ServerPlayerEntity player) {
        return getXp(player) >= 0;
    }

    public static int stageOf(int xp) {
        TreeConfig c = TreeConfig.get();
        if (xp >= c.stage3Xp) return 3;
        if (xp >= c.stage2Xp) return 2;
        if (xp >= c.stage1Xp) return 1;
        return 0;
    }

    public static String stageName(int stage) {
        return STAGE_NAMES[Math.max(0, Math.min(3, stage))];
    }

    /** Points needed for the next stage, or -1 at the top. */
    public static int nextStageXp(int stage) {
        TreeConfig c = TreeConfig.get();
        return switch (stage) {
            case 0 -> c.stage1Xp;
            case 1 -> c.stage2Xp;
            case 2 -> c.stage3Xp;
            default -> -1;
        };
    }

    public static void becomeTree(ServerPlayerEntity player) {
        setXp(player, 0);
        applyStage(player, 0);
        player.sendMessage(Text.literal("You died... and woke up as a tree. You are a Sapling.").formatted(Formatting.GREEN), false);
        player.sendMessage(Text.literal("Stand in the sun, or sneak still on dirt, to grow. Right-click the seed to see your growth.").formatted(Formatting.GRAY), false);
    }

    public static void resetRace(ServerPlayerEntity player) {
        setXp(player, -1);
        clearModifiers(player);
    }

    public static void status(ServerPlayerEntity player) {
        int xp = getXp(player);
        if (xp < 0) {
            player.sendMessage(Text.literal("You are not a tree."), true);
            return;
        }
        int stage = stageOf(xp);
        int next = nextStageXp(stage);
        String text = stageName(stage) + " - growth " + xp + (next > 0 ? " / " + next : " (fully grown)");
        player.sendMessage(Text.literal(text).formatted(Formatting.GREEN), true);
    }

    private static void clearModifiers(ServerPlayerEntity player) {
        remove(player, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_ID);
        remove(player, EntityAttributes.GENERIC_ARMOR, ARMOR_ID);
        remove(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_ID);
    }

    private static void remove(ServerPlayerEntity p, RegistryEntry<EntityAttribute> attr, Identifier id) {
        EntityAttributeInstance inst = p.getAttributeInstance(attr);
        if (inst != null) inst.removeModifier(id);
    }

    private static void set(ServerPlayerEntity p, RegistryEntry<EntityAttribute> attr, Identifier id, double value,
                            EntityAttributeModifier.Operation op) {
        EntityAttributeInstance inst = p.getAttributeInstance(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        inst.addTemporaryModifier(new EntityAttributeModifier(id, value, op));
    }

    /** Sapling: 16 health, 0 bark. Each stage: +8 health, +3 armor. Trees are a bit slow. */
    private static void applyStage(ServerPlayerEntity p, int stage) {
        set(p, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_ID, -4.0 + 8.0 * stage, EntityAttributeModifier.Operation.ADD_VALUE);
        set(p, EntityAttributes.GENERIC_ARMOR, ARMOR_ID, 3.0 * stage, EntityAttributeModifier.Operation.ADD_VALUE);
        set(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_ID, -0.1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    /** Called once per second for every player. */
    public static void secondTick(ServerPlayerEntity player) {
        int xp = getXp(player);
        if (xp < 0) {
            clearModifiers(player);
            return;
        }
        ServerWorld world = player.getServerWorld();
        TreeConfig cfg = TreeConfig.get();
        int oldStage = stageOf(xp);

        boolean sun = world.getDimension().hasSkyLight()
                && world.isSkyVisible(player.getBlockPos().up())
                && (world.isDay() || world.isRaining());
        boolean rooted = player.isSneaking()
                && player.getVelocity().horizontalLengthSquared() < 0.0004
                && player.isOnGround()
                && (world.getBlockState(player.getBlockPos().down()).isIn(BlockTags.DIRT)
                    || world.getBlockState(player.getBlockPos().down()).isOf(net.minecraft.block.Blocks.GRASS_BLOCK));

        int gain = 0;
        if (sun) gain += cfg.xpPerSecondInSun;
        if (rooted) gain += cfg.xpPerSecondRooted;
        if (gain > 0) xp += gain;

        // Photosynthesis: sunlight feeds and slowly heals a tree.
        if (sun) {
            if (world.getTime() % 80 < 20 && player.getHungerManager().getFoodLevel() < 20) {
                player.getHungerManager().add(1, 0.5F);
            }
            if (oldStage >= 1) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 0, false, false, true));
            }
        }

        // Rooted: tough and healing, with leaves floating around.
        if (rooted) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 1, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 1, false, false, true));
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.5, 0.6, 0.5, 0.0);
        }

        // Trees burn: fire hurts a little extra.
        if (player.isOnFire() && !player.isCreative()) {
            player.damage(world.getDamageSources().onFire(), 1.0F);
        }

        // Great Tree and above: the land around you grows.
        if (oldStage >= 2 && world.getTime() % 100 < 20) {
            for (int i = 0; i < 4; i++) {
                BlockPos pos = player.getBlockPos().add(world.random.nextInt(9) - 4, world.random.nextInt(3) - 1, world.random.nextInt(9) - 4);
                BlockState state = world.getBlockState(pos);
                if (state.getBlock() instanceof Fertilizable) {
                    BoneMealItem.useOnFertilizable(new ItemStack(Items.BONE_MEAL), world, pos);
                    world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.0);
                }
            }
        }

        if (xp != getXp(player)) {
            setXp(player, xp);
        }
        int newStage = stageOf(xp);
        applyStage(player, newStage);
        if (newStage > oldStage) {
            player.sendMessage(Text.literal("You have grown into a " + stageName(newStage) + "!").formatted(Formatting.GREEN, Formatting.BOLD), false);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 0.8F);
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.8, 1.0, 0.8, 0.0);
        }
    }
}
