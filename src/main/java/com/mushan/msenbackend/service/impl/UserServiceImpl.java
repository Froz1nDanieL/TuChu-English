package com.mushan.msenbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushan.msenbackend.exception.BusinessException;
import com.mushan.msenbackend.exception.ErrorCode;
import com.mushan.msenbackend.exception.ThrowUtils;
import com.mushan.msenbackend.model.dto.user.EmailRegisterRequest;
import com.mushan.msenbackend.model.dto.user.ResetPasswordRequest;
import com.mushan.msenbackend.model.dto.user.UserEditRequest;
import com.mushan.msenbackend.model.dto.user.UserQueryRequest;
import com.mushan.msenbackend.model.entity.User;
import com.mushan.msenbackend.model.enums.UserRoleEnum;
import com.mushan.msenbackend.model.vo.LoginUserVO;
import com.mushan.msenbackend.model.vo.UserVO;
import com.mushan.msenbackend.service.EmailService;
import com.mushan.msenbackend.service.UserService;
import com.mushan.msenbackend.mapper.UserMapper;
import com.mushan.msenbackend.utils.VerifyCodeUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.mushan.msenbackend.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author Danie
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-12-07 16:36:56
*/
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Resource
    private EmailService emailService;
    
    @Resource
    private VerifyCodeUtil verifyCodeUtil;


    /**
     * 用户注册
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 检查是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        
        // 2. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        
        // 3. 判断是邮箱登录还是账号登录（根据是否包含@符号）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (userAccount.contains("@")) {
            // 邮箱登录
            queryWrapper.eq("userEmail", userAccount);
        } else {
            // 账号登录
            queryWrapper.eq("userAccount", userAccount);
        }
        queryWrapper.eq("userPassword", encryptPassword);
        
        // 4. 查询用户是否存在
        User user = this.baseMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("user login failed, userAccount/email cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        
        // 5. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }



    @Override
    public Boolean editUser(User loginUser, UserEditRequest userEditRequest){
        Long id = loginUser.getId();
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "用户不存在");
        User user = this.getById(id);

        // 更新用户信息
        String userName = userEditRequest.getUserName();
        String userProfile = userEditRequest.getUserProfile();
        String userAvatar = userEditRequest.getUserAvatar();

        user.setUserName(userName);
        user.setUserProfile(userProfile);
        user.setUserAvatar(userAvatar);

        user.setEditTime(new Date());
        return this.updateById(user);
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     *获取加密密码
     * @param userPassword
     * @return
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "mushan";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 获取脱敏后的用户视图信息
     * @param user
     * @return
     */

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user,loginUserVO);
        return loginUserVO;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接返回上述结果）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }
    
    /**
     * 发送注册验证码
     */
    @Override
    public void sendRegisterCode(String email) {
        // 1. 校验邮箱格式
        if (StrUtil.isBlank(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        }
        
        // 2. 检查邮箱是否已注册
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userEmail", email);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        
        // 3. 检查是否可以发送（防止频繁发送）
        if (!verifyCodeUtil.canSendCode(email, "register")) {
            throw new BusinessException(ErrorCode.SEND_CODE_TOO_FREQUENT);
        }
        
        // 4. 生成验证码
        String code = verifyCodeUtil.generateCode();
        
        // 5. 保存验证码到Redis
        verifyCodeUtil.saveRegisterCode(email, code);
        
        // 6. 发送邮件
        emailService.sendVerifyCode(email, code, "register");
        
        log.info("注册验证码发送成功，邮箱：{}", email);
    }
    
    /**
     * 邮箱注册
     */
    @Override
    public long emailRegister(EmailRegisterRequest request) {
        String userEmail = request.getUserEmail();
        String emailCode = request.getEmailCode();
        String userAccount = request.getUserAccount();
        String userPassword = request.getUserPassword();
        String checkPassword = request.getCheckPassword();
        
        // 1. 校验参数
        if (StrUtil.hasBlank(userEmail, emailCode, userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        // 校验账号格式：只能包含大小写字母、数字和下划线
        if (!userAccount.matches("^[a-zA-Z0-9_]+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号格式不正确，只能包含字母、数字和下划线");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }
        
        // 2. 校验验证码
        if (!verifyCodeUtil.verifyRegisterCode(userEmail, emailCode)) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_ERROR);
        }
        
        // 3. 检查账号是否重复
        QueryWrapper<User> accountWrapper = new QueryWrapper<>();
        accountWrapper.eq("userAccount", userAccount);
        long accountCount = this.baseMapper.selectCount(accountWrapper);
        if (accountCount > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        
        // 4. 检查邮箱是否重复
        QueryWrapper<User> emailWrapper = new QueryWrapper<>();
        emailWrapper.eq("userEmail", userEmail);
        long emailCount = this.baseMapper.selectCount(emailWrapper);
        if (emailCount > 0) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        
        // 5. 加密密码
        String encryptPassword = getEncryptPassword(userPassword);
        
        // 6. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserEmail(userEmail);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        
        // 7. 删除验证码
        verifyCodeUtil.deleteCode(userEmail, "register");
        
        log.info("邮箱注册成功，用户ID：{}，邮箱：{}", user.getId(), userEmail);
        return user.getId();
    }
    
    /**
     * 发送重置密码验证码
     */
    @Override
    public void sendResetPasswordCode(String email) {
        // 1. 校验邮箱格式
        if (StrUtil.isBlank(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        }
        
        // 2. 检查邮箱是否已注册
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userEmail", email);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_EXISTS);
        }
        
        // 3. 检查是否可以发送（防止频繁发送）
        if (!verifyCodeUtil.canSendCode(email, "reset")) {
            throw new BusinessException(ErrorCode.SEND_CODE_TOO_FREQUENT);
        }
        
        // 4. 生成验证码
        String code = verifyCodeUtil.generateCode();
        
        // 5. 保存验证码到Redis
        verifyCodeUtil.saveResetCode(email, code);
        
        // 6. 发送邮件
        emailService.sendVerifyCode(email, code, "reset");
        
        log.info("重置密码验证码发送成功，邮箱：{}", email);
    }
    
    /**
     * 重置密码
     */
    @Override
    public boolean resetPassword(ResetPasswordRequest request) {
        String userEmail = request.getUserEmail();
        String emailCode = request.getEmailCode();
        String newPassword = request.getNewPassword();
        String checkPassword = request.getCheckPassword();
        
        // 1. 校验参数
        if (StrUtil.hasBlank(userEmail, emailCode, newPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (newPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短，最少需要8位");
        }
        if (!newPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }
        
        // 2. 校验验证码
        if (!verifyCodeUtil.verifyResetCode(userEmail, emailCode)) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_ERROR);
        }
        
        // 3. 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userEmail", userEmail);
        User user = this.baseMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_EXISTS);
        }
        
        // 4. 加密新密码
        String encryptPassword = getEncryptPassword(newPassword);
        
        // 5. 更新密码
        user.setUserPassword(encryptPassword);
        boolean updateResult = this.updateById(user);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "密码重置失败");
        }
        
        // 6. 删除验证码
        verifyCodeUtil.deleteCode(userEmail, "reset");
        
        log.info("密码重置成功，用户ID：{}，邮箱：{}", user.getId(), userEmail);
        return true;
    }

//    @Override
//    public String uploadAvatar(User loginUser, MultipartFile avatar) {
//        String uploadPathPrefix = String.format("avatar/%s", loginUser.getId());
//        UploadPictureResult uploadPictureResult = filePictureUpload.uploadPicture(avatar, uploadPathPrefix);
//        return uploadPictureResult.getUrl();
//    }


}




