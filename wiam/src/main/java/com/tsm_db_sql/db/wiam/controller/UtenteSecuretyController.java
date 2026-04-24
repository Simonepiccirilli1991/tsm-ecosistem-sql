package com.tsm_db_sql.db.wiam.controller;

import com.tsm_db_sql.db.wiam.model.request.ChangePswRequest;
import com.tsm_db_sql.db.wiam.model.request.RetrivePswStep1Request;
import com.tsm_db_sql.db.wiam.model.request.RetrivePswStep2Request;
import com.tsm_db_sql.db.wiam.model.request.RetrivePswStep3Request;
import com.tsm_db_sql.db.wiam.model.response.BaseResponse;
import com.tsm_db_sql.db.wiam.model.response.RetrivePswStep1Response;
import com.tsm_db_sql.db.wiam.model.response.RetrivePswStep2Response;
import com.tsm_db_sql.db.wiam.service.securety.ChangePswService;
import com.tsm_db_sql.db.wiam.service.securety.RetrivePswStep1Service;
import com.tsm_db_sql.db.wiam.service.securety.RetrivePswStep2Service;
import com.tsm_db_sql.db.wiam.service.securety.RetrivePswStep3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/utente/securety")
@RequiredArgsConstructor
public class UtenteSecuretyController {


    private final ChangePswService  changePswService;
    private final RetrivePswStep1Service retrivePswStep1Service;
    private final RetrivePswStep2Service retrivePswStep2Service;
    private final RetrivePswStep3Service retrivePswStep3Service;


    @PostMapping("changepsw")
    public ResponseEntity<BaseResponse> changePsw(@RequestBody ChangePswRequest request) {
        return ResponseEntity.ok(changePswService.changePsw(request));
    }

    @PostMapping("retrivestep1")
    public ResponseEntity<RetrivePswStep1Response> retriveStep1(@RequestBody RetrivePswStep1Request request) {
        return ResponseEntity.ok(retrivePswStep1Service.retrivePseStep1(request));
    }

    @PostMapping("retrivestep2")
    public ResponseEntity<RetrivePswStep2Response> retriveStep2(@RequestBody RetrivePswStep2Request request) {
        return ResponseEntity.ok(retrivePswStep2Service.retrivePswStep2(request));
    }

    @PostMapping("retrivestep3")
    public ResponseEntity<BaseResponse> retriveStep3(@RequestBody RetrivePswStep3Request request) {
        return ResponseEntity.ok(retrivePswStep3Service.retrivePswStep3(request));
    }

}
