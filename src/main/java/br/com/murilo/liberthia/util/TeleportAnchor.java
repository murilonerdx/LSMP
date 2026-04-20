package br.com.murilo.liberthia.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record TeleportAnchor(ResourceKey<Level> dimension, double x, double y, double z) {
}
