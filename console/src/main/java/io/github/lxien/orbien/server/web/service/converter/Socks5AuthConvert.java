package io.github.lxien.orbien.server.web.service.converter;

import io.github.lxien.orbien.server.web.dto.socks5auth.Socks5AuthDetailDTO;
import io.github.lxien.orbien.server.web.dto.socks5auth.Socks5UserDTO;
import io.github.lxien.orbien.server.web.entity.Socks5AuthDO;
import io.github.lxien.orbien.server.web.entity.Socks5UserDO;
import io.github.lxien.orbien.server.web.param.socks5auth.Socks5UserAddParam;
import io.github.lxien.orbien.server.web.param.socks5auth.Socks5UserUpdateParam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface Socks5AuthConvert {
    @Mapping(expression = "java(toUserDTOList(users))", target = "users")
    Socks5AuthDetailDTO toDetailDTO(Socks5AuthDO authDO, List<Socks5UserDO> users);

    Socks5UserDTO toUserDTO(Socks5UserDO userDO);

    List<Socks5UserDTO> toUserDTOList(List<Socks5UserDO> users);

    Socks5UserDO toUserDO(Socks5UserAddParam param);

    void updateUserDO(@MappingTarget Socks5UserDO userDO, Socks5UserUpdateParam param);
}
