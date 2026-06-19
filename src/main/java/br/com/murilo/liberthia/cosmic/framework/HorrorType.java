package br.com.murilo.liberthia.cosmic.framework;

/**
 * v0.1.24 r81: 18 categorias de horror psicológico/cósmico que compõem o
 * framework unificado de horror Liberthia.
 *
 * <p>Cada tipo tem um {@link br.com.murilo.liberthia.cosmic.framework.HorrorSystem}
 * dedicado que tickea por player, contribuindo pra exposição cumulativa
 * armazenada no {@link HorrorState}.
 *
 * <p>Cada player tem um <b>perfil de exposição</b> independente — o horror que
 * te afeta depende do tempo/lugar/itens/comportamento. Diferentes players podem
 * estar em fases diferentes do mesmo horror simultaneamente.
 */
public enum HorrorType {
    /** Lovecraftian — entidades incompreensíveis dormindo entre dimensões. */
    COSMIC("Horror Cósmico"),
    /** Algo parece humano, mas <i>errado</i>. Empty Man, Mirror Mask. */
    UNCANNY("Vale da Estranheza"),
    /** Realidade é falsa. Déjà vu, loops, livre arbítrio ilusório. */
    EXISTENTIAL("Existencial"),
    /** Realidade física falhando. Blocos sozinhos, biomas mudando. */
    ONTOLOGICAL("Ontológico"),
    /** Mente não processa. Símbolos proibidos, glitch perceptual. */
    COGNITIVE("Cognitivo"),
    /** Idéias que se espalham. Entidades existem se forem lembradas. */
    MEMETIC("Memético"),
    /** Tempo quebrado. Loops, futuro morto, ecos do passado. */
    TEMPORAL("Temporal"),
    /** Dimensões sangrando. Portais impossíveis, biomas sobrepostos. */
    DIMENSIONAL("Dimensional"),
    /** Carne viva. Crescimentos orgânicos, paredes que respiram. */
    FLESH("Carne"),
    /** Angelical/divino. Olhos no céu, coral cósmico, geometria sagrada. */
    RELIGIOUS("Religioso"),
    /** VHS, broadcasts de emergência, transmissões corrompidas. */
    ANALOG("Analog"),
    /** Espaços vazios em transição. Corredores, subúrbios silenciosos. */
    LIMINAL("Liminal"),
    /** Cosmos vivo. Luas que olham, estrelas senciantes, eclipses. */
    ASTRONOMICAL("Astronômico"),
    /** Criaturas que dependem de observação. Movem só quando não vistas. */
    PERCEPTION("Percepção"),
    /** Matemática proibida. Fractais, ângulos impossíveis, recursão. */
    GEOMETRIC("Geométrico"),
    /** Nada absoluto. Silêncio total, ausência detectável. */
    VOID("Vazio"),
    /** Infecção inteligente. Mente coletiva, raízes vivas, parasitas. */
    PARASITIC("Parasita"),
    /** Sonhos, almas, posse espiritual, consciência sobreposta. */
    PSYCHOSPIRITUAL("Psicoespiritual");

    private final String displayName;

    HorrorType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
