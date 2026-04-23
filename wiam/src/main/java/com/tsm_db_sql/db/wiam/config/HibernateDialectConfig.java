package com.tsm_db_sql.db.wiam.config;

import org.hibernate.community.dialect.SQLiteDialect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configurazione del dialect Hibernate per SQLite.
 *
 * Problema: Hibernate usa un classloader interno (AggregatedClassLoader) per caricare
 * il dialect. In alcuni ambienti (es. IntelliJ IDEA), questo classloader NON riesce
 * a trovare la classe SQLiteDialect dal jar hibernate-community-dialects, anche se
 * il jar è presente nelle dipendenze Maven.
 *
 * Soluzione: invece di specificare il dialect come stringa nel YAML (es. database-platform),
 * lo registriamo programmaticamente passando direttamente l'ISTANZA della classe.
 * In questo modo è il classloader di Spring (che funziona correttamente) a caricare
 * la classe, non quello interno di Hibernate.
 *
 * @ConditionalOnProperty: questo bean si attiva SOLO quando il driver è org.sqlite.JDBC.
 * Così durante i test (che usano H2) non viene caricato, e H2 usa il suo dialect nativo.
 */
@Configuration
@ConditionalOnProperty(name = "spring.datasource.driver-class-name", havingValue = "org.sqlite.JDBC")
public class HibernateDialectConfig {

    @Bean
    public HibernatePropertiesCustomizer hibernateDialectCustomizer() {
        /*
         * "hibernate.dialect" accetta sia una stringa (nome classe) sia un oggetto Dialect.
         * Passando un'istanza concreta, evitiamo completamente il problema di classloading
         * perché la classe è già stata caricata da Spring al momento della creazione del bean.
         */
        return properties -> properties.put("hibernate.dialect", new SQLiteDialect());
    }
}
