package com.lovetropics.minigames.common.minigames;

import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;

/**
 * An instance used to track which participants and spectators are inside
 * the running minigame. Also holds the definition to process the content
 * within the minigame.
 */
public interface IMinigameInstance
{
    /**
     * The definition used to define what content the minigame contains.
     * @return The minigame definition.
     */
    IMinigameDefinition getDefinition();

    /**
     * Adds a player to a list of participants for tracking throughout the minigame.
     *
     * @param player The participant being added to the minigame instance.
     * @throws IllegalArgumentException Will throw an exception if you try to
     * add a player that is either:
     *
     * - Already a participant in the instance.
     * - Or a spectator in the instance.
     *
     * If you'd like to add a participant and they're already a spectator, make sure
     * to remove them as a spectator first.
     */
    void addParticipant(ServerPlayerEntity player) throws IllegalArgumentException;

    /**
     * Removes a player from a list of participants for tracking throughout the minigame.
     * @param player The particiapnt being removed from the minigame instance.
     * @throws IllegalArgumentException Will throw an exception if you try to
     * remove a participant that isn't in the minigame instance.
     */
    void removeParticipant(ServerPlayerEntity player) throws IllegalArgumentException;

    /**
     * Adds the player to a list of spectators for tracking.
     * @param player The player you're add as a spectator to the instance.
     * @throws IllegalArgumentException Will throw an exception if you try to add
     * a spectator that is already a participant in the instance or is already a spectator.
     */
    void addSpectator(ServerPlayerEntity player) throws IllegalArgumentException;

    /**
     * Removes the player from a list of spectators for tracking.
     * @param player The player you're removing as a spectator from the instance.
     * @throws IllegalArgumentException Will throw an exception if you try to remove
     * a spectator that isn't a spectator in the instance.
     */
    void removeSpectator(ServerPlayerEntity player) throws IllegalArgumentException;

    /**
     * @return The list of participants that are playing within the minigame instance.
     */
    PlayerSet getParticipants();

    /**
     * @return The list of spectators that are observing the minigame instance.
     */
    PlayerSet getSpectators();

    /**
     * @return The list of all players that are a part of this minigame instance.
     */
    PlayerSet getAllPlayers();

    /**
     * Used for executing commands of datapacks within the minigames.
     * @return The command source for this minigame instance.
     */
    CommandSource getCommandSource();
}
