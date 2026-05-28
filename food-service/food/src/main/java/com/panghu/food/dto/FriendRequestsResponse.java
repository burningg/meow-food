package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FriendRequestsResponse {
    private List<FriendRequestItemResponse> incoming = new ArrayList<>();
    private List<FriendRequestItemResponse> outgoing = new ArrayList<>();
}
