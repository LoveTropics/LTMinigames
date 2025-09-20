package com.lovetropics.minigames.common.content.connect4;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameWinner;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.util.SequentialList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.TriState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConnectFourBehavior implements IGameBehavior {
    public static final MapCodec<ConnectFourBehavior> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
            Codec.unboundedMap(GameTeamKey.CODEC, GameBlock.CODEC.codec()).fieldOf("team_blocks").forGetter(c -> c.teamBlocks),
            Codec.unboundedMap(GameTeamKey.CODEC, ProgressChannel.CODEC).fieldOf("team_timers").forGetter(c -> c.teamTimers),
            Codec.STRING.fieldOf("placing_region").forGetter(c -> c.placingRegionKey),
            MoreCodecs.BLOCK_STATE.fieldOf("separator").forGetter(c -> c.separator),
            MoreCodecs.BLOCK_STATE.fieldOf("blocker").forGetter(c -> c.blocker),
            Codec.INT.fieldOf("grid_width").forGetter(c -> c.width),
            Codec.INT.fieldOf("grid_height").forGetter(c -> c.height),
            Codec.INT.optionalFieldOf("connect", 4).forGetter(c -> c.connectAmount)
    ).apply(in, ConnectFourBehavior::new));

    private final Map<GameTeamKey, GameBlock> teamBlocks;
    private final Map<GameTeamKey, ProgressChannel> teamTimers;
    private final String placingRegionKey;
    private final BlockState separator;
    private final BlockState blocker;

    private final int width, height, connectAmount;

    public ConnectFourBehavior(Map<GameTeamKey, GameBlock> teamBlocks, Map<GameTeamKey, ProgressChannel> teamTimers, String placingRegionKey, BlockState separator, BlockState blocker, int width, int height, int connectAmount) {
        this.teamBlocks = teamBlocks;
        this.teamTimers = teamTimers;
        this.placingRegionKey = placingRegionKey;
        this.separator = separator;
        this.blocker = blocker;
        this.width = width;
        this.height = height;
        this.connectAmount = connectAmount;
    }

    private IGamePhase game;

    @Nullable
    private PendingGate pendingGate;

    private SequentialList<PlayingTeam> playingTeams;

    private TeamState teams;
    private BlockBox placingRegion;

    private PlacedPiece[][] pieces;
    private int placedPieces;

    private boolean gameOver;

    @Override
    public void register(IGamePhase game, EventRegistrar events) throws GameException {
        this.game = game;
        placingRegion = game.mapRegions().getOrThrow(placingRegionKey);
        teams = game.instanceState().getOrThrow(TeamState.KEY);

        pieces = new PlacedPiece[width][height];
        placedPieces = 0;

        events.listen(GamePhaseEvents.START, this::onStart);

        events.listen(GamePlayerEvents.PLACE_BLOCK, this::onPlaceBlock);
        events.listen(GamePlayerEvents.BREAK_BLOCK, (player, pos, state, hand) -> player.isCreative() ? TriState.DEFAULT : TriState.FALSE);

        events.listen(GameWorldEvents.BLOCK_LANDED, this::onBlockLanded);

        events.listen(GameLogicEvents.GAME_OVER, winner -> gameOver = true);
    }

    private void onStart() {
        var teams = new ArrayList<PlayingTeam>(this.teams.getTeamKeys().size());
        this.teams.getTeamKeys().forEach(key -> {
            var players = this.teams.getPlayersForTeam(key).stream().map(PlayerKey::from).toList();
            if (!players.isEmpty()) {
                var timer = teamTimers.get(key).getOrThrow(game);
                timer.pause();
                teams.add(new PlayingTeam(key, timer, new SequentialList<>(players, -1)));
            }
        });
        Collections.shuffle(teams);
        playingTeams = new SequentialList<>(teams, -1);

        nextPlayer();
    }

    private TriState onPlaceBlock(ServerPlayer player, BlockPos pos, BlockState placed, BlockState placedOn, ItemStack placedItemStack) {
        if (player.isCreative()) return TriState.DEFAULT;

        if (gameOver || !Objects.equals(playingTeams.current().players.current(), PlayerKey.from(player)) || !placingRegion.contains(pos))
            return TriState.FALSE;

        var expected = teamBlocks.get(playingTeams.current().key).powder;
        if (expected != placed.getBlock()) return TriState.FALSE;

        int x = blockToGridX(pos);
        if (x < 0 || x >= pieces.length) {
            return TriState.FALSE;
        }

		PlacedPiece[] column = pieces[x];
		if (column[column.length - 1] != null) {
            // Column already filled
            player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0f, 1.0f);
			return TriState.FALSE;
		}

        var below = pos.below();
        pendingGate = new PendingGate(below, player.level().getBlockState(below));
        player.level().setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

        return TriState.DEFAULT;
    }

    private void onBlockLanded(ServerLevel level, BlockPos pos, BlockState state) {
        if (gameOver) {
            return;
        }

        var currentTeam = playingTeams.current();
        var expected = teamBlocks.get(currentTeam.key);

        if (!state.is(expected.powder)) return;

        level.setBlock(pos, expected.solid.defaultBlockState(), Block.UPDATE_ALL);

        if (pendingGate != null) {
            level.setBlock(pendingGate.gatePosition(), pendingGate.gate(), Block.UPDATE_ALL);
            pendingGate = null;
        }

        level.setBlock(pos.above(), separator, Block.UPDATE_ALL);
        // Separator above, and gate above that
        if (placingRegion.contains(pos.getX(), pos.getY() + 3, pos.getZ())) {
            level.setBlock(pos.above(3), blocker, Block.UPDATE_ALL);
        }

        int x = blockToGridX(pos);
        var column = pieces[x];
        int y;
        for (y = 0; y < column.length; y++) {
            if (column[y] == null) {
                column[y] = new PlacedPiece(pos, currentTeam.key());
                break;
            }
        }
        placedPieces++;

        var team = currentTeam.key();

        currentTeam.timer().pause();
        game.allPlayers().getPlayerBy(currentTeam.players().current()).setGlowingTag(false);

        Line winningLine = checkWin(x, y, team);
        if (winningLine != null) {
            showBlinkingLine(level, team, winningLine);
            GameTeam gameTeam = teams.getTeamByKey(team);
            triggerGameOver(new GameWinner.Team(gameTeam));
        } else {
            if (placedPieces == width * height) {
                triggerGameOver(new GameWinner.Nobody());
            } else {
                nextPlayer();
            }
        }
    }

    private int blockToGridX(BlockPos pos) {
        if (placingRegion.min().getX() == placingRegion.max().getX()) {
            return (pos.getZ() - Math.min(placingRegion.min().getZ(), placingRegion.max().getZ())) / 2;
        } else {
            return (pos.getX() - Math.min(placingRegion.min().getX(), placingRegion.max().getX())) / 2;
        }
    }

    private void showBlinkingLine(ServerLevel level, GameTeamKey team, Line winningLine) {
        GameBlock teamBlocks = this.teamBlocks.get(team);
        MutableBoolean blink = new MutableBoolean(true);
        game.scheduler().runPeriodic(0, SharedConstants.TICKS_PER_SECOND / 2,() -> {
            BlockState blockState = blink.getValue() ? teamBlocks.highlighted().defaultBlockState() : teamBlocks.solid().defaultBlockState();
            fillLine(level, winningLine, blockState);
            blink.setValue(!blink.getValue());
        });
    }

    private void fillLine(ServerLevel level, Line line, BlockState blockState) {
        for (int i = 0; i < connectAmount; i++) {
            PlacedPiece piece = pieces[line.x(i)][line.y(i)];
            if (piece != null) {
                level.setBlockAndUpdate(piece.pos(), blockState);
            }
        }
    }

    private void triggerGameOver(GameWinner winner) {
        game.invoker(GameLogicEvents.GAME_OVER).onGameOver(winner);
    }

    private void nextPlayer() {
        var nextTeam = playingTeams.next();
        var nextPlayer = nextTeam.players().next();

        game.allPlayers().sendMessage(ConnectFourTexts.TEAM_GOES_NEXT.apply(teams.getTeamByKey(nextTeam.key).config().styledName()), true);

        nextTeam.timer().start();

        var player = game.allPlayers().getPlayerBy(nextPlayer);
        player.addItem(teamBlocks.get(nextTeam.key).powder.asItem().getDefaultInstance());

        player.setGlowingTag(true);
        PlayerSet.of(player).showTitle(ConnectFourTexts.IT_IS_YOUR_TURN_TITLE, ConnectFourTexts.IT_IS_YOUR_TURN_SUBTITLE, 10, 40, 10);
        player.playNotifySound(SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    @Nullable
    private Line checkWin(int x, int y, GameTeamKey team) {
        List<Line> possibleLines = new ArrayList<>();
        possibleLines.add(new Line(x, y, 0, -1)); // vertical
        for (int offset = 0; offset < connectAmount; offset++) {
            possibleLines.add(new Line(x - (connectAmount - 1) + offset, y, 1, 0)); // horizontal
            possibleLines.add(new Line(x - (connectAmount - 1) + offset, y + (connectAmount - 1) - offset, 1, -1)); // leading diagonal
            possibleLines.add(new Line(x - (connectAmount - 1) + offset, y - (connectAmount - 1) + offset, 1, 1)); // trailing diagonal
        }
        for (Line line : possibleLines) {
            if (checkLine(line, team)) {
                return line;
            }
        }
        return null;
    }

    private boolean checkLine(Line line, GameTeamKey team) {
        for (int i = 0; i < connectAmount; i++) {
            int x = line.x(i);
            int y = line.y(i);
            if (x < 0 || x > pieces.length - 1) {
                return false;
            }
            if (y < 0 || y > pieces[x].length - 1) {
                return false;
            }
            PlacedPiece piece = pieces[x][y];
            if (piece == null || team != piece.team) {
                return false;
            }
        }
        return true;
    }

    private record Line(int xs, int ys, int dx, int dy) {
        public int x(int i) {
            return xs + dx * i;
        }

        public int y(int i) {
            return ys + dy * i;
        }
    }

    private record PlacedPiece(BlockPos pos, GameTeamKey team) {
    }

    private record GameBlock(Block powder, Block solid, Block highlighted) {
        public static final MapCodec<GameBlock> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("powder").forGetter(GameBlock::powder),
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("solid").forGetter(GameBlock::solid),
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("highlighted").forGetter(GameBlock::highlighted)
        ).apply(in, GameBlock::new));
    }

    private record PendingGate(BlockPos gatePosition, BlockState gate) {
    }

    private record PlayingTeam(GameTeamKey key, ProgressHolder timer, SequentialList<PlayerKey> players) {

    }
}
