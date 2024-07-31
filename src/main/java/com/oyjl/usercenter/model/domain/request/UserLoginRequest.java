package com.oyjl.usercenter.model.domain.request;

import lombok.Data;

/**
 * 用户登录请求参数
 *
 * @author oyjl
 */
import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {
    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;

}
