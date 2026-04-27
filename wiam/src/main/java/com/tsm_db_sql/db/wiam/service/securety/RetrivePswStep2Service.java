package com.tsm_db_sql.db.wiam.service.securety;


import com.tsm_db_sql.db.wiam.exception.UtenteException;
import com.tsm_db_sql.db.wiam.exception.UtenteSecuretyException;
import com.tsm_db_sql.db.wiam.model.request.RetrivePswStep2Request;
import com.tsm_db_sql.db.wiam.model.response.RetrivePswStep2Response;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RetrivePswStep2Service {

    private final UtenteRepository utenteRepository;

    @Value("${wiam.otp.secret-key}")
    private String otpSecretKey;



    public RetrivePswStep2Response retrivePswStep2(RetrivePswStep2Request request){
        log.info("RetrivePswStep2 service started for utente: {}",request.username());

        // controllo se esiste utente
        var utente = utenteRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.error("RetrivePswStep2 service error Utente non trovato: {}", request.username());
                    return new UtenteException("Utente non trovato","ERR-UT-404");
                });
        var utenteSecurety = utente.getUtenteSecurety();
        // controllo che otp sia ancora valido, valido per 2 minuti
        if(utenteSecurety.getOtpTimeRequest().plusMinutes(2).isBefore(LocalDateTime.now())) {
            log.error("Otp time request scaduto per utente: {}", request.username());
            throw new UtenteSecuretyException("Otp scaduto","ERR-SEC-401");
        }
        var otpDecifrato = decifraOtp(request.otp());
        // controllo che otp machina
        if(!utenteSecurety.getOtp().equals(otpDecifrato)){
            log.error("Otp non valido per utente: {}", request.username());
            throw new UtenteSecuretyException("Otp non valido","ERR-SEC-402");
        }
        // genero codice da passare al seor per step3
        var step3Chiave = UUID.randomUUID().toString();
        utenteSecurety.setChiaveStep3(step3Chiave);
        // aggiunto gia tempo massimo di validita inserento il lus 10 minutes
        utenteSecurety.setDurataChiaveStep3(LocalDateTime.now().plusMinutes(10));
        // aggiorno utente securety
        utente.setUtenteSecurety(utenteSecurety);
        // salvo entity principale
        utenteRepository.save(utente);
        log.info("RetrivePswStep2 service ended successfully for utente: {}",utente.getUsername());
        return new RetrivePswStep2Response(step3Chiave);
    }

    private String decifraOtp(String otpCifratoBase64) {
        try {
            // Recupera la chiave segreta — deve essere identica a quella usata per cifrare
            byte[] keyBytes = otpSecretKey.getBytes(StandardCharsets.UTF_8);

            // Decodifica la stringa Base64 per ottenere i byte grezzi (IV + OTP cifrato)
            byte[] dati = Base64.getDecoder().decode(otpCifratoBase64);

            // Estrae i primi 12 byte che corrispondono all'IV usato durante la cifratura
            byte[] iv = new byte[12];
            System.arraycopy(dati, 0, iv, 0, iv.length);

            // Estrae i byte rimanenti che corrispondono all'OTP effettivamente cifrato
            byte[] otpCifrato = new byte[dati.length - iv.length];
            System.arraycopy(dati, iv.length, otpCifrato, 0, otpCifrato.length);

            // Ricostruisce la chiave AES dagli stessi byte della chiave segreta
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

            // Inizializza il cifrario in modalità AES/GCM — stesso algoritmo usato per cifrare
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            // Configura i parametri GCM con lo stesso IV estratto dal payload
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

            // Inizializza il cifrario in modalità DECRYPT con chiave e IV originali
            // GCM verifica automaticamente l'integrità: se i dati sono stati manomessi lancia un'eccezione
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec);

            // Decifra i byte e ottiene l'OTP originale in chiaro
            byte[] otpDecifrato = cipher.doFinal(otpCifrato);

            // Converte i byte in stringa UTF-8 e restituisce l'OTP originale
            return new String(otpDecifrato, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Errore durante la decifratura dell'OTP", e);
            throw new UtenteSecuretyException("Errore decifratura OTP", "ERR-SEC-500");
        }
    }

}
