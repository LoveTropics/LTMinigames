package com.lovetropics.minigames.common.core.game.state.control;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

public record ControlCommand(Scope scope, Handler handler) {
	private static final SimpleCommandExceptionType NO_PERMISSION = new SimpleCommandExceptionType(new LiteralMessage("You do not have permission to use this command!"));

	public static ControlCommand forEveryone(Handler handler) {
		return new ControlCommand(Scope.EVERYONE, handler);
	}

	public static ControlCommand forAdmins(Handler handler) {
		return new ControlCommand(Scope.ADMINS, handler);
	}

	public static ControlCommand forInitiator(Handler handler) {
		return new ControlCommand(Scope.INITIATOR, handler);
	}

	public void invoke(CommandSourceStack source, @Nullable PlayerKey initiator) throws CommandSyntaxException {
		if (!canUse(source, initiator)) {
			throw NO_PERMISSION.create();
		}

		handler.run(source);
	}

	public boolean canUse(CommandSourceStack source, @Nullable PlayerKey initiator) {
		return scope.testSource(source, initiator);
	}

	public enum Scope {
		EVERYONE("everyone") {
			@Override
			public boolean testSource(CommandSourceStack source, @Nullable PlayerKey initiator) {
				return true;
			}
		},
		ADMINS("admins") {
			@Override
			public boolean testSource(CommandSourceStack source, @Nullable PlayerKey initiator) {
				return source.hasPermission(Commands.LEVEL_GAMEMASTERS);
			}
		},
		INITIATOR("initiator") {
			@Override
			public boolean testSource(CommandSourceStack source, @Nullable PlayerKey initiator) {
				if (source.hasPermission(Commands.LEVEL_GAMEMASTERS)) {
					return true;
				}

				Entity entity = source.getEntity();
				if (entity instanceof ServerPlayer) {
					return initiator != null && initiator.matches(entity);
				}

				return false;
			}
		};

		public static final Codec<Scope> CODEC = MoreCodecs.stringVariants(Scope.values(), s -> s.key);

		public final String key;

		Scope(String key) {
			this.key = key;
		}

		public abstract boolean testSource(CommandSourceStack source, @Nullable PlayerKey initiator);
	}

	public interface Handler {
		void run(CommandSourceStack source);
	}
}
