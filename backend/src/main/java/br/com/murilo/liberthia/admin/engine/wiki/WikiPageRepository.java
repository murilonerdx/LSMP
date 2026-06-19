package br.com.murilo.liberthia.admin.engine.wiki;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WikiPageRepository extends JpaRepository<WikiPage, Long> {
    Optional<WikiPage> findBySlug(String slug);
    Page<WikiPage> findByStatusOrderByUpdatedAtDesc(String status, Pageable p);
    Page<WikiPage> findByCategoryOrderByUpdatedAtDesc(String category, Pageable p);
    Page<WikiPage> findAllByOrderByUpdatedAtDesc(Pageable p);

    /**
     * Busca por texto em título OU conteúdo.
     *
     * IMPORTANTE: O caller (WikiController) é responsável por:
     *   1. fazer toLowerCase no termo
     *   2. embrulhar com % (ex.: "%termo%")
     *
     * Antes a query usava LOWER(CONCAT('%', :q, '%')), mas o Hibernate 6
     * não consegue inferir o tipo do parâmetro dentro do CONCAT + LOWER,
     * jogando "FunctionArgumentException: Parameter 1 of function 'lower()'
     * has type 'STRING', but argument is of type 'java.lang.String'" no
     * startup do Spring (afeta CRIAÇÃO do bean, derruba app inteiro).
     */
    @Query("SELECT w FROM WikiPage w WHERE LOWER(w.title) LIKE :pattern OR LOWER(w.contentMarkdown) LIKE :pattern")
    Page<WikiPage> search(@Param("pattern") String pattern, Pageable p);
}
