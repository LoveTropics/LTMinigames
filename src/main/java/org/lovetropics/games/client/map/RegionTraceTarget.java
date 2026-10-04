package org.lovetropics.games.client.map;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.lovetropics.games.common.core.map.workspace.ClientWorkspaceRegions;

public record RegionTraceTarget(ClientWorkspaceRegions.Entry entry, Direction side, Vec3 intersectPoint, double distanceToSide) {
}
