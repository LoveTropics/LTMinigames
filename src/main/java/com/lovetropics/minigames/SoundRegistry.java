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

	public static final Holder<SoundEvent> generic_no_1 = register("escape_race.generic.no.1");
	public static final Holder<SoundEvent> generic_no_2 = register("escape_race.generic.no.2");
	public static final Holder<SoundEvent> generic_no_3 = register("escape_race.generic.no.3");
	public static final Holder<SoundEvent> generic_no_4 = register("escape_race.generic.no.4");
	public static final Holder<SoundEvent> generic_yes_1 = register("escape_race.generic.yes.1");
	public static final Holder<SoundEvent> generic_yes_2 = register("escape_race.generic.yes.2");
	public static final Holder<SoundEvent> generic_yes_3 = register("escape_race.generic.yes.3");
	public static final Holder<SoundEvent> generic_yes_4 = register("escape_race.generic.yes.4");
	public static final Holder<SoundEvent> room_1_generic = register("escape_race.room_1.generic");
	public static final Holder<SoundEvent> room_1_intro_1 = register("escape_race.room_1.intro.1");
	public static final Holder<SoundEvent> room_5_4 = register("escape_race.room_5.4");
	public static final Holder<SoundEvent> warehouse_2 = register("escape_race.warehouse.2");
	public static final Holder<SoundEvent> warehouse_3 = register("escape_race.warehouse.3");
	public static final Holder<SoundEvent> warehouse_4 = register("escape_race.warehouse.4");
	public static final Holder<SoundEvent> warehouse_5 = register("escape_race.warehouse.5");
	public static final Holder<SoundEvent> warehouse_6 = register("escape_race.warehouse.6");
	public static final Holder<SoundEvent> warehouse_7 = register("escape_race.warehouse.7");
	public static final Holder<SoundEvent> warehouse_8 = register("escape_race.warehouse.8");
	public static final Holder<SoundEvent> warehouse_9 = register("escape_race.warehouse.9");
	public static final Holder<SoundEvent> warehouse_10 = register("escape_race.warehouse.10");
	public static final Holder<SoundEvent> warehouse_11 = register("escape_race.warehouse.11");
	public static final Holder<SoundEvent> warehouse_12 = register("escape_race.warehouse.12");
	public static final Holder<SoundEvent> warehouse_13 = register("escape_race.warehouse.13");
	public static final Holder<SoundEvent> warehouse_14 = register("escape_race.warehouse.14");
	public static final Holder<SoundEvent> warehouse_15 = register("escape_race.warehouse.15");
	public static final Holder<SoundEvent> warehouse_16 = register("escape_race.warehouse.16");
	public static final Holder<SoundEvent> warehouse_17 = register("escape_race.warehouse.17");

	public static final Holder<SoundEvent> UPSET_STOMACH_FART = register("upset_stomach.fart");

	private static Holder<SoundEvent> register(String name) {
		return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(LoveTropics.location(name)));
	}
}
