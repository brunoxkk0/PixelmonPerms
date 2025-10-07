package supermemnon.pixelmonperms;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pixelmonmod.pixelmon.api.events.npc.NPCEvent;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.Level;
import supermemnon.pixelmonperms.command.PixelmonPermsCommand;
import supermemnon.pixelmonperms.util.FormattingHelper;
import supermemnon.pixelmonperms.util.PermUtils;
import supermemnon.pixelmonperms.util.PixelmonUtils;

import static supermemnon.pixelmonperms.util.CommandUtils.executeCommandList;
import static supermemnon.pixelmonperms.util.CommandUtils.executeCommandString;


//@Mod.EventBusSubscriber(modid = "pixelmonperms")

public class PixelmonPermsEventHandler {

    @Mod.EventBusSubscriber(modid = "pixelmonperms", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
    public static class ForgeEvents {

        @SubscribeEvent
        public static void registerCommands(RegisterCommandsEvent event) {
            PixelmonPermsCommand.register(event.getDispatcher());
        }

    }

    public static class ModEvents {

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onNPCBattleEvent(NPCEvent.StartBattle event) throws CommandSyntaxException {
            PixelmonPerms.getLOGGER().log(Level.INFO, "NPC BATTLE STARTED");
            if (!NBTHandler.hasRequiredPermission(event.npc)) {
                return;
            }
            String[] perms = NBTHandler.getRequiredPermissions(event.npc);
            if (!PermUtils.hasAllRequiredPermissions(event.player, perms)) {
                PixelmonUtils.customNpcChat(event.npc, (ServerPlayerEntity) event.player, NBTHandler.getCancelMessages(event.npc));
//                event.player.sendMessage(new StringTextComponent(FormattingHelper.formatWithAmpersand(NBTHandler.getCancelMessage(event.npc))), event.player.getUUID());
                if (NBTHandler.hasFailCommand(event.npc)) {
                    boolean commandSuccess = executeCommandList(event.player.getServer(), event.player, NBTHandler.getFailCommands(event.npc));
                }
                event.setCanceled(true);
            }
        }


        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onNPCInteractEvent(NPCEvent.Interact event) throws CommandSyntaxException {
            if (!NBTHandler.hasRequiredPermission(event.npc)) {
                return;
            }
            String[] perms = NBTHandler.getRequiredPermissions(event.npc);
            if (!PermUtils.hasAllRequiredPermissions(event.player, perms)) {
                PixelmonUtils.customNpcChat(event.npc, (ServerPlayerEntity) event.player, NBTHandler.getCancelMessages(event.npc));
//                event.player.sendMessage(new StringTextComponent(FormattingHelper.formatWithAmpersand(NBTHandler.getCancelMessage(event.npc))), event.player.getUUID());
                if (NBTHandler.hasFailCommand(event.npc)) {
                    boolean commandSuccess = executeCommandList(event.player.getServer(), event.player, NBTHandler.getFailCommands(event.npc));
                }
                event.setCanceled(true);
            }
        }
    }
}
