package supermemnon.pixelmonperms.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.api.rules.teamselection.TeamSelectionRegistry;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.TrainerParticipant;
import com.pixelmonmod.pixelmon.entities.npcs.NPCEntity;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Level;
import supermemnon.pixelmonperms.InteractionHandler;
import supermemnon.pixelmonperms.PixelmonPerms;
import supermemnon.pixelmonperms.util.RayTraceHelper;

import java.util.UUID;

public class PixelmonPermsCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> commandStructure = Commands.literal("pixelmonperms").requires(source -> source.hasPermission(2));
        commandStructure = appendSetCommand(commandStructure);
        commandStructure = appendGetCommand(commandStructure);
        commandStructure = appendRemoveCommand(commandStructure);
        commandStructure = appendNPCBattleCommand(commandStructure);
        dispatcher.register(commandStructure);
    }

    private static LiteralArgumentBuilder<CommandSource> appendSetCommand(LiteralArgumentBuilder<CommandSource> command) {
           return command.then(Commands.literal("set")
                .then(Commands.literal("message")
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(context -> runSetCancelMessage(context.getSource(), StringArgumentType.getString(context, "message")))))
                .then(Commands.literal("permission")
                        .then(Commands.argument("permission", PermissionNodeArgument.permissionNode())
                                .executes(context -> runSetPermission(context.getSource(), PermissionNodeArgument.getPermissionNode(context, "permission"))))));
    }

    private static LiteralArgumentBuilder<CommandSource> appendGetCommand(LiteralArgumentBuilder<CommandSource> command) {
        return command.then(Commands.literal("get")
                .then(Commands.literal("message")
                        .executes(context -> runGetCancelMessage(context.getSource())))
                .then(Commands.literal("permission")
                        .executes(context -> runGetPermission(context.getSource()))));
    }

    private static LiteralArgumentBuilder<CommandSource> appendRemoveCommand(LiteralArgumentBuilder<CommandSource> command) {
        return command.then(Commands.literal("remove")
                .then(Commands.literal("message")
                        .executes(context -> runRemoveCancelMessage(context.getSource())))
                .then(Commands.literal("permission")
                        .executes(context -> runRemovePermission(context.getSource()))
                )
        );
    }

    private static LiteralArgumentBuilder<CommandSource> appendNPCBattleCommand(LiteralArgumentBuilder<CommandSource> command) {
        return command.then(Commands.literal("npcbattle")
                .then(Commands.argument("player",  StringArgumentType.word())
                        .then(Commands.argument("uuid", StringArgumentType.word())
                            .executes(context -> runNpcBattle(context.getSource(), StringArgumentType.getString(context, "player"), StringArgumentType.getString(context, "uuid")))
                        )
                )
        );
    }


    private static int runNpcBattle(CommandSource source, String playerName, String npcUUID) throws CommandSyntaxException {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerWorld world = server.overworld().getWorldServer();
        ServerPlayerEntity playerBattler = server.getPlayerList().getPlayerByName(playerName);
        UUID entityUUID = UUID.fromString(npcUUID);
        Entity entity = world.getEntity(entityUUID);
        if (playerBattler == null) {
            source.sendFailure(new StringTextComponent("No player with that name found."));
            return 0;
        }
        else if (entity == null) {
            source.sendFailure(new StringTextComponent("No entity with that UUID found."));
            return 0;
        }
        else if (!(entity instanceof NPCEntity)) {
            source.sendFailure(new StringTextComponent("Entity is not an NPC!"));
            return 0;
        }
        else {
            if (!(entity instanceof NPCTrainer)) {
                source.sendFailure(new StringTextComponent("NPC is not a trainer!"));
                return 0;
            }
            NPCTrainer trainer = (NPCTrainer) entity;
            Pokemon startingPixelmon = StorageProxy.getParty(playerBattler).getSelectedPokemon();
            if (startingPixelmon == null) {
                source.sendFailure(new StringTextComponent("Trainer has no pokemon!!"));
                return 0;
            }
            TeamSelectionRegistry.builder().members(trainer, playerBattler).showRules().showOpponentTeam().closeable(true).battleRules(trainer.battleRules).start();
            PixelmonPerms.getLOGGER().log(Level.INFO, String.format("Started NPC Battle! Between %s and %s", playerName, trainer.getName().getString()));
        }
        return 1;
    }
    private static int runGetPermission(CommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrException();
        Entity lookEntity = RayTraceHelper.getEntityLookingAt(player, 8.0);
//        String perm = StringArgumentType.getString(context.getSource());
        if (lookEntity == null) {
            source.sendFailure(new StringTextComponent("No entity found."));
        }
        else if (lookEntity instanceof NPCEntity) {
            if (!InteractionHandler.hasRequiredPermission(lookEntity)) {
                source.sendFailure(new StringTextComponent("NPC has no required permission!"));
                return 0;
            }
            String perm = InteractionHandler.getRequiredPermission(lookEntity);
            source.sendSuccess(new StringTextComponent(String.format("Required Permission: %s", perm)), true);
        }
        else {
            source.sendFailure(new StringTextComponent("Entity is not NPC!"));
        }
        return 1;
    }

    private static int runSetPermission(CommandSource source, String permission) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrException();
        Entity lookEntity = RayTraceHelper.getEntityLookingAt(player, 8.0);
        if (lookEntity == null) {
            source.sendFailure(new StringTextComponent("No entity found."));
        }
        else if (lookEntity instanceof NPCEntity) {
                InteractionHandler.setRequiredPermission(lookEntity, permission);
            }
        else {
                source.sendFailure(new StringTextComponent("Entity is not NPC!"));
            }
        return 1;
    }

    private static int runGetCancelMessage(CommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrException();
        Entity lookEntity = RayTraceHelper.getEntityLookingAt(player, 8.0);
        if (lookEntity == null) {
            source.sendFailure(new StringTextComponent("No entity found."));
        }
        else if (lookEntity instanceof NPCEntity) {
            if (!InteractionHandler.hasCancelMessage(lookEntity)) {
                source.sendFailure(new StringTextComponent("NPC has no custom cancel message!"));
                return 0;
            }
            String cancelMessage = InteractionHandler.getCancelMessage(lookEntity);
            source.sendSuccess(new StringTextComponent(String.format("Cancel Message: %s", cancelMessage)), true);
        }
        else {
            source.sendFailure(new StringTextComponent("Entity is not NPC!"));
        }
        return 1;
    }

    private static int runSetCancelMessage(CommandSource source, String cancelMessage) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrException();
        Entity lookEntity = RayTraceHelper.getEntityLookingAt(player, 8.0);
        if (lookEntity == null) {
            source.sendFailure(new StringTextComponent("No entity found."));
        }
        else if (lookEntity instanceof NPCEntity) {
            InteractionHandler.setCancelMessage(lookEntity, cancelMessage);
        }
        else {
            source.sendFailure(new StringTextComponent("Entity is not NPC!"));
        }
        return 1;
    }

    private static int runRemovePermission(CommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrException();
        Entity lookEntity = RayTraceHelper.getEntityLookingAt(player, 8.0);
        if (lookEntity == null) {
            source.sendFailure(new StringTextComponent("No entity found."));
        }
        else if (lookEntity instanceof NPCEntity) {
            if (!InteractionHandler.hasRequiredPermission(lookEntity)) {
                source.sendFailure(new StringTextComponent("NPC does not have required permission!!"));
                return 0;
            }
            InteractionHandler.removeRequirePermission(lookEntity);
            source.sendSuccess(new StringTextComponent("Removed NPC's required permission."), true);
        }
        else {
            source.sendFailure(new StringTextComponent("Entity is not NPC!"));
        }
        return 1;
    }

    private static int runRemoveCancelMessage(CommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrException();
        Entity lookEntity = RayTraceHelper.getEntityLookingAt(player, 8.0);
        if (lookEntity == null) {
            source.sendFailure(new StringTextComponent("No entity found."));
        }
        else if (lookEntity instanceof NPCEntity) {
            if (!InteractionHandler.hasCancelMessage(lookEntity)) {
                source.sendFailure(new StringTextComponent("NPC does not have custom cancel message!!"));
                return 0;
            }
            InteractionHandler.removeCancelMessage(lookEntity);
            source.sendSuccess(new StringTextComponent("Removed NPC's custom cancel message."), false);
        }
        else {
            source.sendFailure(new StringTextComponent("Entity is not NPC!"));
        }
        return 1;
    }

}
