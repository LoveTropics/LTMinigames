package com.lovetropics.minigames.gametests.api;

import org.objectweb.asm.Type;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface RegisterMinigameTest {
	Type TYPE = Type.getType(RegisterMinigameTest.class);
}
