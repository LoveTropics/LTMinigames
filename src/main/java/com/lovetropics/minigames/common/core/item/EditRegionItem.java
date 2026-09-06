package com.lovetropics.minigames.common.core.item;

import com.lovetropics.minigames.client.map.MapWorkspaceTracer;
import com.lovetropics.minigames.client.map.RegionEditOperator;
import com.lovetropics.minigames.client.map.RegionTraceTarget;
import com.lovetropics.minigames.common.core.network.workspace.UpdateWorkspaceRegionMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import org.jspecify.annotations.Nullable;
import java.util.Optional;

public final class EditRegionItem extends Item {
	private static final int USE_INTERVAL = 2;

	private static Mode mode = Mode.RESIZE;
	private static int nextUseTick;

	public EditRegionItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean canDestroyBlock(ItemStack stack, BlockState state, Level level, BlockPos pos, LivingEntity entity) {
		return false;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		return doUse(level, player);
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
		return doUse(context.getLevel(), context.getPlayer());
	}

	private InteractionResult doUse(Level level, @Nullable Player player) {
		if (level.isClientSide() && player != null && isClientPlayer(player)) {
			RegionTraceTarget traceResult = MapWorkspaceTracer.trace(player);

			nextUseTick = player.tickCount + USE_INTERVAL;

			if (traceResult != null && mode == Mode.REMOVE) {
				ClientPacketDistributor.sendToServer(new UpdateWorkspaceRegionMessage(traceResult.entry().id, Optional.empty()));
				return InteractionResult.SUCCESS;
			}

			if (MapWorkspaceTracer.select(player, traceResult, target -> mode.createEdit(target))) {
				return InteractionResult.SUCCESS;
			} else {
				return InteractionResult.CONSUME;
			}
		}

		// DO NOT INTERACT (with blocks)
		return InteractionResult.CONSUME;
	}

	@Override
	public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
		if (entity.level().isClientSide() && entity.tickCount > nextUseTick && isClientPlayer(entity)) {
			mode = mode.getNext();
			nextUseTick = entity.tickCount + USE_INTERVAL;

			MapWorkspaceTracer.stopEditing();

			if (entity instanceof Player player) {
				Component message = Component.literal("Changed mode to: ")
						.append(Component.literal(mode.key).withStyle(mode.color));
				player.sendOverlayMessage(message);
			}
		}

		return false;
	}

	private static boolean isClientPlayer(LivingEntity entity) {
		return Minecraft.getInstance().player == entity;
	}

	enum Mode {
		RESIZE("resize", ChatFormatting.BLUE),
		MOVE("move", ChatFormatting.BLUE),
		// TODO: Provide commands that can operate on selected regions
		SELECT("select", ChatFormatting.BLUE),
		REMOVE("remove", ChatFormatting.RED);

		static final Mode[] MODES = values();

		final String key;
		final ChatFormatting color;

		Mode(String key, ChatFormatting color) {
			this.key = key;
			this.color = color;
		}

		@Nullable RegionEditOperator createEdit(RegionTraceTarget target) {
			return switch (this) {
				case RESIZE -> new RegionEditOperator.Resize(target);
				case MOVE -> new RegionEditOperator.Move(target);
				case SELECT -> new RegionEditOperator.Select(target);
				case REMOVE -> null;
			};
		}

		static Mode byIndex(int index) {
			return MODES[index % MODES.length];
		}

		Mode getNext() {
			return byIndex(ordinal() + 1);
		}
	}
}
