package com.lovetropics.minigames.client.map;

import com.google.common.collect.ImmutableSet;
import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.map.workspace.ClientWorkspaceRegions;
import com.lovetropics.minigames.common.core.network.workspace.UpdateWorkspaceRegionMessage;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public interface RegionEditOperator {
	void update(Player player);

	boolean select(Player player, @Nullable RegionTraceTarget target);

	default Set<ClientWorkspaceRegions.Entry> getSelectedRegions() {
		return ImmutableSet.of();
	}

	abstract class EditOne implements RegionEditOperator {
		final RegionTraceTarget target;
		final Set<ClientWorkspaceRegions.Entry> selected;

		protected EditOne(RegionTraceTarget target) {
			this.target = target;
			selected = Collections.singleton(target.entry());
		}

		@Override
		public void update(Player player) {
			target.entry().region = updateEditing(player, target);
		}

		@Override
		public boolean select(Player player, @Nullable RegionTraceTarget target) {
			ClientPacketDistributor.sendToServer(new UpdateWorkspaceRegionMessage(this.target.entry().id, Optional.of(this.target.entry().region)));
			return true;
		}

		@Override
		public Set<ClientWorkspaceRegions.Entry> getSelectedRegions() {
			return selected;
		}

		protected abstract BlockBox updateEditing(Player player, RegionTraceTarget editTarget);
	}

	final class Resize extends EditOne {
		public Resize(RegionTraceTarget target) {
			super(target);
		}

		@Override
		protected BlockBox updateEditing(Player player, RegionTraceTarget editTarget) {
			Vec3 origin = player.getEyePosition(1.0F);

			// TODO: not totally sure how to make this feel natural
			Vec3 grabPoint = origin.add(player.getLookAngle().scale(target.distanceToSide()));
			BlockPos grabPos = BlockPos.containing(grabPoint);

			BlockBox region = editTarget.entry().region;

			return switch (editTarget.side()) {
				case DOWN -> region.withMin(new BlockPos(region.min().getX(), grabPos.getY(), region.min().getZ()));
				case UP -> region.withMax(new BlockPos(region.max().getX(), grabPos.getY(), region.max().getZ()));
				case NORTH -> region.withMin(new BlockPos(region.min().getX(), region.min().getY(), grabPos.getZ()));
				case SOUTH -> region.withMax(new BlockPos(region.max().getX(), region.max().getY(), grabPos.getZ()));
				case WEST -> region.withMin(new BlockPos(grabPos.getX(), region.min().getY(), region.min().getZ()));
				case EAST -> region.withMax(new BlockPos(grabPos.getX(), region.max().getY(), region.max().getZ()));
			};
		}
	}

	final class Move extends EditOne {
		private final Vec3 offset;

		public Move(RegionTraceTarget target) {
			super(target);
			offset = target.intersectPoint().subtract(target.entry().region.center());
		}

		@Override
		protected BlockBox updateEditing(Player player, RegionTraceTarget editTarget) {
			Vec3 origin = player.getEyePosition(1.0F);

			BlockBox region = editTarget.entry().region;

			Vec3 grabPoint = region.center().add(offset);
			Vec3 targetPoint = origin.add(player.getLookAngle().scale(target.distanceToSide()));
			Vec3 offset = targetPoint.subtract(grabPoint);

			return region.offset(Mth.floor(offset.x), Mth.floor(offset.y), Mth.floor(offset.z));
		}
	}

	final class Select implements RegionEditOperator {
		private final Set<ClientWorkspaceRegions.Entry> selected = new ObjectOpenHashSet<>();

		public Select(RegionTraceTarget target) {
			selected.add(target.entry());
		}

		@Override
		public void update(Player player) {
		}

		@Override
		public boolean select(Player player, @Nullable RegionTraceTarget target) {
			if (target != null) {
				selected.add(target.entry());
			}
			return false;
		}

		@Override
		public Set<ClientWorkspaceRegions.Entry> getSelectedRegions() {
			return selected;
		}
	}
}
