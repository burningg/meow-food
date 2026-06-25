package com.panghu.food.web;

import com.panghu.food.dto.RawMaterialMatchRequest;
import com.panghu.food.dto.RawMaterialResponse;
import com.panghu.food.service.RawMaterialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/raw-materials")
public class RawMaterialController {
    private final RawMaterialService rawMaterialService;

    public RawMaterialController(RawMaterialService rawMaterialService) {
        this.rawMaterialService = rawMaterialService;
    }

    @PostMapping("/match")
    public ResponseEntity<List<RawMaterialResponse>> matchRawMaterials(@RequestBody RawMaterialMatchRequest request) {
        List<String> names = request == null ? List.of() : request.getNames();
        return ResponseEntity.ok(rawMaterialService.findMatchedRawMaterials(names));
    }
}
