package com.lovetropics.minigames.client.game.handler.spectate;

import com.lovetropics.minigames.common.core.network.SpectatePlayerAndTeleportMessage;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.UUID;
import java.util.function.BooleanSupplier;

interface SpectatingState {
	FreeCamera FREE_CAMERA = new FreeCamera();

	StateApplicator apply(Minecraft client, SpectatingSession session);

	default SpectatingState tick(Minecraft client, SpectatingSession session, LocalPlayer player) {
		return this;
	}

	default void renderTick(Minecraft client, SpectatingSession session, LocalPlayer player) {
	}

	default void applyToCamera(Minecraft client, SpectatingSession session, LocalPlayer player, Camera camera, float partialTicks, ViewportEvent.ComputeCameraAngles event) {
	}

	default void applyCameraDistance(Minecraft client, SpectatingSession session, LocalPlayer player, Camera camera, float partialTicks, CalculateDetachedCameraDistanceEvent event) {
	}

	boolean allowsZoom();

	record FreeCamera() implements SpectatingState {
		@Override
		public StateApplicator apply(Minecraft client, SpectatingSession session) {
			client.options.smoothCamera = false;
			client.options.setCameraType(CameraType.FIRST_PERSON);

			return new StateApplicator(
					() -> ClientPacketDistributor.sendToServer(new SpectatePlayerAndTeleportMessage(client.player.getUUID())),
					() -> client.getCameraEntity() == client.player
			);
		}

		@Override
		public SpectatingState tick(Minecraft client, SpectatingSession session, LocalPlayer player) {
			// force player to maximum flying speed
			player.getAbilities().setFlyingSpeed(0.2F);

			if (client.getCameraEntity() != player) {
				return new SelectedPlayer(client.getCameraEntity().getUUID());
			}

			return this;
		}

		@Override
		public boolean allowsZoom() {
			return false;
		}
	}

	record SelectedPlayer(UUID spectatedId) implements SpectatingState {
		@Override
		public StateApplicator apply(Minecraft client, SpectatingSession session) {
			return new StateApplicator(
					() -> ClientPacketDistributor.sendToServer(new SpectatePlayerAndTeleportMessage(spectatedId)),
					() -> spectatedId.equals(client.getCameraEntity().getUUID())
			);
		}

		@Override
		public SpectatingState tick(Minecraft client, SpectatingSession session, LocalPlayer player) {
			if (!spectatedId.equals(client.getCameraEntity().getUUID())) {
				return SpectatingState.FREE_CAMERA;
			}

			if (player.level().getGameTime() % (SharedConstants.TICKS_PER_SECOND / 2) == 0) {
				// we need to send position updates to the server or we won't properly track chunks
				Player spectatedPlayer = player.level().getPlayerByUUID(spectatedId);
				if (spectatedPlayer != null) {
					player.setPos(spectatedPlayer.getX(), spectatedPlayer.getY(), spectatedPlayer.getZ());
					// If the entity got untracked, and tracked again, reset it
					if (spectatedPlayer != client.getCameraEntity()) {
						client.setCameraEntity(spectatedPlayer);
					}
				}
			}

			return this;
		}

		@Override
		public void renderTick(Minecraft client, SpectatingSession session, LocalPlayer player) {
			Entity focusEntity = client.getCameraEntity();
			focusEntity = focusEntity != null ? focusEntity : player;

			boolean firstPerson = session.zoom <= 1e-2;

			if (!firstPerson) {
				client.options.smoothCamera = true;
				client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			} else {
				client.options.smoothCamera = false;
				client.options.setCameraType(CameraType.FIRST_PERSON);

				player.setYRot(focusEntity.getYRot());
				player.setXRot(focusEntity.getXRot());
			}
		}

		@Override
		public void applyToCamera(Minecraft client, SpectatingSession session, LocalPlayer player, Camera camera, float partialTicks, ViewportEvent.ComputeCameraAngles event) {
			double zoom = session.getZoom(partialTicks);
			boolean firstPerson = zoom <= 1e-2;
			if (!firstPerson) {
				event.setYaw(player.getViewYRot(partialTicks));
				event.setPitch(player.getViewXRot(partialTicks));
			}
		}

		@Override
		public void applyCameraDistance(Minecraft client, SpectatingSession session, LocalPlayer player, Camera camera, float partialTicks, CalculateDetachedCameraDistanceEvent event) {
			double zoom = session.getZoom(partialTicks);
			boolean firstPerson = zoom <= 1e-2;
			if (!firstPerson) {
				float distance = (float) (zoom * ClientSpectatingManager.MAX_CHASE_DISTANCE);
				event.setDistance(distance);
			}
		}

		@Override
		public boolean allowsZoom() {
			return true;
		}
	}

	final class StateApplicator {
		private static final long APPLY_INTERVAL_TICKS = SharedConstants.TICKS_PER_SECOND;

		final Runnable apply;
		final BooleanSupplier condition;

		long lastApplyTime;

		StateApplicator(Runnable apply, BooleanSupplier condition) {
			this.apply = apply;
			this.condition = condition;
		}

		boolean tryApply(Minecraft client) {
			long time = client.level.getGameTime();
			if (time - lastApplyTime >= APPLY_INTERVAL_TICKS) {
				apply.run();
				lastApplyTime = time;
			}

			return condition.getAsBoolean();
		}

		boolean isApplied() {
			return condition.getAsBoolean();
		}
	}
}
