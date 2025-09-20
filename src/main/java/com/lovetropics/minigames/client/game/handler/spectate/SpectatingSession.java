package com.lovetropics.minigames.client.game.handler.spectate;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

class SpectatingSession {
	List<UUID> players;

	final SpectatingUi ui;

	SpectatingState state;
	@Nullable
	SpectatingState.StateApplicator stateApplicator;

	double targetZoom = 1.0;
	double zoom = 1.0;
	double prevZoom = 1.0;

	SpectatingSession(List<UUID> players) {
		this.players = players;
		ui = new SpectatingUi(this);
		applyState(SpectatingState.FREE_CAMERA);
	}

	void tick() {
		prevZoom = zoom;
		zoom = targetZoom;

		if (stateApplicator != null) {
			if (stateApplicator.tryApply(Minecraft.getInstance())) {
				stateApplicator = null;
			} else {
				return;
			}
		}

		SpectatingState newState = state.tick(Minecraft.getInstance(), this, Minecraft.getInstance().player);
		if (!newState.equals(state)) {
			applyState(newState);
			ui.updateState(newState);
		}
	}

	double getZoom(float partialTicks) {
		return Mth.lerp(partialTicks, prevZoom, zoom);
	}

	void renderTick() {
		Minecraft minecraft = Minecraft.getInstance();
		state.renderTick(minecraft, this, minecraft.player);
	}

	void applyToCamera(Camera camera, float partialTicks, ViewportEvent.ComputeCameraAngles event) {
		Minecraft minecraft = Minecraft.getInstance();
		state.applyToCamera(minecraft, this, minecraft.player, camera, partialTicks, event);
	}

	void applyCameraDistance(Camera camera, float partialTicks, CalculateDetachedCameraDistanceEvent event) {
		Minecraft minecraft = Minecraft.getInstance();
		state.applyCameraDistance(minecraft, this, minecraft.player, camera, partialTicks, event);
	}

	void applyState(SpectatingState state) {
		if (state.equals(this.state)) {
			return;
		}
		this.state = state;
		SpectatingState.StateApplicator applicator = state.apply(Minecraft.getInstance(), this);
		stateApplicator = applicator.isApplied() ? null : applicator;
	}

	void updatePlayers(List<UUID> players) {
		this.players = players;
		ui.updatePlayers(players);
	}

	void close() {
		applyState(SpectatingState.FREE_CAMERA);
	}
}
