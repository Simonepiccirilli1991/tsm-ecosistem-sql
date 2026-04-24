package com.tsm_db_sql.db.wiam.service.securety;


import com.tsm_db_sql.db.wiam.exception.UtenteException;
import com.tsm_db_sql.db.wiam.exception.UtenteSecuretyException;
import com.tsm_db_sql.db.wiam.model.request.RetrivePswStep1Request;
import com.tsm_db_sql.db.wiam.model.response.RetrivePswStep1Response;
import com.tsm_db_sql.db.wiam.repository.UtenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class RetrivePswStep1Service {

    private final UtenteRepository utenteRepository;
    private Random random = new Random();

    @Value("${wiam.otp.secret-key}")
    private String otpSecretKey;

    public RetrivePswStep1Response retrivePseStep1(RetrivePswStep1Request request){
        log.info("RetrivePswStep1 service started for utente: {}",request.username());

        // controllo se esiste utente
        var utente = utenteRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.error("RetrivePswStep1 service error Utente non trovato: {}", request.username());
                    return new UtenteException("Utente non trovato","ERR-UT-404");
                });

        var utenteOtpCounter = utente.getUtenteSecurety().getOtpCounter();

        // caso otp == 5 ma time counter non su puo resettare lancio eccezzione
        if(utenteOtpCounter >= 5 && (LocalDateTime.now().isAfter(utente.getUtenteSecurety().getDataRichiestaUltimoOtp().plusDays(1)))){
          log.error("Error on RetrivePswStep1 service: otp giornaliero massimo raggiunto");
          throw new UtenteSecuretyException("Error on RetrivePswStep1 service, max otp requested","ERR-UT-401");
        }
        // genero otp
        var otp = generaOtp();
        // cripto otp
        var otpCriptato = cifraOtp(otp);
        // potrebbe essere la prima volta che richiede otp, se data richiesta ultimo otp e null setto a 24 ore prima per far si che possa richiedere subito, altrimenti lascio la data precedente
        var dataOtpUltimaRic = (ObjectUtils.isEmpty(utente.getUtenteSecurety().getDataRichiestaUltimoOtp())) ? LocalDateTime.now().minusDays(1) : utente.getUtenteSecurety().getDataRichiestaUltimoOtp();
        // calcolo counter, se possono passate 24 ore setto a 1, altrimenti aumento dal precedente, logica e a 24 ore da ultima richiesta, non giornaliera su 24
        var counter =(LocalDateTime.now().isBefore(dataOtpUltimaRic.plusDays(1))) ? 1 : utenteOtpCounter + 1;
        utente.getUtenteSecurety().setOtp(otp);
        utente.getUtenteSecurety().setOtpCounter(counter);
        utenteRepository.save(utente);
        log.info("RetrivePswStep1 service ended successfully for utente: {}",utente.getUsername());
        return new RetrivePswStep1Response(utente.getEmail(), otpCriptato);
    }


    private String generaOtp(){
        String caratteri = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder otp = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            otp.append(caratteri.charAt(random.nextInt(caratteri.length())));
        }
        return otp.toString();
    }

    private String cifraOtp(String otp) {
        try {
            // Recupera la chiave segreta dalla configurazione (32 caratteri = 256 bit per AES)
            byte[] keyBytes = otpSecretKey.getBytes(StandardCharsets.UTF_8);

            // Costruisce la chiave AES dai byte della chiave segreta
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

            // Inizializza il cifrario con algoritmo AES in modalità GCM (garantisce cifratura + integrità)
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            // Genera un IV (Initialization Vector) casuale di 12 byte — obbligatorio in GCM
            // L'IV garantisce che cifrando lo stesso OTP due volte il risultato sia sempre diverso
            byte[] iv = new byte[12];
            random.nextBytes(iv);

            // Configura i parametri GCM: 128 bit di tag di autenticazione + IV generato
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

            // Inizializza il cifrario in modalità ENCRYPT con chiave e parametri GCM
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec);

            // Cifra l'OTP e ottiene i byte cifrati
            byte[] otpCifrato = cipher.doFinal(otp.getBytes(StandardCharsets.UTF_8));

            // Concatena IV + OTP cifrato in un unico array di byte
            // L'IV viene preposto perché il destinatario ne ha bisogno per decifrare
            byte[] risultato = new byte[iv.length + otpCifrato.length];
            System.arraycopy(iv, 0, risultato, 0, iv.length);
            System.arraycopy(otpCifrato, 0, risultato, iv.length, otpCifrato.length);

            // Codifica il risultato in Base64 per poterlo trasportare come stringa nella response JSON
            return Base64.getEncoder().encodeToString(risultato);

        } catch (Exception e) {
            log.error("Errore durante la cifratura dell'OTP", e);
            throw new UtenteSecuretyException("Errore cifratura OTP", "ERR-SEC-500");
        }
    }
}
