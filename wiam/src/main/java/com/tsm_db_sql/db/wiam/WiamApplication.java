package com.tsm_db_sql.db.wiam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class WiamApplication {

	public static void main(String[] args) throws IOException {
		/*
		 * Creiamo la cartella "data/" PRIMA di avviare Spring.
		 * SQLite crea il file .db automaticamente, ma NON le directory padre.
		 * Se la cartella non esiste, l'app fallisce con SQLITE_CANTOPEN.
		 *
		 * Lo facciamo nel main() perché Hibernate prova a connettersi durante
		 * l'inizializzazione dei bean Spring (EntityManagerFactory), che avviene
		 * PRIMA di qualsiasi ApplicationRunner o @PostConstruct.
		 * Il main() è l'unico punto sicuro che esegue prima di tutto.
		 */
		Files.createDirectories(Path.of("./data"));
		SpringApplication.run(WiamApplication.class, args);
	}

}
