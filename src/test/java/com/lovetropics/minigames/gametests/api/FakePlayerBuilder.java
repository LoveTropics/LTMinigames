package com.lovetropics.minigames.gametests.api;

import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NullMarked;

import java.util.function.Predicate;

@NullMarked
public class FakePlayerBuilder {
	private final LTGameTestHelper helper;

	public FakePlayerBuilder(LTGameTestHelper helper) {
		this.helper = helper;
	}

	Predicate<Packet<?>> packetPredicate = p -> false;
	Predicate<DamageSource> isInvulnerableTo = s -> true;
	GameType gameMode = GameType.CREATIVE;
	Predicate<Player> canBeHarmedBy = player -> false;
	boolean shouldRegenerateNaturally = true;

	boolean invulnerable;

	public FakePlayerBuilder packetFilter(Predicate<Packet<?>> packetPredicate) {
		this.packetPredicate = packetPredicate;
		return this;
	}

	public FakePlayerBuilder isInvulnerableTo(Predicate<DamageSource> isInvulnerableTo) {
		this.isInvulnerableTo = isInvulnerableTo;
		return this;
	}

	public FakePlayerBuilder isVulnerableTo(Predicate<DamageSource> isVulnerableTo) {
		return isInvulnerableTo(Predicate.not(isVulnerableTo));
	}

	public FakePlayerBuilder gameMode(GameType gameMode) {
		this.gameMode = gameMode;
		return this;
	}

	public FakePlayerBuilder canBeHarmedBy(Predicate<Player> canBeHarmedBy) {
		this.canBeHarmedBy = canBeHarmedBy;
		return this;
	}

	public FakePlayerBuilder shouldRegenerateNaturally(boolean shouldRegenerateNaturally) {
		this.shouldRegenerateNaturally = shouldRegenerateNaturally;
		return this;
	}

	public FakePlayerBuilder invulnerable(boolean invulnerable) {
		this.invulnerable = invulnerable;
		return this;
	}

	public LTFakePlayer build() {
		final var player = new LTFakePlayer(helper.getLevel(), this, pl ->
				pl.setPos(helper.absoluteVec(Vec3.atCenterOf(new Vec3i(0, 1, 0)))), "test-mock-player/" + helper.info.id() + "/" + helper.playerCount.incrementAndGet());
		helper.info.addListener(new GameTestListener() {
			@Override
			public void testStructureLoaded(GameTestInfo pTestInfo) {

			}

			@Override
			public void testPassed(GameTestInfo pTest, GameTestRunner pRunner) {
				player.exitWorld();
			}

			@Override
			public void testFailed(GameTestInfo pTest, GameTestRunner pRunner) {
				player.exitWorld();
			}

			@Override
			public void testAddedForRerun(GameTestInfo pOldTest, GameTestInfo pNewTest, GameTestRunner pRunner) {

			}
		});
		player.level().getServer().getConnection().getConnections().add(player.connection.getConnection());
		player.getAbilities().invulnerable = invulnerable;
		return player;
	}
}
