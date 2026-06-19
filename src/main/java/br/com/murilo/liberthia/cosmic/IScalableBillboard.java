package br.com.murilo.liberthia.cosmic;

/**
 * r183 — entidade billboard cujo TAMANHO visual varia (ex.: cresce ao ser encarada).
 * O {@code client/renderer/BillboardEntityRenderer} multiplica o quad por este valor.
 * Interface em pacote comum (sem deps de cliente) → segura no servidor.
 */
public interface IScalableBillboard {
    float billboardScale();
}
