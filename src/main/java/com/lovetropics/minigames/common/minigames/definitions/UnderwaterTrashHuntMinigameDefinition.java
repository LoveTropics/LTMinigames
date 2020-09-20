package com.lovetropics.minigames.common.minigames.definitions;

import com.lovetropics.minigames.client.data.TropicraftLangKeys;
import com.lovetropics.minigames.common.Util;
import com.lovetropics.minigames.common.minigames.IMinigameDefinition;
import com.lovetropics.minigames.common.minigames.IMinigameInstance;
import com.lovetropics.minigames.common.minigames.MinigameManager;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.registries.ObjectHolder;

/**
 * Definition implementation for Signature Run minigame.
 */
public class UnderwaterTrashHuntMinigameDefinition implements IMinigameDefinition {
    
    private DimensionType tropicsDim;
    
    private ResourceLocation id = Util.resource("underwater_trash_hunt");
    private String displayName = TropicraftLangKeys.MINIGAME_UNDERWATER_TRASH_HUNT;

    private BlockPos spectatorPos = new BlockPos(5916, 108, 7000);

    private BlockPos[] playerPositions = new BlockPos[] {
            new BlockPos(5982, 126, 6970), // player 1
            new BlockPos(5982, 126, 6970), // player 2
            new BlockPos(5982, 126, 6970), // player 3
            new BlockPos(5982, 126, 6970), // player 4
            new BlockPos(5982, 126, 6970), // player 5
            new BlockPos(5982, 126, 6970), // player 6
            new BlockPos(5982, 126, 6970), // player 7
            new BlockPos(5982, 126, 6970), // player 8
            new BlockPos(5982, 126, 6970), // player 9
            new BlockPos(5982, 126, 6970), // player 10
            new BlockPos(5982, 126, 6970), // player 11
            new BlockPos(5982, 126, 6970), // player 12
            new BlockPos(5982, 126, 6970), // player 13
            new BlockPos(5982, 126, 6970), // player 14
            new BlockPos(5982, 126, 6970), // player 15
            new BlockPos(5982, 126, 6970), // player 16
    };

    private int maximumPlayerCount = 16;
    private int minimumPlayerCount = 8;

    private long minigameTime = 0;

    private MinecraftServer server;

    public UnderwaterTrashHuntMinigameDefinition(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public ResourceLocation getID() {
        return this.id;
    }

    @Override
    public String getUnlocalizedName() {
        return this.displayName;
    }

    @Override
    public DimensionType getDimension() {
        DimensionType ret = tropicsDim;
        if (ret == null) {
            tropicsDim = ret = DimensionType.byName(new ResourceLocation("tropicraft", "tropics"));
            if (ret == null) {
                throw new IllegalStateException("Could not find tropics dimension");
            }
        }
        return ret;
    }

    @Override
    public GameType getParticipantGameType() {
        return GameType.SURVIVAL;
    }

    @Override
    public GameType getSpectatorGameType() {
        return GameType.SPECTATOR;
    }

    @Override
    public BlockPos getSpectatorPosition() {
        return this.spectatorPos;
    }

    @Override
    public BlockPos getPlayerRespawnPosition(IMinigameInstance instance) {
        return this.spectatorPos;
    }

    @Override
    public BlockPos[] getParticipantPositions() {
        return this.playerPositions;
    }

    @Override
    public int getMinimumParticipantCount() {
        return this.minimumPlayerCount;
    }

    @Override
    public int getMaximumParticipantCount() {
        return this.maximumPlayerCount;
    }

    @Override
    public void worldUpdate(World world, IMinigameInstance instance) {
        if (world.getDimension().getType() == getDimension()) {
            minigameTime++;

            if (minigameTime >= 4800) {
                MinigameManager.getInstance().finishCurrentMinigame();
            }
        }
    }

    @Override
    public void onPlayerDeath(ServerPlayerEntity player, IMinigameInstance instance) {
        if (!instance.getSpectators().contains(player.getUniqueID())) {
            instance.removeParticipant(player);
            instance.addSpectator(player);

            player.setGameType(GameType.SPECTATOR);
        }

        if (instance.getParticipants().size() <= 0) {
            MinigameManager.getInstance().finishCurrentMinigame();
        }

        player.inventory.dropAllItems();
    }

    @Override
    public void onFinish(CommandSource commandSource, IMinigameInstance instance) {
        Commands commands = this.server.getCommandManager();

        try {
            commands.getDispatcher().execute("function ocean_reset:reset", commandSource);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart(CommandSource commandSource, IMinigameInstance instance) {
        minigameTime = 0;

        Commands commands = this.server.getCommandManager();

        try {
            commands.getDispatcher().execute("function trash_cleanup:reset_game", commandSource);
            commands.getDispatcher().execute("function trash_cleanup:start_game", commandSource);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
    }
}
