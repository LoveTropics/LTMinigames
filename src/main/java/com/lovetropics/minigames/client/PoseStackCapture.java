package com.lovetropics.minigames.client;

import com.lovetropics.minigames.mixin.client.PoseStackAccessor;
import com.mojang.blaze3d.vertex.PoseStack;

public class PoseStackCapture {
    private PoseStackCapture() {
    }

    public static int get(PoseStack poseStack) {
        return ((PoseStackAccessor) poseStack).getLastIndex();
    }

    public static void restore(PoseStack poseStack, int lastIndex) {
		((PoseStackAccessor) poseStack).setLastIndex(lastIndex);
    }
}
