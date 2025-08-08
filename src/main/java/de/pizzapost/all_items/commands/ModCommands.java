package de.pizzapost.all_items.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import de.pizzapost.all_items.AllItems;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ModCommands {
    public static void initializeCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("start").requires(source -> source.hasPermissionLevel(4)).executes(context -> {
                MinecraftServer server = context.getSource().getServer();
                AllItems.startGame(server, false);
                return 1;
            }));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("set_timer").requires(source -> source.hasPermissionLevel(4)).then(argument("days", IntegerArgumentType.integer(0)).then(argument("hours", IntegerArgumentType.integer(0, 23)).then(argument("minutes", IntegerArgumentType.integer(0, 59)).then(argument("seconds", IntegerArgumentType.integer(0, 59)).executes(context -> {
                ServerCommandSource source = context.getSource();
                int days = IntegerArgumentType.getInteger(context, "days");
                int hours = IntegerArgumentType.getInteger(context, "hours");
                int minutes = IntegerArgumentType.getInteger(context, "minutes");
                int seconds = IntegerArgumentType.getInteger(context, "seconds");
                AllItems.setGameTimer(source.getServer(), days, hours, minutes, seconds);
                return 1;
            }))))));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("shuffle_items").requires(source -> source.hasPermissionLevel(4)).executes(context -> {
            AllItems.shuffleItems();
            return 1;
        })));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("skip_item").requires(source -> source.hasPermissionLevel(4)).executes(context -> {
            Text skippedItem=AllItems.skipItem();
            context.getSource().sendFeedback(() -> Text.translatable("command.all_items.skipped", skippedItem), false);
            return 1;
        })));
    }

    public static void registerModCommands() {
        AllItems.LOGGER.info("Registering Mod Commands for " + AllItems.MOD_ID);
        initializeCommands();
    }
}
