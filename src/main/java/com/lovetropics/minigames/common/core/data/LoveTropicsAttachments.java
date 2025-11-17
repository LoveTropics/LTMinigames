package com.lovetropics.minigames.common.core.data;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.integration.state.MinecrafterDonor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class LoveTropicsAttachments {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LoveTropics.ID);
	public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> HIGHLIGHT_COLOR = ATTACHMENT_TYPES.register(
			"highlight_color",
			() -> AttachmentType.builder(() -> 0)
					.sync(ByteBufCodecs.INT)
					.build()
	);

	public static final DeferredHolder<AttachmentType<?>, AttachmentType<MinecrafterDonor>> DONOR = ATTACHMENT_TYPES.register(
			"donor", () -> AttachmentType.builder(MinecrafterDonor::empty).serialize(MinecrafterDonor.MAP_CODEC).build()
	);
}
