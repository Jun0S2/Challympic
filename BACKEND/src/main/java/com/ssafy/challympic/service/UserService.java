package com.ssafy.challympic.service;

import com.ssafy.challympic.domain.Media;
import com.ssafy.challympic.domain.User;
import com.ssafy.challympic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 이메일 중복 감지
     */
    public boolean validateDuplicateEmail(String user_email){
        List<User> findUser = userRepository.validateEmail(user_email);
        if(findUser.isEmpty()){
            return false;
        }
        return true;
    }

    /**
     * 닉네임 중복 감지
     */
    public boolean validateDuplicateNickname(String user_nickname){
        List<User> findUser = userRepository.validateNickname(user_nickname);
        if(findUser.isEmpty()){
            return false;
        }
        return true;
    }

    /**
     * 회원가입
     */
    @Transactional
    public int join(User user){
        userRepository.save(user);
        return user.getUser_no();
    }

    /**
     * 정보 수정
     */
    @Transactional
    public void updateUser(int user_no, String user_nickname, Media file, String user_title){
        User user = userRepository.findOne(user_no);
        if(user_nickname != null) user.setUser_nickname(user_nickname);
        if(file != null) user.setMedia(file);
        if(user_title != null) user.setUser_title(user_title);
    }

    @Transactional
    public void updatePwd(int user_no, String user_pwd){
        User user = userRepository.findOne(user_no);
        user.setUser_pwd(user_pwd);
    }

    public User findUser(int user_no){
        return userRepository.findOne(user_no);
    }

    public void deleteUser(int user_no) {
        User user = userRepository.findOne(user_no);
        userRepository.delete(user);
    }

    public User findByNickname(String user_nickname){
        return userRepository.findByNickname(user_nickname);
    }

    public List<User> findAllUser() {
        return userRepository.findAllUser();
    }

    public User findByEmail(String user_email) {
        return userRepository.findByEmail(user_email);
    }
}
