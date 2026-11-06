package org.lovetropics.games.common.core.game.behavior.instances.donation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.IGameState;
import org.lovetropics.games.common.core.game.util.GameTexts;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DonationMilestones implements IGameState {
	public static final GameStateKey<DonationMilestones> KEY = GameStateKey.create("Donation milestones");

	private final Map<String, Progress> byId = new LinkedHashMap<>();

	/// @throws GameException if another milestone of this phase already has this id, as their commands would clash
	Progress add(String id, double every) {
		if (byId.containsKey(id)) {
			throw new GameException(Component.literal("Duplicate donation milestones id: " + id));
		}
		Progress progress = new Progress(id, every);
		byId.put(id, progress);
		return progress;
	}

	Collection<Progress> all() {
		return Collections.unmodifiableCollection(byId.values());
	}

	static final class Progress {
		static final double MIN_AMOUNT = 0.01;

		private final String id;
		private long everyCents;
		private long donatedCents;

		private Progress(String id, double every) {
			this.id = id;
			everyCents = toCents(every);
		}

		/// @return how many milestones were reached
		int donate(double amount) {
			donatedCents += Math.max(Math.round(amount * 100.0), 0);
			int reached = (int) (donatedCents / everyCents);
			donatedCents %= everyCents;
			return reached;
		}

		void setEvery(double every) {
			long newEveryCents = toCents(every);
			donatedCents = Math.min(Math.round((double) donatedCents * newEveryCents / everyCents), newEveryCents - 1);
			everyCents = newEveryCents;
		}

		Component describe() {
			return GameTexts.Commands.DONATION_MILESTONES.apply(name(), format(everyCents), format(everyCents - donatedCents));
		}

		Component name() {
			return Component.literal(id).withStyle(ChatFormatting.AQUA);
		}

		private static long toCents(double every) {
			return Math.max(Math.round(every * 100.0), 1);
		}

		private static String format(long cents) {
			return cents % 100 == 0 ? String.valueOf(cents / 100) : String.format("%d.%02d", cents / 100, cents % 100);
		}
	}
}
