package dev.treebound;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

/**
 * Seed of Rebirth. Not a tree yet: right-click to be reincarnated as one (the seed is used up).
 * Already a tree: right-click = Root Snare (nearby monsters are tangled and slowed, Young Tree and up),
 * sneak + right-click = show your growth. The seed is kept.
 */
public class SeedItem extends Item {
    public SeedItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!(world instanceof ServerWorld sw) || !(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.success(stack);
        }

        if (!TreeRace.isTree(player)) {
            TreeRace.becomeTree(player);
            sw.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AZALEA_LEAVES_PLACE, SoundCategory.PLAYERS, 1.0F, 0.7F);
            sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.6, 1.0, 0.6, 0.0);
            if (!player.isCreative()) stack.decrement(1);
            return TypedActionResult.success(stack);
        }

        if (player.isSneaking()) {
            TreeRace.status(player);
            return TypedActionResult.success(stack);
        }

        int stage = TreeRace.stageOf(TreeRace.getXp(player));
        if (stage < 1) {
            player.sendMessage(Text.literal("Root Snare unlocks at Young Tree."), true);
            return TypedActionResult.fail(stack);
        }
        TreeConfig cfg = TreeConfig.get();
        Box box = player.getBoundingBox().expand(Math.max(1.0, cfg.snareRadius));
        int count = 0;
        for (LivingEntity e : sw.getEntitiesByClass(LivingEntity.class, box, x -> x.isAlive() && x instanceof Monster)) {
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 4, false, false));
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1, false, false));
            sw.spawnParticles(ParticleTypes.COMPOSTER, e.getX(), e.getY() + 0.5, e.getZ(), 15, 0.4, 0.5, 0.4, 0.0);
            count++;
        }
        sw.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AZALEA_LEAVES_BREAK, SoundCategory.PLAYERS, 1.0F, 0.6F);
        player.sendMessage(Text.literal("Root Snare! " + count + " monsters tangled."), true);
        int cd = Math.max(0, cfg.snareCooldownSeconds) * 20;
        if (cd > 0) player.getItemCooldownManager().set(this, cd);
        return TypedActionResult.success(stack);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Right-click: be reincarnated as a tree").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("As a tree: right-click = Root Snare, sneak = growth").formatted(Formatting.DARK_GREEN));
    }
}
