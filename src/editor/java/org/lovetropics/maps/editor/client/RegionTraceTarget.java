package org.lovetropics.maps.editor.client;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.lovetropics.maps.editor.workspace.ClientWorkspaceRegions;

public record RegionTraceTarget(ClientWorkspaceRegions.Entry entry, Direction side, Vec3 intersectPoint, double distanceToSide) {
}
