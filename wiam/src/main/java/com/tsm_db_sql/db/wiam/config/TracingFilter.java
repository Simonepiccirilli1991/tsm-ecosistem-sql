package com.tsm_db_sql.db.wiam.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro per il tracing delle richieste HTTP tramite un identificativo univoco (tracingId).
 *
 * Come funziona:
 * 1. Ogni richiesta HTTP passa da questo filtro PRIMA di arrivare ai controller
 * 2. Il filtro cerca l'header "tracingId" nella request
 * 3. Se presente, lo usa; se assente, ne genera uno nuovo (UUID casuale)
 * 4. Inserisce il tracingId nel MDC (Mapped Diagnostic Context) di SLF4J
 * 5. Grazie al MDC, OGNI riga di log prodotta durante la richiesta conterrà
 *    automaticamente il tracingId — senza bisogno di passarlo manualmente ai metodi
 * 6. Il tracingId viene aggiunto anche alla response come header, così il client
 *    può usarlo per correlare la sua richiesta con i log del server
 *
 * Perché è utile:
 * - In un sistema con tante richieste concorrenti, i log si mescolano.
 *   Il tracingId permette di filtrare TUTTE le righe di log di una singola richiesta.
 * - Esempio: grep "tracingId=abc-123" nei log → trovi tutto il percorso della richiesta.
 */
@Component
@Slf4j
/*
 * @Order(Ordered.HIGHEST_PRECEDENCE): questo filtro deve essere il PRIMO ad eseguire.
 * Così il tracingId è disponibile nel MDC per tutti gli altri filtri e componenti
 * che potrebbero loggare durante la richiesta (es. Spring Security, altri filtri custom).
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TracingFilter implements Filter {

    /**
     * Nome dell'header HTTP e della chiave MDC usata per il tracing.
     * Usiamo una costante per evitare errori di battitura — se cambi il nome,
     * lo cambi in un solo punto.
     */
    private static final String TRACING_ID_HEADER = "tracingId";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        /*
         * Proviamo a leggere il tracingId dall'header della request.
         * Se il client lo ha inviato (es. un altro microservizio a monte),
         * lo riutilizziamo per mantenere la tracciabilità end-to-end.
         */
        String tracingId = request.getHeader(TRACING_ID_HEADER);

        if (tracingId == null || tracingId.isBlank()) {
            /*
             * Se il client non ha inviato un tracingId, ne generiamo uno nuovo.
             * UUID.randomUUID() genera un ID univoco praticamente impossibile da duplicare.
             */
            tracingId = UUID.randomUUID().toString();
        }

        try {
            /*
             * MDC.put() inserisce il tracingId nel contesto diagnostico del thread corrente.
             * Da questo momento, ogni log.info/error/warn prodotto su QUESTO thread
             * includerà automaticamente il tracingId nel messaggio (grazie al pattern
             * %X{tracingId} configurato in application.yaml).
             *
             * Il MDC funziona come una mappa thread-local: ogni thread ha il suo contesto,
             * quindi richieste diverse su thread diversi non si "mischiano".
             */
            MDC.put(TRACING_ID_HEADER, tracingId);

            /*
             * Aggiungiamo il tracingId come header nella response.
             * Così il client che ha fatto la richiesta può leggerlo e usarlo
             * per chiedere supporto: "ho un errore, il mio tracingId è XYZ".
             */
            response.setHeader(TRACING_ID_HEADER, tracingId);

            log.info("Richiesta ricevuta: {} {} [tracingId={}]",
                    request.getMethod(), request.getRequestURI(), tracingId);

            /*
             * filterChain.doFilter() passa la richiesta al prossimo filtro nella catena
             * (o al controller se non ci sono altri filtri). TUTTO il codice che esegue
             * dopo questa chiamata (controller, service, repository) avrà il tracingId nel MDC.
             */
            filterChain.doFilter(servletRequest, servletResponse);

        } finally {
            /*
             * IMPORTANTE: puliamo il MDC nel blocco finally.
             * I server usano un pool di thread: lo stesso thread può gestire richieste
             * diverse in momenti diversi. Se non puliamo, il tracingId della richiesta
             * precedente potrebbe "leakare" nella richiesta successiva sullo stesso thread.
             *
             * Il finally garantisce che la pulizia avviene SEMPRE, anche se c'è un'eccezione.
             */
            MDC.remove(TRACING_ID_HEADER);
        }
    }
}
