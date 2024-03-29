package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public record AddEquipmentAction(List<ItemStack> items, ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet, boolean clear, boolean colorByTeam) implements IGameBehavior {
	public static final MapCodec<AddEquipmentAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MoreCodecs.ITEM_STACK.listOf().optionalFieldOf("items", List.of()).forGetter(AddEquipmentAction::items),
			MoreCodecs.ITEM_STACK.optionalFieldOf("head", ItemStack.EMPTY).forGetter(AddEquipmentAction::head),
			MoreCodecs.ITEM_STACK.optionalFieldOf("chest", ItemStack.EMPTY).forGetter(AddEquipmentAction::chest),
			MoreCodecs.ITEM_STACK.optionalFieldOf("legs", ItemStack.EMPTY).forGetter(AddEquipmentAction::legs),
			MoreCodecs.ITEM_STACK.optionalFieldOf("feet", ItemStack.EMPTY).forGetter(AddEquipmentAction::feet),
			Codec.BOOL.optionalFieldOf("clear", false).forGetter(AddEquipmentAction::clear),
			Codec.BOOL.optionalFieldOf("color_by_team", false).forGetter(AddEquipmentAction::colorByTeam)
	).apply(i, AddEquipmentAction::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		final TeamState teams = game.getInstanceState().getOrNull(TeamState.KEY);
		events.listen(GameActionEvents.APPLY_TO_PLAYER, (context, player) -> {
			if (clear) {
				player.getInventory().clearContent();
			}
			for (final ItemStack item : items) {
				player.getInventory().add(copyAndModify(player, teams, item));
			}
			player.setItemSlot(EquipmentSlot.HEAD, copyAndModify(player, teams, head));
			player.setItemSlot(EquipmentSlot.CHEST, copyAndModify(player, teams, chest));
			player.setItemSlot(EquipmentSlot.LEGS, copyAndModify(player, teams, legs));
			player.setItemSlot(EquipmentSlot.FEET, copyAndModify(player, teams, feet));
			return true;
		});
	}

	private ItemStack copyAndModify(final ServerPlayer player, @Nullable final TeamState teams, final ItemStack item) {
		final ItemStack result = item.copy();
		if (!colorByTeam) {
			return result;
		}
		if (result.getItem() instanceof final DyeableLeatherItem dyeableItem && teams != null) {
			final GameTeamKey teamKey = teams.getTeamForPlayer(player);
			final GameTeam team = teamKey != null ? teams.getTeamByKey(teamKey) : null;
			if (team != null) {
				setColor(dyeableItem, team, result);
			}
		}
		return result;
	}

	private static void setColor(final DyeableLeatherItem dyeableItem, final GameTeam team, final ItemStack stack) {
		final float[] color = team.config().dye().getTextureDiffuseColors();
		final int red = Mth.floor(color[0] * 255.0f);
		final int green = Mth.floor(color[1] * 255.0f);
		final int blue = Mth.floor(color[2] * 255.0f);
		dyeableItem.setColor(stack, FastColor.ARGB32.color(0, red, green, blue));
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.ADD_EQUIPMENT;
	}
}
