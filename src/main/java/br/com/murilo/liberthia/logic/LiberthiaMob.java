package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * v0.1.22: helper centralizado pra detectar QUALQUER mob/entity do mod
 * Liberthia (sangue + flesh + corruptos + ordem + outros). Usado pelos
 * sistemas da Boss Crown e outras features pra "skip" o dano em entities
 * próprias do mod (aliados implícitos do dono da coroa).
 *
 * <p>Implementação: pega o registry ID do EntityType e verifica se o
 * namespace é "liberthia". Cobre TUDO sem precisar manter lista hardcoded.
 */
public final class LiberthiaMob {

    private LiberthiaMob() {}

    /**
     * True se o entity é de qualquer mob/entity registrado pelo mod Liberthia.
     * Implementação via namespace do EntityType registry ID — cobre todos
     * automaticamente (blood, flesh, corrupted, ordem, projéteis, etc).
     */
    public static boolean isMob(Entity entity) {
        if (entity == null) return false;
        EntityType<?> type = entity.getType();
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return key != null && LiberthiaMod.MODID.equals(key.getNamespace());
    }

    /** Conveniência: aceita LivingEntity ou null. */
    public static boolean isMobLiving(LivingEntity entity) {
        return isMob(entity);
    }
}
