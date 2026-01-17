package com.eshop.app.mapper;

import com.eshop.app.dto.response.StoreResponse;
import com.eshop.app.entity.Store;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface StoreMapper {
    StoreMapper INSTANCE = Mappers.getMapper(StoreMapper.class);

    @Mapping(target = "sellerId", source = "seller.id")
    @Mapping(target = "sellerUsername", source = "seller.username")
    StoreResponse toStoreResponse(Store store);
}
