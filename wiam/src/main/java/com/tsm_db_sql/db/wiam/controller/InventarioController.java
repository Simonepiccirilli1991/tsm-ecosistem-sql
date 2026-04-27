package com.tsm_db_sql.db.wiam.controller;

import com.tsm_db_sql.db.wiam.model.request.AggiungiAcquistoRequest;
import com.tsm_db_sql.db.wiam.model.request.AggiungiVenditaRequest;
import com.tsm_db_sql.db.wiam.model.request.CancellaAcquistoRequest;
import com.tsm_db_sql.db.wiam.model.request.CancellaVenditaRequest;
import com.tsm_db_sql.db.wiam.model.request.InventarioInquiryRequest;
import com.tsm_db_sql.db.wiam.model.response.BaseResponse;
import com.tsm_db_sql.db.wiam.model.response.InventarioInquiryResponse;
import com.tsm_db_sql.db.wiam.service.inventario.AggiungiAcquistoService;
import com.tsm_db_sql.db.wiam.service.inventario.AggiungiVenditaService;
import com.tsm_db_sql.db.wiam.service.inventario.CancellaAcquistoService;
import com.tsm_db_sql.db.wiam.service.inventario.CancellaVenditaService;
import com.tsm_db_sql.db.wiam.service.inventario.InventarioInquiryService;
import com.tsm_db_sql.db.wiam.service.inventario.InventarioInquiryV2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST per la gestione dell'inventario.
 * Espone gli endpoint per il ciclo di vita degli item:
 * acquisto → vendita → annullo vendita / cancellazione acquisto.
 * Include anche l'inquiry per la ricerca multi-criterio.
 */
@RestController
@RequestMapping("api/v1/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final AggiungiAcquistoService aggiungiAcquistoService;
    private final AggiungiVenditaService aggiungiVenditaService;
    private final CancellaVenditaService cancellaVenditaService;
    private final CancellaAcquistoService cancellaAcquistoService;
    private final InventarioInquiryService inventarioInquiryService;
    private final InventarioInquiryV2Service inventarioInquiryV2Service;

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

    @PostMapping("inquiry")
    public ResponseEntity<Page<InventarioInquiryResponse>> inquiry(@RequestBody InventarioInquiryRequest request) {
        return ResponseEntity.ok(inventarioInquiryService.inquiry(request));
    }

    @PostMapping("inquiry-v2")
    public ResponseEntity<Page<InventarioInquiryResponse>> inquiryV2(@RequestBody InventarioInquiryRequest request) {
        return ResponseEntity.ok(inventarioInquiryV2Service.inquiry(request));
    }
}
