package br.com.murilo.liberthia.block.entity;

import net.minecraft.world.inventory.ContainerData;
import net.minecraftforge.items.IItemHandler;

/**
 * r182 — contrato dos blocos tech que abrem a GUI de energia unificada
 * ({@code menu/TechEnergyMenu} + {@code client/screen/TechEnergyScreen}).
 * Layouts: PASSIVE (só energia: solar/térmico), GENERATOR (slot de combustível +
 * barra de queima: reator/furnator/magmator), CHARGER (slot de carga: célula/carregador).
 */
public interface ITechEnergyUI {
    int LAYOUT_PASSIVE = 0;
    int LAYOUT_GENERATOR = 1;
    int LAYOUT_CHARGER = 2;
    int LAYOUT_CONSUMER = 3; // consome FE p/ um efeito (selos): só energia + estado ativo

    /** ContainerData de 6 ints (energia lo/hi, max lo/hi, atividade, total) — ver {@link TechEnergyData}. */
    ContainerData getEnergyData();

    /** Handler do slot da GUI (1 slot) — pode ser null em layout PASSIVE. */
    IItemHandler getMenuSlots();

    int getLayoutId();
}
