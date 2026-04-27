package com.tsm_db_sql.db.wiam.controller;

import com.tsm_db_sql.db.wiam.model.request.AggiungiAcquistoRequest;
import com.tsm_db_sql.db.wiam.model.request.AggiungiVenditaRequest;
import com.tsm_db_sql.db.wiam.model.request.CancellaAcquistoRequest;
import com.tsm_db_sql.db.wiam.model.request.CancellaVenditaRequest;
import com.tsm_db_sql.db.wiam.model.response.BaseResponse;
import com.tsm_db_sql.db.wiam.service.inventario.AggiungiAcquistoService;
import com.tsm_db_sql.db.wiam.service.inventario.AggiungiVenditaService;
import com.tsm_db_sql.db.wiam.service.inventario.CancellaAcquistoService;
import com.tsm_db_sql.db.wiam.service.inventario.CancellaVenditaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST per la gestione dell'inventario.
 * Espone gli endpoint per il ciclo di vita degli item:
 * acquisto → vendita → annullo vendita / cancellazione acquisto.
 */
@RestController
@RequestMapping("api/v1/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final AggiungiAcquistoService aggiungiAcquistoService;
    private final AggiungiVenditaService aggiungiVenditaService;
    private final CancellaVenditaService cancellaVenditaService;
    private final CancellaAcquistoService cancellaAcquistoService;

    @PostMapping("acquisto")
    public ResponseEntity<BaseResponse> aggiungiAcquisto(@RequestBody AggiungiAcquistoRequest request) {
        return ResponseEntity.ok(aggiungiAcquistoService.aggiungiAcquisto(request));
    }

    @PutMapping("vendita")
    public ResponseEntity<BaseResponse> aggiungiVendita(@RequestBody AggiungiVenditaRequest request) {
        return ResponseEntity.ok(aggiungiVenditaService.aggiungiVendita(request));
    }

    @PutMapping("cancella-vendita")
    public ResponseEntity<BaseResponse> cancellaVendita(@RequestBody CancellaVenditaRequest request) {
        return ResponseEntity.ok(cancellaVenditaService.cancellaVendita(request));
    }

    @PutMapping("cancella-acquisto")
    public ResponseEntity<BaseResponse> cancellaAcquisto(@RequestBody CancellaAcquistoRequest request) {
        return ResponseEntity.ok(cancellaAcquistoService.cancellaAcquisto(request));
    }
}
