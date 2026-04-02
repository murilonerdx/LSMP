package br.com.murilo.liberthia.logic;

import net.minecraft.core.BlockPos;

/**
 * Exemplo simples de validação das regras atuais de growth:
 * - altura máxima de 3 blocos;
 * - distância mínima de 8 blocos entre focos.
 */
public final class InfectionSpacingExample {
    public static final int MAX_HEIGHT = 3;
    public static final int MIN_SPACING = 8;

    private InfectionSpacingExample() {
    }

    public static boolean isValidGrowth(int growthHeight, BlockPos a, BlockPos b) {
        if (growthHeight < 1 || growthHeight > MAX_HEIGHT) {
            return false;
        }

        double horizontalDistance = Math.sqrt(a.distToLowCornerSqr(b.getX(), a.getY(), b.getZ()));
        return horizontalDistance >= MIN_SPACING;
    }
}
