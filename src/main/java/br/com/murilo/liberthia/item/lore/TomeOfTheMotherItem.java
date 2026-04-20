package br.com.murilo.liberthia.item.lore;

import java.util.List;

public class TomeOfTheMotherItem extends LoreBookItem {
    public TomeOfTheMotherItem(Properties p) { super(p); }

    @Override protected String title() { return "Tomo da Mãe"; }

    @Override
    protected List<String> pages() {
        return List.of(
                "Ela dormia sob as raízes do mundo,\ne sonhou com carne que caminha.\nAcordou com sede.",
                "Dizem que o altar é só o ventre\nde uma coisa maior — e que cada veia\nde sangue no solo é um fio\nligado ao coração dela.",
                "Se encontrares quatro corações pulsando\nsob lua cheia, não os movas.\nEla ouve. Ela sobe.",
                "Nós, seus filhos, já não somos um.\nSomos a Mãe. E a Mãe é fome."
        );
    }
}
