package com.panghu.food.web;

import com.panghu.food.dto.PetClaimRequest;
import com.panghu.food.dto.PetRenameRequest;
import com.panghu.food.dto.PetResponse;
import com.panghu.food.service.PetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pets")
public class PetController {
    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping("/me")
    public ResponseEntity<PetResponse> getMyPet() {
        return ResponseEntity.ok(petService.getMyPet());
    }

    @PostMapping("/claim")
    public ResponseEntity<PetResponse> claimPet(@RequestBody PetClaimRequest request) {
        return ResponseEntity.ok(petService.claimPet(request));
    }

    @PostMapping("/rename")
    public ResponseEntity<PetResponse> renamePet(@RequestBody PetRenameRequest request) {
        return ResponseEntity.ok(petService.renamePet(request));
    }
}
