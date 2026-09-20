package org.lovetropics.games.client.map;

import org.lovetropics.games.common.core.map.workspace.ClientWorkspaceRegions;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public record RegionTraceTarget(ClientWorkspaceRegions.Entry entry, Direction side, Vec3 intersectPoint, double distanceToSide) {
}
