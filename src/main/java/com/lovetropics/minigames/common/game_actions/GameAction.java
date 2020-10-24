package com.lovetropics.minigames.common.game_actions;

import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

public abstract class GameAction implements Comparable<GameAction> {
    public UUID uuid;
    public String triggerTime;

    public GameAction(final UUID uuid, final String triggerTime) {
        this.uuid = uuid;
        this.triggerTime = triggerTime;
    }

    /**
     * Resolves the requested action.
     *
     * @param server The game server the action is resolved on.
     * @return Whether or not to send an acknowledgement back that
     * the action has been resolved.
     */
    public abstract boolean resolve(final MinecraftServer server);

    @Override
    public int compareTo(final GameAction other) {
        final LocalDateTime thisDate = getDate(triggerTime);
        final LocalDateTime thatDate = getDate(other.triggerTime);
        if (thisDate != null && thatDate != null) {
            return thisDate.compareTo(thatDate);
        }
        return 0;
    }

    @Nullable
    private LocalDateTime getDate(String dateStr) {
        dateStr = dateStr.split("\\.")[0];
        LocalDateTime date;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            date = LocalDateTime.parse(dateStr, formatter);
        }
        catch (final DateTimeParseException exc) {
            System.out.printf("%s is not parsable!%n", dateStr);
            return null;
        }
        return date;
    }
}