package com.panghu.food.service;

import com.panghu.food.auth.AuthContext;
import com.panghu.food.dto.DishDetailResponse;
import com.panghu.food.entity.BuddyCircleMember;
import com.panghu.food.entity.UserProfileSettings;
import com.panghu.food.mapper.ActivityFeedMapper;
import com.panghu.food.mapper.BuddyCircleMemberMapper;
import com.panghu.food.mapper.DishIngredientMapper;
import com.panghu.food.mapper.DishMapper;
import com.panghu.food.mapper.DishStepMapper;
import com.panghu.food.mapper.FriendRelationMapper;
import com.panghu.food.mapper.UserProfileSettingsMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DishServiceImplTest {
    private final DishMapper dishMapper = mock(DishMapper.class);
    private final DishIngredientMapper dishIngredientMapper = mock(DishIngredientMapper.class);
    private final DishStepMapper dishStepMapper = mock(DishStepMapper.class);
    private final UserProfileSettingsMapper userProfileSettingsMapper = mock(UserProfileSettingsMapper.class);
    private final FriendRelationMapper friendRelationMapper = mock(FriendRelationMapper.class);
    private final ActivityFeedMapper activityFeedMapper = mock(ActivityFeedMapper.class);
    private final BuddyCircleMemberMapper buddyCircleMemberMapper = mock(BuddyCircleMemberMapper.class);

    private final DishServiceImpl dishService = new DishServiceImpl();

    DishServiceImplTest() {
        ReflectionTestUtils.setField(dishService, "dishMapper", dishMapper);
        ReflectionTestUtils.setField(dishService, "dishIngredientMapper", dishIngredientMapper);
        ReflectionTestUtils.setField(dishService, "dishStepMapper", dishStepMapper);
        ReflectionTestUtils.setField(dishService, "userProfileSettingsMapper", userProfileSettingsMapper);
        ReflectionTestUtils.setField(dishService, "friendRelationMapper", friendRelationMapper);
        ReflectionTestUtils.setField(dishService, "activityFeedMapper", activityFeedMapper);
        ReflectionTestUtils.setField(dishService, "buddyCircleMemberMapper", buddyCircleMemberMapper);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void getDishByIdAllowsViewerInSharedCircleWhenNotFriends() {
        AuthContext.setUserId("viewer");
        when(dishMapper.selectDishDetailById("dish-1")).thenReturn(dish("dish-1", "owner"));
        when(userProfileSettingsMapper.selectById("owner")).thenReturn(settings("owner", "friends"));
        when(friendRelationMapper.selectCount(any())).thenReturn(0L);
        when(buddyCircleMemberMapper.selectList(any())).thenReturn(List.of(circleMember("circle-1", "viewer")));
        when(buddyCircleMemberMapper.selectCount(any())).thenReturn(1L);
        when(dishIngredientMapper.selectByDishId("dish-1")).thenReturn(List.of());
        when(dishStepMapper.selectByDishId("dish-1")).thenReturn(List.of());

        DishDetailResponse result = dishService.getDishById("dish-1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("dish-1");
        assertThat(result.getEffectiveVisibility()).isEqualTo("friends");
    }

    private DishDetailResponse dish(String id, String ownerUserId) {
        DishDetailResponse dish = new DishDetailResponse();
        dish.setId(id);
        dish.setOwnerUserId(ownerUserId);
        dish.setName("红烧肉");
        dish.setVisibility("friends");
        return dish;
    }

    private UserProfileSettings settings(String userId, String defaultVisibility) {
        UserProfileSettings settings = new UserProfileSettings();
        settings.setUserId(userId);
        settings.setDefaultMenuVisibility(defaultVisibility);
        return settings;
    }

    private BuddyCircleMember circleMember(String circleId, String userId) {
        BuddyCircleMember member = new BuddyCircleMember();
        member.setCircleId(circleId);
        member.setUserId(userId);
        return member;
    }
}
