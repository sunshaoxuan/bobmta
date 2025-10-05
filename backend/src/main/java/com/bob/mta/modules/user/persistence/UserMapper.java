package com.bob.mta.modules.user.persistence;

import com.bob.mta.modules.user.domain.UserStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface UserMapper {

    void insertUser(UserEntity entity);

    void updateUser(UserEntity entity);

    void deleteUser(@Param("userId") String userId);

    UserEntity findById(@Param("userId") String userId);

    UserEntity findByUsername(@Param("username") String username);

    UserEntity findByEmail(@Param("email") String email);

    List<UserEntity> findUsers(@Param("status") UserStatus status);

    List<UserRoleEntity> findRolesByUserId(@Param("userId") String userId);

    List<UserRoleEntity> findRolesByUserIds(@Param("userIds") Collection<String> userIds);

    void deleteRoles(@Param("userId") String userId);

    void insertRoles(@Param("roles") List<UserRoleEntity> roles);

    ActivationTokenEntity findActivationTokenByUserId(@Param("userId") String userId);

    List<ActivationTokenEntity> findActivationTokensByUserIds(@Param("userIds") Collection<String> userIds);

    ActivationTokenEntity findActivationTokenByToken(@Param("token") String token);

    void deleteActivationToken(@Param("userId") String userId);

    void insertActivationToken(ActivationTokenEntity entity);
}
