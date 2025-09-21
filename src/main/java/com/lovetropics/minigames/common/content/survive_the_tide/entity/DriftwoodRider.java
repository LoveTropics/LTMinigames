package com.lovetropics.minigames.common.content.survive_the_tide.entity;

import com.lovetropics.minigames.LoveTropics;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nullable;
import java.util.function.Supplier;

@EventBusSubscriber(modid = LoveTropics.ID)
public final class DriftwoodRider {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LoveTropics.ID);

	public static final Supplier<AttachmentType<DriftwoodRider>> ATTACHMENT = ATTACHMENT_TYPES.register(
			"driftwood_rider", () -> AttachmentType.builder(holder -> new DriftwoodRider((Player) holder)).build()
	);

	private final Player player;
	@Nullable
	private DriftwoodEntity ridingDriftwood;
	private int ridingTime;

	DriftwoodRider(Player player) {
		this.player = player;
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		event.getEntity().getData(ATTACHMENT).tick();
	}

	private void tick() {
		DriftwoodEntity ridingDriftwood = this.ridingDriftwood;
		if (ridingTime <= 0 || ridingDriftwood == null) {
			return;
		}

		if (--ridingTime <= 0) {
			this.ridingDriftwood = null;
		}

		if (player.isLocalPlayer()) {
			double deltaX = ridingDriftwood.getX() - ridingDriftwood.xo;
			double deltaZ = ridingDriftwood.getZ() - ridingDriftwood.zo;
			double deltaY = Math.max(ridingDriftwood.getY() - ridingDriftwood.yo, 0.0);
			move(deltaX, deltaY, deltaZ);
		}
	}

	private void move(double deltaX, double deltaY, double deltaZ) {
		if (deltaX == 0.0 && deltaY == 0.0 && deltaZ == 0.0) {
			return;
		}

		Vec3 motion = player.getDeltaMovement();
		boolean onGround = player.onGround();

		player.move(MoverType.SELF, new Vec3(deltaX, deltaY, deltaZ));

		player.setDeltaMovement(motion);
		player.setOnGround(onGround);
	}

	public void setRiding(DriftwoodEntity driftwood) {
		ridingDriftwood = driftwood;
		ridingTime = 10;
	}

	@Nullable
	public DriftwoodEntity getRiding() {
		return ridingDriftwood;
	}
}
