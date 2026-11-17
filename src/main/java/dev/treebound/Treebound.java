package dev.treebound;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class Treebound implements ModInitializer {
    public static final String MOD_ID = "treebound";
    public static Item SEED;

    @Override
    public void onInitialize() {
        TreeConfig.load();
        TreeRace.register();

        SEED = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "seed_of_rebirth"),
                new SeedItem(new Item.Settings().maxCount(16)));
        Registry.register(Registries.ITEM_GROUP, Identifier.of(MOD_ID, "main"),
                FabricItemGroup.builder()
                        .icon(() -> new ItemStack(SEED))
                        .displayName(Text.translatable("itemGroup.treebound.main"))
                        .entries((context, entries) -> entries.add(SEED))
                        .build());

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.isAlive() && !player.isSpectator()) {
                    TreeRace.secondTick(player);
                }
            }
        });

        // /treerace reset  and  /treerace xp <amount>   (operators only, for testing)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("treerace")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("reset").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            TreeRace.resetRace(p);
                            p.sendMessage(Text.literal("You are no longer a tree."), false);
                            return 1;
                        }))
                        .then(CommandManager.literal("xp")
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(0, 1000000))
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                            TreeRace.setXp(p, IntegerArgumentType.getInteger(ctx, "amount"));
                                            TreeRace.status(p);
                                            return 1;
                                        })))));
    }
}
