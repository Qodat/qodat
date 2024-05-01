package net.runelite.mapping;

public @interface ObfuscatedSignature
{
	String descriptor();

	String garbageValue() default ""; // valid garbage value for last parameter. can't be an Object because Java.
}
