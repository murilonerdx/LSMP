package br.com.murilo.liberthia.observation.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * v0.1.22 r60: <b>ObservationSpell</b> — recipe imutável, inspired by AN's
 * {@code Spell}.
 *
 * <p>Estrutura: lista ordenada de {@link ObservationPart}. Sempre começa com
 * um {@link WatchMethod}, seguido de {@link Manifestation}s + {@link Distortion}s
 * intercalados.
 *
 * <h2>Validação</h2>
 * Recipe DEVE:
 * <ul>
 *   <li>Começar com WatchMethod</li>
 *   <li>Conter pelo menos 1 Manifestation</li>
 *   <li>Distortions sempre seguem uma Manifestation/Distortion (não no início)</li>
 * </ul>
 */
public final class ObservationSpell {

    private final List<ObservationPart> recipe;
    private final int color;
    private final String name;

    public ObservationSpell(List<ObservationPart> recipe, int color, String name) {
        this.recipe = Collections.unmodifiableList(new ArrayList<>(recipe));
        this.color = color;
        this.name = name;
    }

    public List<ObservationPart> recipe() { return recipe; }
    public int color() { return color; }
    public String name() { return name; }

    /**
     * Coleta TODAS as Distortions que seguem o índice {@code start} até
     * encontrar um non-Distortion. Mesmo pattern do AN's {@code Spell.getAugments}.
     */
    public List<Distortion> distortionsAfter(int start) {
        List<Distortion> result = new ArrayList<>();
        for (int i = start + 1; i < recipe.size(); i++) {
            ObservationPart p = recipe.get(i);
            if (p instanceof Distortion d) {
                result.add(d);
            } else {
                break;
            }
        }
        return result;
    }

    /** Custo total em sanidade (base + distortions). */
    public int totalSanityCost() {
        int total = 0;
        for (int i = 0; i < recipe.size(); i++) {
            ObservationPart part = recipe.get(i);
            if (part instanceof Distortion d) {
                // Find parent (closest non-distortion before)
                ObservationPart parent = null;
                for (int j = i - 1; j >= 0; j--) {
                    if (!(recipe.get(j) instanceof Distortion)) {
                        parent = recipe.get(j);
                        break;
                    }
                }
                total += d.sanityCostForPart(parent);
            } else {
                total += part.sanityCost();
            }
        }
        return total;
    }

    /** r61: Custo total em Source (mana cósmica). */
    public int totalSourceCost() {
        int total = 0;
        for (ObservationPart part : recipe) {
            total += part.sourceCost();
        }
        return total;
    }

    /** Validação básica. Retorna null se válido, mensagem de erro se inválido. */
    public String validate() {
        if (recipe.isEmpty()) return "Recipe vazio";
        if (!(recipe.get(0) instanceof WatchMethod)) return "Recipe deve começar com WatchMethod";
        boolean hasManifestation = false;
        for (ObservationPart p : recipe) {
            if (p instanceof Manifestation) {
                hasManifestation = true;
                break;
            }
        }
        if (!hasManifestation) return "Recipe precisa de pelo menos 1 Manifestation";
        return null;
    }

    public static Builder builder(String name, int color) {
        return new Builder(name, color);
    }

    public static final class Builder {
        private final List<ObservationPart> recipe = new ArrayList<>();
        private final String name;
        private final int color;

        Builder(String name, int color) {
            this.name = name;
            this.color = color;
        }

        public Builder add(ObservationPart part) {
            recipe.add(part);
            return this;
        }

        public ObservationSpell build() {
            return new ObservationSpell(recipe, color, name);
        }
    }
}
