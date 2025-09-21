package com.lovetropics.minigames.common.util.world.gamedata;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.util.TranslationCollector;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public class GameDataAccessor implements DataAccessor {

	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".commands.gamedata.");

	public static final TranslationCollector.Fun5 STORAGE_GET = KEYS.add5("storage.get", "%s in %s for %s after scale factor of %s is %s");
	public static final TranslationCollector.Fun2 STORAGE_MODIFIED = KEYS.add2("storage.modified", "Modified %s in %s");
	public static final TranslationCollector.Fun3 STORAGE_QUERY = KEYS.add3("storage.query", "Gamedata for %s in %s has the following contents: %s");

	static final SuggestionProvider<CommandSourceStack> SUGGEST_GAMEDATA = ((context, builder) -> SharedSuggestionProvider.suggestResource(
			getGameDataStorage(context).playerData.keySet(), builder
	));

	public static final Function<String, DataCommands.DataProvider> PROVIDER = (str) -> new DataCommands.DataProvider() {
		@Override
		public DataAccessor access(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
			Collection<GameProfile> gameProfiles = GameProfileArgument.getGameProfiles(context, "player");
			if (gameProfiles.size() != 1) {
				throw EntityArgument.ERROR_NOT_SINGLE_PLAYER.create();
			}
			Optional<GameProfile> playerProfile = gameProfiles.stream().findFirst();
			return new GameDataAccessor(context.getSource().getLevel(), getGameDataStorage(context),
					ResourceLocationArgument.getId(context, str), playerProfile.get());
		}

		@Override
		public ArgumentBuilder<CommandSourceStack, ?> wrap(ArgumentBuilder<CommandSourceStack, ?> builder, Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> action) {
			return builder.then(
					Commands.literal("gamedata")
							.then(Commands.argument(str, ResourceLocationArgument.id()).suggests(GameDataAccessor.SUGGEST_GAMEDATA)
									.then(action.apply(Commands.argument("player", GameProfileArgument.gameProfile()))))
			);
		}
	};

	@NotNull
	private static GameDataStorage getGameDataStorage(CommandContext<CommandSourceStack> context) {
		return GameDataStorage.get(context.getSource().getLevel());
	}

	private final Level level;
	private final GameDataStorage gameDataStorage;
	private final ResourceLocation id;
	private final GameProfile player;

	public GameDataAccessor(Level level, GameDataStorage gameDataStorage, ResourceLocation id, GameProfile player) {
		this.level = level;
		this.gameDataStorage = gameDataStorage;
		this.id = id;
		this.player = player;
	}

	@Override
	public void setData(CompoundTag other) throws CommandSyntaxException {
		gameDataStorage.set(id, player.getId(), other);
	}

	@Override
	public CompoundTag getData() throws CommandSyntaxException {
		return gameDataStorage.get(id, player.getId());
	}

	@Override
	public Component getModifiedSuccess() {
		return STORAGE_MODIFIED.apply(player.getName(), Component.translationArg(this.id));
	}

	@Override
	public Component getPrintSuccess(Tag nbt) {
		return STORAGE_QUERY.apply(player.getName(), Component.translationArg(this.id), NbtUtils.toPrettyComponent(nbt));
	}

	@Override
	public Component getPrintSuccess(NbtPathArgument.NbtPath path, double scale, int value) {
		return STORAGE_GET.apply(path.asString(), Component.translationArg(this.id), player.getName(), String.format(Locale.ROOT, "%.2f", scale), value);
	}
}
