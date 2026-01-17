package com.eshop.app.mapper;

import com.eshop.app.dto.response.UserResponse;
import com.eshop.app.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "role", source = "role")
    @Mapping(target = "sellerType", source = "sellerType")
    @Mapping(target = "shop", source = "store")
    UserResponse toUserResponse(User user);
}
