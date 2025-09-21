package com.lovetropics.minigames.client.map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.map.workspace.ClientWorkspaceRegions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Function;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class MapWorkspaceTracer {
	private static final double TRACE_RANGE = 64.0;

	@Nullable
	private static RegionEditOperator edit;

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null && edit != null) {
			edit.update(player);
		}
	}

	@Nullable
	public static RegionTraceTarget trace(Player player) {
		ClientWorkspaceRegions regions = ClientMapWorkspace.INSTANCE.getRegions();
		if (regions.isEmpty()) {
			return null;
		}

		Vec3 origin = player.getEyePosition(1.0F);
		Vec3 target = origin.add(player.getLookAngle().scale(TRACE_RANGE));

		AABB traceScope = new AABB(origin, target).inflate(1.0);

		ClientWorkspaceRegions.Entry closestEntry = null;
		double closestDistance = Double.POSITIVE_INFINITY;
		Vec3 closestPoint = null;
		Direction closestSide = null;

		for (ClientWorkspaceRegions.Entry entry : regions) {
			if (!entry.region.intersects(traceScope)) {
				continue;
			}

			AABB bounds = entry.region.asAabb();
			BlockHitResult traceResult = AABB.clip(ImmutableList.of(bounds), origin, target, BlockPos.ZERO);
			if (traceResult != null) {
				Vec3 intersectPoint = traceResult.getLocation();
				double distance = intersectPoint.distanceTo(origin);
				if (distance < closestDistance) {
					closestEntry = entry;
					closestPoint = intersectPoint;
					closestDistance = distance;
					closestSide = traceResult.getDirection();
				}
			}
		}

		if (closestEntry != null) {
			return new RegionTraceTarget(closestEntry, closestSide, closestPoint, closestDistance);
		} else {
			return null;
		}
	}

	public static boolean select(Player player, @Nullable RegionTraceTarget target, Function<RegionTraceTarget, RegionEditOperator> operatorFactory) {
		if (edit != null) {
			if (edit.select(player, target)) {
				edit = null;
			}
			return true;
		} else if (target != null) {
			edit = operatorFactory.apply(target);
			return true;
		}
		return false;
	}

	public static void stopEditing() {
		edit = null;
	}

	public static Set<ClientWorkspaceRegions.Entry> getSelectedRegions() {
		if (edit != null) {
			return edit.getSelectedRegions();
		} else {
			return ImmutableSet.of();
		}
	}
}
