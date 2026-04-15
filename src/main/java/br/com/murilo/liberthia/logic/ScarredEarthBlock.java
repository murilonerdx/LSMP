package br.com.murilo.liberthia.logic;

import net.minecraft.world.level.block.Block;

/**
 * Cicatriz no terreno — substitui solo corrompido quando infecção é limpa.
 * Bloco simples sem randomTick.
 */
public class ScarredEarthBlock extends Block {
    public ScarredEarthBlock(Properties properties) {
        super(properties);
    }
}
