package com.example.backend.mapper;

import com.example.backend.dto.request.user.UserCreateRequest;
import com.example.backend.dto.response.user.UserResponse;
import com.example.backend.entity.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "password", ignore = true)
    User toUser(UserCreateRequest request);
    UserResponse toUserResponse(User user);

}
