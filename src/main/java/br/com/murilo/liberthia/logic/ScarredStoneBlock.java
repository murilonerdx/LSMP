package br.com.murilo.liberthia.logic;

import net.minecraft.world.level.block.Block;

/**
 * Cicatriz no terreno — substitui pedra corrompida/matéria escura quando infecção é limpa.
 * Bloco simples sem randomTick.
 */
public class ScarredStoneBlock extends Block {
    public ScarredStoneBlock(Properties properties) {
        super(properties);
    }
}
