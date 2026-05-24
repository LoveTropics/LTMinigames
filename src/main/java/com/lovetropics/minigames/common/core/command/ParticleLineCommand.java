package com.lovetropics.minigames.common.core.command;

import com.lovetropics.minigames.client.particle_line.DrawParticleLineMessage;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class ParticleLineCommand {
	public static void register(CommandBuildContext context, CommandDispatcher<CommandSourceStack> dispatcher) {
		// @formatter:off
		dispatcher.register(
			literal("drawline").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(argument("particle", ParticleArgument.particle(context))
				.then(argument("from", Vec3Argument.vec3())
				.then(argument("to", Vec3Argument.vec3())
				.then(argument("spacing", FloatArgumentType.floatArg(0.01F))
				.executes(ParticleLineCommand::spawnLine)
			))))
		);
		// @formatter:on
	}

	private static int spawnLine(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();

		ParticleOptions particle = ParticleArgument.getParticle(context, "particle");
		Vec3 from = Vec3Argument.getVec3(context, "from");
		Vec3 to = Vec3Argument.getVec3(context, "to");
		float spacing = FloatArgumentType.getFloat(context, "spacing");

		PacketDistributor.sendToPlayer(player, new DrawParticleLineMessage(particle, from, to, spacing));

		return Command.SINGLE_SUCCESS;
	}
}
