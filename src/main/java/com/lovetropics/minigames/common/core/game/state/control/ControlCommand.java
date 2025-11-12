package com.lovetropics.minigames.common.core.game.state.control;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.util.StringRepresentable;

import java.util.function.Predicate;

public record ControlCommand(Scope scope, Action action) {
	private static final SimpleCommandExceptionType NO_PERMISSION = new SimpleCommandExceptionType(new LiteralMessage("You do not have permission to use this command!"));

	public void invoke(CommandSourceStack source) throws CommandSyntaxException {
		if (!scope.canUse(source)) {
			throw NO_PERMISSION.create();
		}
		action.run(source);
	}

	public enum Scope implements StringRepresentable {
		EVERYONE("everyone", Commands.hasPermission(Commands.LEVEL_ALL)),
		ADMINS("admins", Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)),
		;

		public static final Codec<Scope> CODEC = StringRepresentable.fromEnum(Scope::values);

		private final String name;
		private final Predicate<CommandSourceStack> permissionCheck;

		Scope(String name, Predicate<CommandSourceStack> permissionCheck) {
			this.name = name;
			this.permissionCheck = permissionCheck;
		}

		public boolean canUse(CommandSourceStack source) {
			return permissionCheck.test(source);
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public interface Action {
		void run(CommandSourceStack source);
	}
}
