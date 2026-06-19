package br.com.murilo.liberthia.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Backend de administração do Liberthia.
 *
 * @EntityScan + @EnableJpaRepositories explicitos pra garantir que TODAS
 * as entities em sub-packages (engine.*) sejam descobertas — não confiar só
 * em auto-discovery do Spring Boot.
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync
@EntityScan(basePackages = "br.com.murilo.liberthia.admin")
@EnableJpaRepositories(
        basePackages = "br.com.murilo.liberthia.admin",
        // considerNestedRepositories=true permite repositories declarados
        // como inner interfaces dentro de Services (ex: VoiceRetentionService.Repo).
        // Sem isso, Spring só descobre interfaces top-level e dá "required a
        // bean of type 'X$Repo' that could not be found".
        considerNestedRepositories = true)
public class LiberthiaAdminApplication {
    public static void main(String[] args) {
        // Força TTL curto de DNS na JVM ANTES do Spring subir.
        // Por padrão a JVM em produção (security manager habilitado) cacheia
        // DNS PRA SEMPRE — isso quebrou produção em 2026-05-19 quando o IP
        // dinâmico do servidor MC mudou e o backend ficou apontando pro IP
        // velho até reiniciar. Setando aqui é defensa em profundidade: mesmo
        // se alguém remover o -Dnetworkaddress.cache.ttl do JAVA_OPTS, o
        // backend ainda re-resolve DNS a cada 30s. System.setProperty()
        // funciona porque essa main() roda ANTES de qualquer InetAddress
        // resolver inicializar.
        if (System.getProperty("networkaddress.cache.ttl") == null) {
            System.setProperty("networkaddress.cache.ttl", "30");
        }
        if (System.getProperty("networkaddress.cache.negative.ttl") == null) {
            System.setProperty("networkaddress.cache.negative.ttl", "10");
        }
        if (System.getProperty("sun.net.inetaddr.ttl") == null) {
            System.setProperty("sun.net.inetaddr.ttl", "30");
        }
        SpringApplication.run(LiberthiaAdminApplication.class, args);
    }
}
