package com.panghu.food.service;

import com.panghu.food.dto.PetClaimRequest;
import com.panghu.food.dto.PetResponse;

public interface PetService {
    PetResponse getMyPet();

    PetResponse claimPet(PetClaimRequest request);
}
