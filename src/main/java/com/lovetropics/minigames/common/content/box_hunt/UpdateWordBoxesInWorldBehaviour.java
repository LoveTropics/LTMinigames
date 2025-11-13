package com.lovetropics.minigames.common.content.box_hunt;

import com.google.common.collect.Lists;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.commands.data.BlockDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Optional;

public record UpdateWordBoxesInWorldBehaviour(
		Holder<BlockEntityType<?>> blockEntityTypeHolder
) implements IGameBehavior {
	public static final MapCodec<UpdateWordBoxesInWorldBehaviour> CODEC =
			RecordCodecBuilder.mapCodec(inst ->
					inst.group(BuiltInRegistries.BLOCK_ENTITY_TYPE.holderByNameCodec().fieldOf("block_entity_type").forGetter(UpdateWordBoxesInWorldBehaviour::blockEntityTypeHolder)).apply(inst, UpdateWordBoxesInWorldBehaviour::new));
	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		events.listen(GameWorldEvents.CHUNK_LOAD, (chunk) -> {
			PlayerSet hiders = teams.getPlayersForTeam(teams.getTeamByKey("hiders").key());
			chunk.getBlockEntitiesPos().forEach((pos) -> {
				Optional<? extends BlockEntity> blockEntity = chunk.getBlockEntity(pos, blockEntityTypeHolder.value());
				blockEntity.ifPresent(entity -> {
					BlockDataAccessor blockDataAccessor = new BlockDataAccessor(entity, pos);
					CompoundTag originalData = blockDataAccessor.getData();
					CompoundTag components = new CompoundTag();
					Component name;
					if(!hiders.isEmpty()){
						ServerPlayer selectedPlayer = Util.getRandom(Lists.newArrayList(hiders), game.random());
						name = selectedPlayer.getTabListDisplayName();
						if(name == null){
							name = selectedPlayer.getName();
						}
					} else {
						name = Component.literal("BOX");
					}
					components.put("minecraft:custom_name", ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, name).getOrThrow());
					CompoundTag newData = new CompoundTag();
					newData.put("components", components);
					CompoundTag mergedData = originalData.copy().merge(newData);
					blockDataAccessor.setData(mergedData);
				});
			});
		});
	}
}
