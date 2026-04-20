package br.com.murilo.liberthia.item.lore;

import java.util.List;

public class TomeOfThePilgrimItem extends LoreBookItem {
    public TomeOfThePilgrimItem(Properties p) { super(p); }

    @Override protected String title() { return "Diário do Peregrino"; }

    @Override
    protected List<String> pages() {
        return List.of(
                "Fui da Ordem. Achei que a luz\nqueimava o mal — até ver a luz\nqueimar os meus.",
                "Quando o santuário caiu, os irmãos\nme chamaram de traidor por perguntar.\nNão por crer: por perguntar.",
                "Se encontrares um peregrino ferido,\nnão o mates. Ele já foi morto uma vez\npelos que juravam salvá-lo."
        );
    }
}
