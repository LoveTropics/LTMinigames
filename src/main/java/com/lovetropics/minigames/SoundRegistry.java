package com.lovetropics.minigames;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SoundRegistry {
	public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(Registries.SOUND_EVENT, LoveTropics.ID);

	public static final Holder<SoundEvent> SWAP_PLAYERS = register("swap_players");
	public static final Holder<SoundEvent> PACKAGE_RECEIVE = register("package_receive");
	public static final Holder<SoundEvent> SABOTAGE_RECEIVE = register("sabotage_receive");
	public static final Holder<SoundEvent> ACID_FLASH_FLOODING_IMMINENT = register("stt4.acid_flash_flooding_imminent");
	public static final Holder<SoundEvent> FLASH_FLOODING_IMMINENT = register("stt4.flash_flooding_imminent");
	public static final Holder<SoundEvent> LAST_SHUTTLE_DEPARTING = register("stt4.last_shuttle_departing");
	public static final Holder<SoundEvent> COINS = register("coins");
	public static final Holder<SoundEvent> CORRECT = register("correct");
	public static final Holder<SoundEvent> INCORRECT = register("incorrect");
	public static final Holder<SoundEvent> QUIET_EXPLOSION = register("quiet_explosion");

	public static final Holder<SoundEvent> TERRY_TRASH_SOUND_1 = register("escape_race.terry_trash.sound_1");
	public static final Holder<SoundEvent> TERRY_TRASH_SOUND_2 = register("escape_race.terry_trash.sound_2");
	public static final Holder<SoundEvent> TERRY_TRASH_SOUND_3 = register("escape_race.terry_trash.sound_3");
	public static final Holder<SoundEvent> TERRY_TRASH_SOUND_4 = register("escape_race.terry_trash.sound_4");
	public static final Holder<SoundEvent> TERRY_TRASH_SOUND_5 = register("escape_race.terry_trash.sound_5");
	public static final Holder<SoundEvent> TERRY_TRASH_SOUND_6 = register("escape_race.terry_trash.sound_6");

	public static final Holder<SoundEvent> UPSET_STOMACH_FART = register("upset_stomach.fart");

	private static Holder<SoundEvent> register(String name) {
		return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(LoveTropics.location(name)));
	}
}
