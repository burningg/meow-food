package com.panghu.food.service;

import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.PetClaimRequest;
import com.panghu.food.dto.PetRenameRequest;
import com.panghu.food.dto.PetResponse;
import com.panghu.food.entity.UserPet;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.CirclePlanMapper;
import com.panghu.food.mapper.DishMapper;
import com.panghu.food.mapper.UserPetMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class PetServiceImplTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-06-20T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    private final UserPetMapper userPetMapper = mock(UserPetMapper.class);
    private final DishMapper dishMapper = mock(DishMapper.class);
    private final CirclePlanMapper circlePlanMapper = mock(CirclePlanMapper.class);
    private final PetServiceImpl petService = new PetServiceImpl(userPetMapper, dishMapper, circlePlanMapper);

    PetServiceImplTest() {
        ReflectionTestUtils.setField(petService, "clock", FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void getMyPetReturnsUnclaimedWhenUserHasNoPet() {
        AuthContext.setUserId("owner");
        when(userPetMapper.selectOne(any())).thenReturn(null);

        PetResponse response = petService.getMyPet();

        assertThat(response.isClaimed()).isFalse();
    }

    @Test
    void claimPetCreatesPetWithReservedExperience() {
        AuthContext.setUserId("owner");
        when(userPetMapper.selectOne(any())).thenReturn(null);
        when(userPetMapper.insert(any(UserPet.class))).thenAnswer(invocation -> {
            UserPet pet = invocation.getArgument(0);
            pet.setId("pet-1");
            return 1;
        });
        when(circlePlanMapper.countRecipesCreatedByUserInPlanDateRange(any(), any(), any())).thenReturn(0L);

        PetClaimRequest request = new PetClaimRequest();
        request.setPetType("tabby_cat");
        request.setName("饭团");
        PetResponse response = petService.claimPet(request);

        assertThat(response.isClaimed()).isTrue();
        assertThat(response.getPetType()).isEqualTo("tabby_cat");
        assertThat(response.getPetTypeName()).isEqualTo("狸花猫");
        assertThat(response.getName()).isEqualTo("饭团");
        assertThat(response.getExperience()).isZero();
        assertThat(response.getMoodCode()).isEqualTo("happy");
    }

    @Test
    void claimPetRejectsUnknownType() {
        AuthContext.setUserId("owner");
        when(userPetMapper.selectOne(any())).thenReturn(null);

        PetClaimRequest request = new PetClaimRequest();
        request.setPetType("dragon");
        request.setName("饭团");

        assertThatThrownBy(() -> petService.claimPet(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("暂不支持这个宠物");
    }

    @Test
    void claimPetRejectsDuplicatePet() {
        AuthContext.setUserId("owner");
        when(userPetMapper.selectOne(any())).thenReturn(pet("pet-1", "owner", "tabby_cat", "饭团"));

        PetClaimRequest request = new PetClaimRequest();
        request.setPetType("corgi");
        request.setName("年糕");

        assertThatThrownBy(() -> petService.claimPet(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("你已经领取宠物了");
    }

    @Test
    void getMyPetDefaultsMoodToHappyWithoutRecipeOrPlan() {
        AuthContext.setUserId("owner");
        when(userPetMapper.selectOne(any())).thenReturn(pet("pet-1", "owner", "tabby_cat", "饭团"));
        when(circlePlanMapper.countRecipesCreatedByUserInPlanDateRange(any(), any(), any())).thenReturn(0L);

        PetResponse response = petService.getMyPet();

        assertThat(response.getMoodCode()).isEqualTo("happy");
        assertThat(response.getMoodLabel()).isEqualTo("开心");
    }

    @Test
    void getMyPetReturnsBoredWhenLatestActivityIsOutsideFifteenDayWindow() {
        AuthContext.setUserId("owner");
        when(userPetMapper.selectOne(any())).thenReturn(pet("pet-1", "owner", "tabby_cat", "饭团"));
        when(dishMapper.selectLatestCreatedAtByOwnerUserId("owner"))
                .thenReturn(LocalDateTime.of(2026, 6, 5, 12, 0));
        when(circlePlanMapper.countRecipesCreatedByUserInPlanDateRange(any(), any(), any())).thenReturn(0L);

        PetResponse response = petService.getMyPet();

        assertThat(response.getMoodCode()).isEqualTo("bored");
        assertThat(response.getMoodLabel()).isEqualTo("无聊");
    }

    @Test
    void getMyPetCalculatesFullnessFromPastFifteenDaysAndCapsAtOneHundred() {
        AuthContext.setUserId("owner");
        when(userPetMapper.selectOne(any())).thenReturn(pet("pet-1", "owner", "corgi", "年糕"));
        when(circlePlanMapper.countRecipesCreatedByUserInPlanDateRange(
                "owner",
                LocalDate.of(2026, 6, 6),
                LocalDate.of(2026, 6, 20)))
                .thenReturn(8L);

        PetResponse response = petService.getMyPet();

        assertThat(response.getFullnessPercent()).isEqualTo(100);
        verify(circlePlanMapper).countRecipesCreatedByUserInPlanDateRange(
                "owner",
                LocalDate.of(2026, 6, 6),
                LocalDate.of(2026, 6, 20));
    }

    @Test
    void renamePetUpdatesName() {
        AuthContext.setUserId("owner");
        when(userPetMapper.selectOne(any())).thenReturn(pet("pet-1", "owner", "tabby_cat", "饭团"));
        when(circlePlanMapper.countRecipesCreatedByUserInPlanDateRange(any(), any(), any())).thenReturn(0L);

        PetRenameRequest request = new PetRenameRequest();
        request.setName("年糕");

        PetResponse response = petService.renamePet(request);

        assertThat(response.getName()).isEqualTo("年糕");
        verify(userPetMapper).updateById(any(UserPet.class));
    }

    @Test
    void renamePetRejectsWhenUserHasNoPet() {
        AuthContext.setUserId("owner");
        when(userPetMapper.selectOne(any())).thenReturn(null);

        PetRenameRequest request = new PetRenameRequest();
        request.setName("年糕");

        assertThatThrownBy(() -> petService.renamePet(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("你还没有领取宠物");
        verify(userPetMapper, never()).updateById(any(UserPet.class));
    }

    @Test
    void renamePetRejectsTooLongName() {
        AuthContext.setUserId("owner");
        when(userPetMapper.selectOne(any())).thenReturn(pet("pet-1", "owner", "tabby_cat", "饭团"));

        PetRenameRequest request = new PetRenameRequest();
        request.setName("一二三四五六七八九");

        assertThatThrownBy(() -> petService.renamePet(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("宠物名字最多 8 个字");
        verify(userPetMapper, never()).updateById(any(UserPet.class));
    }

    private UserPet pet(String id, String userId, String petType, String name) {
        UserPet pet = new UserPet();
        pet.setId(id);
        pet.setUserId(userId);
        pet.setPetType(petType);
        pet.setName(name);
        pet.setExperience(0);
        pet.setCreatedAt(LocalDateTime.of(2026, 6, 18, 12, 0));
        pet.setUpdatedAt(LocalDateTime.of(2026, 6, 18, 12, 0));
        return pet;
    }
}
