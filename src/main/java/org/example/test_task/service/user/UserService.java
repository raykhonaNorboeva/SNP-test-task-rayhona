package org.example.test_task.service.user;

import org.example.test_task.entity.UserEntity;

public interface UserService {

    UserEntity findById(Long id);
    void save(UserEntity user);
}
