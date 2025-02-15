package org.example.test_task.service.user;

import lombok.RequiredArgsConstructor;
import org.example.test_task.entity.UserEntity;
import org.example.test_task.exception.DataNotFoundException;
import org.example.test_task.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;


    @Override
    public UserEntity findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new DataNotFoundException("User not found"));
    }

    @Override
    public void save(UserEntity user) {
        userRepository.save(user);
    }
}
