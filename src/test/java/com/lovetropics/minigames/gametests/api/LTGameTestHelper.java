package com.lovetropics.minigames.gametests.api;

import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import com.lovetropics.minigames.common.core.game.impl.GameLobbyManager;
import com.lovetropics.minigames.common.core.game.lobby.LobbyControls;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.mixin.gametest.GameTestHelperAccess;
import com.lovetropics.minigames.mixin.gametest.GameTestInfoAccess;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LTGameTestHelper extends GameTestHelper {
	private final GameTestHelper delegate;
	final GameTestInfo info;
	final AtomicInteger playerCount = new AtomicInteger();

	public LTGameTestHelper(GameTestHelper helper) {
		super(null);
		delegate = helper;
		info = ((GameTestHelperAccess) helper).getTestInfo();
	}

	@Override
	public ServerLevel getLevel() {
		return delegate.getLevel();
	}

	@Override
	public BlockState getBlockState(BlockPos pPos) {
		return delegate.getBlockState(pPos);
	}

	@Override
	public void killAllEntities() {
		delegate.killAllEntities();
	}

	@Override
	public void killAllEntitiesOfClass(Class pEntityClass) {
		delegate.killAllEntitiesOfClass(pEntityClass);
	}

	@Override
	public ItemEntity spawnItem(Item pItem, float pX, float pY, float pZ) {
		return delegate.spawnItem(pItem, pX, pY, pZ);
	}

	@Override
	public ItemEntity spawnItem(Item pItem, BlockPos pPos) {
		return delegate.spawnItem(pItem, pPos);
	}

	@Override
	public <E extends Entity> E spawn(EntityType<E> pType, BlockPos pPos) {
		return delegate.spawn(pType, pPos);
	}

	@Override
	public <E extends Entity> E spawn(EntityType<E> pType, Vec3 pPos) {
		return delegate.spawn(pType, pPos);
	}

	@Override
	public <E extends Entity> E spawn(EntityType<E> pType, int pX, int pY, int pZ) {
		return delegate.spawn(pType, pX, pY, pZ);
	}

	@Override
	public <E extends Entity> E spawn(EntityType<E> pType, float pX, float pY, float pZ) {
		return delegate.spawn(pType, pX, pY, pZ);
	}

	@Override
	public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> pType, BlockPos pPos) {
		return delegate.spawnWithNoFreeWill(pType, pPos);
	}

	@Override
	public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> pType, int pX, int pY, int pZ) {
		return delegate.spawnWithNoFreeWill(pType, pX, pY, pZ);
	}

	@Override
	public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> pType, Vec3 pPos) {
		return delegate.spawnWithNoFreeWill(pType, pPos);
	}

	@Override
	public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> pType, float pX, float pY, float pZ) {
		return delegate.spawnWithNoFreeWill(pType, pX, pY, pZ);
	}

	@Override
	public GameTestSequence walkTo(Mob pMob, BlockPos pPos, float pSpeed) {
		return delegate.walkTo(pMob, pPos, pSpeed);
	}

	@Override
	public void pressButton(int pX, int pY, int pZ) {
		delegate.pressButton(pX, pY, pZ);
	}

	@Override
	public void pressButton(BlockPos pPos) {
		delegate.pressButton(pPos);
	}

	@Override
	public void useBlock(BlockPos pPos) {
		delegate.useBlock(pPos);
	}

	@Override
	public void useBlock(BlockPos pPos, Player pPlayer) {
		delegate.useBlock(pPos, pPlayer);
	}

	@Override
	public void useBlock(BlockPos pPos, Player pPlayer, BlockHitResult pResult) {
		delegate.useBlock(pPos, pPlayer, pResult);
	}

	@Override
	public LivingEntity makeAboutToDrown(LivingEntity pEntity) {
		return delegate.makeAboutToDrown(pEntity);
	}

	@Override
	public Player makeMockPlayer(GameType gameType) {
		return delegate.makeMockPlayer(gameType);
	}

	@Override
	public LivingEntity withLowHealth(LivingEntity pEntity) {
		return delegate.withLowHealth(pEntity);
	}

	@Override
	public void pullLever(int pX, int pY, int pZ) {
		delegate.pullLever(pX, pY, pZ);
	}

	@Override
	public void pullLever(BlockPos pPos) {
		delegate.pullLever(pPos);
	}

	@Override
	public void pulseRedstone(BlockPos pPos, long pDelay) {
		delegate.pulseRedstone(pPos, pDelay);
	}

	@Override
	public void destroyBlock(BlockPos pPos) {
		delegate.destroyBlock(pPos);
	}

	@Override
	public void setBlock(int pX, int pY, int pZ, Block pBlock) {
		delegate.setBlock(pX, pY, pZ, pBlock);
	}

	@Override
	public void setBlock(int pX, int pY, int pZ, BlockState pState) {
		delegate.setBlock(pX, pY, pZ, pState);
	}

	@Override
	public void setBlock(BlockPos pPos, Block pBlock) {
		delegate.setBlock(pPos, pBlock);
	}

	@Override
	public void setBlock(BlockPos pPos, BlockState pState) {
		delegate.setBlock(pPos, pState);
	}

	@Override
	public void setNight() {
		delegate.setNight();
	}

	@Override
	public void setDayTime(int pTime) {
		delegate.setDayTime(pTime);
	}

	@Override
	public void assertBlockPresent(Block pBlock, int pX, int pY, int pZ) {
		delegate.assertBlockPresent(pBlock, pX, pY, pZ);
	}

	@Override
	public void assertBlockPresent(Block pBlock, BlockPos pPos) {
		delegate.assertBlockPresent(pBlock, pPos);
	}

	@Override
	public void assertBlockNotPresent(Block pBlock, int pX, int pY, int pZ) {
		delegate.assertBlockNotPresent(pBlock, pX, pY, pZ);
	}

	@Override
	public void assertBlockNotPresent(Block pBlock, BlockPos pPos) {
		delegate.assertBlockNotPresent(pBlock, pPos);
	}

	@Override
	public void succeedWhenBlockPresent(Block pBlock, int pX, int pY, int pZ) {
		delegate.succeedWhenBlockPresent(pBlock, pX, pY, pZ);
	}

	@Override
	public void succeedWhenBlockPresent(Block pBlock, BlockPos pPos) {
		delegate.succeedWhenBlockPresent(pBlock, pPos);
	}

	@Override
	public <T extends Comparable<T>> void assertBlockProperty(BlockPos pPos, Property<T> pProperty, T pValue) {
		delegate.assertBlockProperty(pPos, pProperty, pValue);
	}

	@Override
	public void assertEntityPresent(EntityType<?> pType) {
		delegate.assertEntityPresent(pType);
	}

	@Override
	public void assertEntityPresent(EntityType<?> pType, int pX, int pY, int pZ) {
		delegate.assertEntityPresent(pType, pX, pY, pZ);
	}

	@Override
	public void assertEntityPresent(EntityType<?> pType, BlockPos pPos) {
		delegate.assertEntityPresent(pType, pPos);
	}

	@Override
	public GameTestAssertException assertionException(Component message) {
		return delegate.assertionException(message);
	}

	@Override
	public GameTestAssertException assertionException(String messageKey, Object... args) {
		return delegate.assertionException(messageKey, args);
	}

	@Override
	public GameTestAssertPosException assertionException(BlockPos pos, Component message) {
		return delegate.assertionException(pos, message);
	}

	@Override
	public GameTestAssertPosException assertionException(BlockPos pos, String messageKey, Object... args) {
		return delegate.assertionException(pos, messageKey, args);
	}

	@Override
	public <T extends BlockEntity> T getBlockEntity(BlockPos pos, Class<T> clazz) {
		return delegate.getBlockEntity(pos, clazz);
	}

	@Override
	public ItemEntity spawnItem(Item item, Vec3 pos) {
		return delegate.spawnItem(item, pos);
	}

	@Override
	public void hurt(Entity entity, DamageSource damageSource, float amount) {
		delegate.hurt(entity, damageSource, amount);
	}

	@Override
	public void kill(Entity entity) {
		delegate.kill(entity);
	}

	@Override
	public <E extends Entity> E findOneEntity(EntityType<E> type) {
		return delegate.findOneEntity(type);
	}

	@Override
	public <E extends Entity> E findClosestEntity(EntityType<E> type, int x, int y, int z, double radius) {
		return delegate.findClosestEntity(type, x, y, z, radius);
	}

	@Override
	public <E extends Entity> List<E> findEntities(EntityType<E> type, int x, int y, int z, double radius) {
		return delegate.findEntities(type, x, y, z, radius);
	}

	@Override
	public <E extends Entity> List<E> findEntities(EntityType<E> type, Vec3 pos, double radius) {
		return delegate.findEntities(type, pos, radius);
	}

	@Override
	public void moveTo(Mob mob, float x, float y, float z) {
		delegate.moveTo(mob, x, y, z);
	}

	@Deprecated(forRemoval = true)
	@Override
	public ServerPlayer makeMockServerPlayerInLevel() {
		return delegate.makeMockServerPlayerInLevel();
	}

	@Override
	public void assertBlockTag(TagKey<Block> tag, BlockPos pos) {
		delegate.assertBlockTag(tag, pos);
	}

	@Override
	public void assertBlock(BlockPos pos, Predicate<Block> predicate, Function<Block, Component> message) {
		delegate.assertBlock(pos, predicate, message);
	}

	@Override
	public <T extends Comparable<T>> void assertBlockProperty(BlockPos pos, Property<T> property, Predicate<T> predicate, Component message) {
		delegate.assertBlockProperty(pos, property, predicate, message);
	}

	@Override
	public void assertBlockState(BlockPos pos, BlockState state) {
		delegate.assertBlockState(pos, state);
	}

	@Override
	public void assertBlockState(BlockPos pos, Predicate<BlockState> predicate, Function<BlockState, Component> message) {
		delegate.assertBlockState(pos, predicate, message);
	}

	@Override
	public <T extends BlockEntity> void assertBlockEntityData(BlockPos pos, Class<T> blockEntityClass, Predicate<T> predicate, Supplier<Component> message) {
		delegate.assertBlockEntityData(pos, blockEntityClass, predicate, message);
	}

	@Override
	public void assertRedstoneSignal(BlockPos pos, Direction direction, IntPredicate signalStrengthPredicate, Supplier<Component> message) {
		delegate.assertRedstoneSignal(pos, direction, signalStrengthPredicate, message);
	}

	@Override
	public void assertEntityPresent(EntityType<?> type, AABB box) {
		delegate.assertEntityPresent(type, box);
	}

	@Override
	public void assertEntitiesPresent(EntityType<?> entityType, int count) {
		delegate.assertEntitiesPresent(entityType, count);
	}

	@Override
	public <T extends Entity> List<T> getEntities(EntityType<T> entityType) {
		return delegate.getEntities(entityType);
	}

	@Override
	public void assertItemEntityPresent(Item item) {
		delegate.assertItemEntityPresent(item);
	}

	@Override
	public void assertItemEntityNotPresent(Item item) {
		delegate.assertItemEntityNotPresent(item);
	}

	@Override
	public void assertEntityNotPresent(EntityType<?> type, AABB box) {
		delegate.assertEntityNotPresent(type, box);
	}

	@Override
	public <E extends Entity, T> void assertEntityData(BlockPos pos, EntityType<E> type, Predicate<E> predicate) {
		delegate.assertEntityData(pos, type, predicate);
	}

	@Override
	public void assertContainerContainsSingle(BlockPos pos, Item item) {
		delegate.assertContainerContainsSingle(pos, item);
	}

	@Override
	public void assertEntityPosition(Entity entity, AABB boundingBox, Component message) {
		delegate.assertEntityPosition(entity, boundingBox, message);
	}

	@Override
	public <E extends Entity> void assertEntityProperty(E entity, Predicate<E> predicate, Component message) {
		delegate.assertEntityProperty(entity, predicate, message);
	}

	@Override
	public <E extends Entity, T> void assertEntityProperty(E entity, Function<E, T> valueGetter, T expectedValue, Component message) {
		delegate.assertEntityProperty(entity, valueGetter, expectedValue, message);
	}

	@Override
	public void assertLivingEntityHasMobEffect(LivingEntity entity, Holder<MobEffect> effect, int amplifier) {
		delegate.assertLivingEntityHasMobEffect(entity, effect, amplifier);
	}

	@Override
	public void tickBlock(BlockPos pos) {
		delegate.tickBlock(pos);
	}

	@Override
	public void tickPrecipitation(BlockPos pos) {
		delegate.tickPrecipitation(pos);
	}

	@Override
	public void tickPrecipitation() {
		delegate.tickPrecipitation();
	}

	@Override
	public void fail(Component message, BlockPos pos) {
		delegate.fail(message, pos);
	}

	@Override
	public void fail(Component message, Entity entity) {
		delegate.fail(message, entity);
	}

	@Override
	public void fail(Component message) {
		delegate.fail(message);
	}

	@Override
	public AABB absoluteAABB(AABB aabb) {
		return delegate.absoluteAABB(aabb);
	}

	@Override
	public AABB relativeAABB(AABB aabb) {
		return delegate.relativeAABB(aabb);
	}

	@Override
	public Rotation getTestRotation() {
		return delegate.getTestRotation();
	}

	@Override
	public void assertTrue(boolean condition, Component message) {
		delegate.assertTrue(condition, message);
	}

	@Override
	public <N> void assertValueEqual(N expected, N actual, Component name) {
		delegate.assertValueEqual(expected, actual, name);
	}

	@Override
	public void assertFalse(boolean condition, Component message) {
		delegate.assertFalse(condition, message);
	}

	@Override
	public AABB getBounds() {
		return delegate.getBounds();
	}

	@Override
	public void setBiome(ResourceKey<Biome> biome) {
		delegate.setBiome(biome);
	}

	@Override
	public void assertEntitiesPresent(EntityType<?> pEntityType, BlockPos pPos, int pCount, double pRadius) {
		delegate.assertEntitiesPresent(pEntityType, pPos, pCount, pRadius);
	}

	@Override
	public void assertEntityPresent(EntityType<?> pType, BlockPos pPos, double pExpansionAmount) {
		delegate.assertEntityPresent(pType, pPos, pExpansionAmount);
	}

	@Override
	public <T extends Entity> List<T> getEntities(EntityType<T> pEntityType, BlockPos pPos, double pRadius) {
		return delegate.getEntities(pEntityType, pPos, pRadius);
	}

	@Override
	public void assertEntityInstancePresent(Entity pEntity, int pX, int pY, int pZ) {
		delegate.assertEntityInstancePresent(pEntity, pX, pY, pZ);
	}

	@Override
	public void assertEntityInstancePresent(Entity pEntity, BlockPos pPos) {
		delegate.assertEntityInstancePresent(pEntity, pPos);
	}

	@Override
	public void assertItemEntityCountIs(Item pItem, BlockPos pPos, double pExpansionAmount, int pCount) {
		delegate.assertItemEntityCountIs(pItem, pPos, pExpansionAmount, pCount);
	}

	@Override
	public void assertItemEntityPresent(Item pItem, BlockPos pPos, double pExpansionAmount) {
		delegate.assertItemEntityPresent(pItem, pPos, pExpansionAmount);
	}

	@Override
	public void assertItemEntityNotPresent(Item pItem, BlockPos pPos, double pRadius) {
		delegate.assertItemEntityNotPresent(pItem, pPos, pRadius);
	}

	@Override
	public void assertEntityNotPresent(EntityType<?> pType) {
		delegate.assertEntityNotPresent(pType);
	}

	@Override
	public void assertEntityNotPresent(EntityType<?> pType, int pX, int pY, int pZ) {
		delegate.assertEntityNotPresent(pType, pX, pY, pZ);
	}

	@Override
	public void assertEntityNotPresent(EntityType<?> pType, BlockPos pPos) {
		delegate.assertEntityNotPresent(pType, pPos);
	}

	@Override
	public void assertEntityTouching(EntityType<?> pType, double pX, double pY, double pZ) {
		delegate.assertEntityTouching(pType, pX, pY, pZ);
	}

	@Override
	public void assertEntityNotTouching(EntityType<?> pType, double pX, double pY, double pZ) {
		delegate.assertEntityNotTouching(pType, pX, pY, pZ);
	}

	@Override
	public <E extends Entity, T> void assertEntityData(BlockPos pPos, EntityType<E> pType, Function<? super E, T> pEntityDataGetter, @org.jetbrains.annotations.Nullable T pTestEntityData) {
		delegate.assertEntityData(pPos, pType, pEntityDataGetter, pTestEntityData);
	}

	@Override
	public <E extends LivingEntity> void assertEntityIsHolding(BlockPos pPos, EntityType<E> pEntityType, Item pItem) {
		delegate.assertEntityIsHolding(pPos, pEntityType, pItem);
	}

	@Override
	public <E extends Entity & InventoryCarrier> void assertEntityInventoryContains(BlockPos pPos, EntityType<E> pEntityType, Item pItem) {
		delegate.assertEntityInventoryContains(pPos, pEntityType, pItem);
	}

	@Override
	public void assertContainerEmpty(BlockPos pPos) {
		delegate.assertContainerEmpty(pPos);
	}

	@Override
	public void assertContainerContains(BlockPos pPos, Item pItem) {
		delegate.assertContainerContains(pPos, pItem);
	}

	@Override
	public void assertSameBlockStates(BoundingBox pBoundingBox, BlockPos pPos) {
		delegate.assertSameBlockStates(pBoundingBox, pPos);
	}

	@Override
	public void assertSameBlockState(BlockPos pTestPos, BlockPos pComparisonPos) {
		delegate.assertSameBlockState(pTestPos, pComparisonPos);
	}

	@Override
	public void assertAtTickTimeContainerContains(long pTickTime, BlockPos pPos, Item pItem) {
		delegate.assertAtTickTimeContainerContains(pTickTime, pPos, pItem);
	}

	@Override
	public void assertAtTickTimeContainerEmpty(long pTickTime, BlockPos pPos) {
		delegate.assertAtTickTimeContainerEmpty(pTickTime, pPos);
	}

	@Override
	public <E extends Entity, T> void succeedWhenEntityData(BlockPos pPos, EntityType<E> pType, Function<E, T> pEntityDataGetter, T pTestEntityData) {
		delegate.succeedWhenEntityData(pPos, pType, pEntityDataGetter, pTestEntityData);
	}

	@Override
	public void succeedWhenEntityPresent(EntityType<?> pType, int pX, int pY, int pZ) {
		delegate.succeedWhenEntityPresent(pType, pX, pY, pZ);
	}

	@Override
	public void succeedWhenEntityPresent(EntityType<?> pType, BlockPos pPos) {
		delegate.succeedWhenEntityPresent(pType, pPos);
	}

	@Override
	public void succeedWhenEntityNotPresent(EntityType<?> pType, int pX, int pY, int pZ) {
		delegate.succeedWhenEntityNotPresent(pType, pX, pY, pZ);
	}

	@Override
	public void succeedWhenEntityNotPresent(EntityType<?> pType, BlockPos pPos) {
		delegate.succeedWhenEntityNotPresent(pType, pPos);
	}

	@Override
	public void succeed() {
		delegate.succeed();
	}

	@Override
	public void succeedIf(Runnable pCriterion) {
		delegate.succeedIf(pCriterion);
	}

	@Override
	public void succeedWhen(Runnable pCriterion) {
		delegate.succeedWhen(pCriterion);
	}

	@Override
	public void succeedOnTickWhen(int pTick, Runnable pCriterion) {
		delegate.succeedOnTickWhen(pTick, pCriterion);
	}

	@Override
	public void runAtTickTime(long pTickTime, Runnable pTask) {
		delegate.runAtTickTime(pTickTime, pTask);
	}

	@Override
	public void runAfterDelay(long pDelay, Runnable pTask) {
		delegate.runAfterDelay(pDelay, pTask);
	}

	@Override
	public void randomTick(BlockPos pPos) {
		delegate.randomTick(pPos);
	}

	@Override
	public int getHeight(Heightmap.Types pHeightmapType, int pX, int pZ) {
		return delegate.getHeight(pHeightmapType, pX, pZ);
	}

	@Override
	public void failIf(Runnable pCriterion) {
		delegate.failIf(pCriterion);
	}

	@Override
	public void failIfEver(Runnable pCriterion) {
		delegate.failIfEver(pCriterion);
	}

	@Override
	public GameTestSequence startSequence() {
		return delegate.startSequence();
	}

	@Override
	public BlockPos absolutePos(BlockPos pPos) {
		return delegate.absolutePos(pPos);
	}

	@Override
	public BlockPos relativePos(BlockPos pPos) {
		return delegate.relativePos(pPos);
	}

	@Override
	public Vec3 absoluteVec(Vec3 pRelativeVec3) {
		return delegate.absoluteVec(pRelativeVec3);
	}

	@Override
	public Vec3 relativeVec(Vec3 pAbsoluteVec3) {
		return delegate.relativeVec(pAbsoluteVec3);
	}

	public void assertTrue(boolean pCondition, String pFailureMessage) {
		delegate.assertTrue(pCondition, Component.literal(pFailureMessage));
	}

	public void assertTrue(boolean pCondition, Supplier<String> pFailureMessage) {
		if (!pCondition) {
			assertTrue(pCondition, pFailureMessage.get());
		}
	}

	public void assertFalse(boolean pCondition, String pFailureMessage) {
		assertFalse(pCondition, Component.literal(pFailureMessage));
	}

	@Override
	public long getTick() {
		return delegate.getTick();
	}

	@Override
	public void forEveryBlockInStructure(Consumer<BlockPos> pConsumer) {
		delegate.forEveryBlockInStructure(pConsumer);
	}

	@Override
	public void onEachTick(Runnable pTask) {
		delegate.onEachTick(pTask);
	}

	@Override
	public void placeAt(Player pPlayer, ItemStack pStack, BlockPos pPos, Direction pDirection) {
		delegate.placeAt(pPlayer, pStack, pPos, pDirection);
	}

	public TestGameLobby createGame(ServerPlayer player, PlayerRole initiatorRole) {
		String name = player.getScoreboardName() + "'s Lobby";

		GameResult<GameLobby> result = GameLobbyManager.get().createGameLobby(name, player);
		assertTrue(result.isOk(), () -> "Game could not be created: " + result.getError().getString());

		GameLobby lobby = result.getOk();
		lobby.getPlayers().join(player, initiatorRole);

		((List<GameTestListener>) ((GameTestInfoAccess) info).getListeners()).addFirst(new GameTestListener() {
			@Override
			public void testStructureLoaded(GameTestInfo pTestInfo) {

			}

			@Override
			public void testPassed(GameTestInfo pTest, GameTestRunner pRunner) {
				if (lobby.getTopPhase() != null) {
					lobby.getTopPhase().requestStop(GameStopReason.finished());
				}
				lobby.getManagement().close();
			}

			@Override
			public void testFailed(GameTestInfo pTest, GameTestRunner pRunner) {
				if (lobby.getTopPhase() != null) {
					lobby.getTopPhase().requestStop(GameStopReason.canceled());
				}
				lobby.getManagement().close();
			}

			@Override
			public void testAddedForRerun(GameTestInfo pOldTest, GameTestInfo pNewTest, GameTestRunner pRunner) {

			}
		});

		return new TestGameLobby(lobby);
	}

	public Runnable startGame(TestGameLobby game) {
		return () -> {
			final var result = game.lobby().getControls().get(LobbyControls.Type.PLAY).run();
			assertTrue(result.isOk(), () -> "Game could not start: " + result.getError().getString());
		};
	}

	public FakePlayerBuilder playerBuilder() {
		return new FakePlayerBuilder(this);
	}

	public LTFakePlayer createFakePlayer() {
		return playerBuilder().build();
	}

	public <T> void assertReceivedPacket(LTFakePlayer player, int index, Class<T> type, Predicate<T> test) {
		assertTrue(index < player.receivedPackets.size(), "Not enough packets received");
		final var pkt = player.receivedPackets.get(index);
		assertTrue(type.isInstance(pkt), "Received packet was of wrong type. Was: " + pkt.getClass() + ", expected: " + type);
		assertTrue(test.test(type.cast(pkt)), "Packet did not match!");
	}

	public void assertNoPacketsReceived(LTFakePlayer player) {
		assertTrue(player.receivedPackets.isEmpty(), "Player received at least a packet!");
	}

	public void assertPlayerInventoryContainsAt(Player player, int index, ItemStack stack) {
		final ItemStack toCompare = player.getInventory().getItem(index);
		assertTrue(ItemStack.isSameItemSameComponents(stack, toCompare), () -> "Items did not match: expected " + stack + ", but was " + toCompare);
		assertTrue(stack.getCount() == toCompare.getCount(), () -> "Stack count did not match: expected " + stack.getCount() + ", but was " + toCompare.getCount());
	}

	public void assertEntityHealth(LivingEntity entity, float health) {
		assertTrue(entity.getHealth() == health, () -> "Entity health did not match! Expected " + health + " but was " + entity.getHealth());
	}

	public void assertEntityMaxHealth(LivingEntity entity, float health) {
		assertTrue(entity.getMaxHealth() == health, () -> "Entity max health did not match! Expected " + health + " but was " + entity.getMaxHealth());
	}

	public TestPermissionAPI.Roles getRoles(Entity entity) {
		return LTMinigamesGameTests.PERMISSIONS.byEntity(entity);
	}
}
