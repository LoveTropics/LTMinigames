package com.lovetropics.minigames.gametests.api;

import net.minecraft.SharedConstants;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GameTest {
	int timeoutTicks() default SharedConstants.TICKS_PER_SECOND * 5;
}
