package br.com.murilo.liberthia.block.entity.arcane;

/**
 * r184 — config de cada máquina Tecno/Tecno-Arcano. Um BlockEntity/Menu/Screen genéricos
 * são parametrizados por este enum (slots in/out, FE/tick, tempo, cor de destaque, precisa energia).
 * Slots: entradas = índices 0..inputSlots-1; saídas = inputSlots..inputSlots+outputSlots-1.
 */
public enum ArcaneMachineType {
    //                  in out  fe/t  tempo  cor(ARGB)    energia
    METAL_PRESS(         1, 1,   40,   120,   0xFF8AA0B8, true),   // aço -> placa endurecida
    ARCANE_INFUSER(      2, 1,   80,   200,   0xFFB060E0, true),   // placa + matéria escura -> placa bastião
    TECH_ASSEMBLER(      3, 1,  100,   240,   0xFF40C0FF, true),   // componentes -> módulo
    MANA_CONDENSER(      1, 1,   60,   160,   0xFFE060C0, true),   // cristal mágico -> capacitor de mana
    CRYSTAL_SMELTER(     2, 1,   80,   180,   0xFF40D0B0, true),   // capacitor + ingot -> liga arcana
    // r188 — Lote de 16 máquinas novas (cadeia tech/tecno-arcano avançada)
    QUANTUM_PULVERIZER(      1, 1,  60, 120, 0xFF60A0FF, true),
    CRYSTALLIZATION_CHAMBER( 1, 1,  80, 200, 0xFF80E0FF, true),
    COIL_WINDER(             2, 1,  50, 150, 0xFFC08040, true),
    MATTER_CONDENSER_T(      2, 1, 120, 200, 0xFF6030A0, true),
    NANO_ASSEMBLER(          2, 1, 150, 300, 0xFF40FFC0, true),
    ENERGY_DISTILLER(        1, 1,  40, 100, 0xFFFF4040, true),
    FLUX_FORGE(              2, 1,  70, 160, 0xFFFFA030, true),
    MANA_CRYSTALLIZER(       1, 1,  50, 140, 0xFFE060E0, true),
    ARCANE_CIRCUIT_PRINTER(  2, 1,  80, 180, 0xFF40C0FF, true),
    DARK_ALLOY_SMELTER(      2, 1,  90, 200, 0xFF402060, true),
    PHOTON_INFUSER(          2, 1,  80, 180, 0xFFFFF080, true),
    MATTER_REPLICATOR(       1, 1, 200, 400, 0xFF2080FF, true),
    CRYSTAL_GROWER(          1, 1,  60, 160, 0xFFB060FF, true),
    ESSENCE_COMPRESSOR(      1, 1,  70, 150, 0xFFFF6060, true),
    RUNE_ETCHER(             2, 1,  80, 200, 0xFF2BD6D6, true),
    SINGULARITY_PRESS(       2, 1, 250, 500, 0xFF301050, true),
    // r189 — +18 máquinas (fecha os 40 blocos com UI)
    ARCANE_COLLECTOR(        1, 1,  50, 140, 0xFFD060FF, true),
    MANA_REACTOR(            2, 1,  70, 160, 0xFFE040A0, true),
    CRYSTAL_RESONATOR(       1, 1,  60, 150, 0xFF80E0FF, true),
    ENDER_CONDENSER(         1, 1,  60, 150, 0xFF20A080, true),
    SOUL_EXTRACTOR(          1, 1,  50, 140, 0xFF5080A0, true),
    BLAZE_REACTOR(           1, 1,  60, 150, 0xFFFF8020, true),
    MATTER_FABRICATOR(       2, 1, 160, 300, 0xFF3060C0, true),
    CIRCUIT_ASSEMBLER(       2, 1,  60, 150, 0xFF40C080, true),
    PLATE_PRESS(             1, 1,  40, 100, 0xFF8AA0B8, true),
    WIRE_DRAWER(             1, 1,  40, 100, 0xFFC08040, true),
    GEM_POLISHER(            1, 1,  70, 160, 0xFF80FFFF, true),
    INGOT_FORMER(            1, 1,  60, 150, 0xFF504070, true),
    ARCANE_SYNTHESIZER(      3, 1, 120, 260, 0xFF40FFC0, true),
    FLUX_DYNAMO(             2, 1,  80, 180, 0xFFFFD040, true),
    SHARD_SPLITTER(          1, 1,  80, 180, 0xFF202840, true),
    COSMIC_DISTILLER(        2, 1, 120, 240, 0xFF6030A0, true),
    RUNE_INSCRIBER(          2, 1,  80, 200, 0xFF2BD6D6, true),
    SINGULARITY_CORE_FORGE(  2, 1, 250, 500, 0xFF402060, true),
    ;

    public final int inputSlots, outputSlots, fePerTick, processTime, accentColor;
    public final boolean needsEnergy;

    ArcaneMachineType(int in, int out, int fe, int time, int color, boolean energy) {
        this.inputSlots = in; this.outputSlots = out;
        this.fePerTick = fe; this.processTime = time;
        this.accentColor = color; this.needsEnergy = energy;
    }

    public int totalSlots() { return inputSlots + outputSlots; }
}
