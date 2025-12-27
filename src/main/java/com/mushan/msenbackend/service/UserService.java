package com.mushan.msenbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mushan.msenbackend.model.dto.user.EmailRegisterRequest;
import com.mushan.msenbackend.model.dto.user.ResetPasswordRequest;
import com.mushan.msenbackend.model.dto.user.UserEditRequest;
import com.mushan.msenbackend.model.dto.user.UserQueryRequest;
import com.mushan.msenbackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mushan.msenbackend.model.vo.LoginUserVO;
import com.mushan.msenbackend.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author Danie
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-12-07 16:36:56
*/
public interface UserService extends IService<User> {

    long userRegister(String userAccount, String userPassword, String checkPassword);

    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    Boolean editUser(User loginUser, UserEditRequest userEditRequest);


    boolean userLogout(HttpServletRequest request);


    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    String getEncryptPassword(String userPassword);

    LoginUserVO getLoginUserVO(User user);

    User getLoginUser(HttpServletRequest request);

//    String uploadAvatar(User loginUser, MultipartFile avatar);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);
    
    /**
     * 发送注册验证码
     * @param email 邮箱
     */
    void sendRegisterCode(String email);
    
    /**
     * 邮箱注册
     * @param request 注册请求
     * @return 用户ID
     */
    long emailRegister(EmailRegisterRequest request);
    
    /**
     * 发送重置密码验证码
     * @param email 邮箱
     */
    void sendResetPasswordCode(String email);
    
    /**
     * 重置密码
     * @param request 重置密码请求
     * @return 是否成功
     */
    boolean resetPassword(ResetPasswordRequest request);
}
