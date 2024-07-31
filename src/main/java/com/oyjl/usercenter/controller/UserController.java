package com.oyjl.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.oyjl.usercenter.common.BaseResponse;
import com.oyjl.usercenter.common.ErrorCode;
import com.oyjl.usercenter.common.ResultUtils;
import com.oyjl.usercenter.contant.UserConstant;
import com.oyjl.usercenter.exception.BusinessException;
import com.oyjl.usercenter.model.domain.User;
import com.oyjl.usercenter.model.domain.request.UserLoginRequest;
import com.oyjl.usercenter.model.domain.request.UserRegisterRequest;
import com.oyjl.usercenter.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import static com.oyjl.usercenter.contant.UserConstant.ADMIN_ROLE;

/**
 * 用户接口
 *
 * @author oyjl
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户登录态键
     */
    public static final String USER_LOGIN_STATE = "userLoginStatue";

    /**
     * 用户注册接口
     *
     * @param userRegisterRequest 用户注册请求
     * @return 注册成功返回用户id，注册失败返回null
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();

        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(result);
    }

    /**
     * 登录接口
     *
     * @param userLoginRequest 用户登录请求
     * @param request          http请求
     * @return 登录成功返回用户信息，登录失败返回null
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    /**
     * 登录接口
     *
     * @param request http请求
     * @return 登录成功返回用户信息，登录失败返回null
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);

    }

    /**
     * 获取当前用户接口
     *
     * @param request http请求
     * @return 当前用户信息
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        // 获取session对象
        HttpSession session = request.getSession(false);
        if (session == null) {
            logger.error("Session is null");
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 打印session的详细信息
        Serializable sessionAttributes = session.getValueNames();
        logger.debug("Session attributes: {}", sessionAttributes);

        // 使用正确的key获取用户对象
        Object userObj = session.getAttribute("userLoginState");
        User currentUser = (User) userObj;

        if (currentUser == null) {
            logger.error("User object is null in session");
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        Long userId = currentUser.getId();
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);

        return ResultUtils.success(safetyUser);
    }

    /**
     * 搜索用户接口
     *
     * @param username 用户名
     * @return 用户列表
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "缺少管理员权限");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    /**
     * 删除用户接口
     *
     * @param id 用户id
     * @return 删除成功返回用户id，删除失败返回false
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "缺少管理员权限");
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id不可以小于等于0");

        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    private boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            // 仅管理员可查询
            Object userObj = session.getAttribute("userLoginState");
            logger.debug("User object from session: {}", userObj);
            User user = userObj != null ? (User) userObj : null;
            return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
        }
        return false;
    }

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

}
