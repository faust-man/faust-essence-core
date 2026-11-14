package com.faust.core.runtime;
/** Client callback without loading client classes on dedicated servers. */
public final class ClientActions {public static java.util.function.Predicate<Object> softFoot=entity->false;public static java.util.function.Predicate<Object> silentStep=entity->false;public static java.util.function.Predicate<Object> sprintAllowed=entity->true;public static java.util.function.BooleanSupplier jumpAllowed=()->true;}
