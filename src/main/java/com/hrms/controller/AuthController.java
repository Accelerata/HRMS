package com.hrms.controller;

import com.hrms.common.context.BaseContext;
import com.hrms.dto.LoginDTO;
import com.hrms.result.Result;
import com.hrms.service.AuthService;
import com.hrms.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {


    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO result = authService.login(dto);
        return Result.success(result);
    }

    /**
     * 修改密码（首次登录强制改密 / 定期改密）
     */
    @PutMapping("/change-password")
    public Result<Object> changePassword(@RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        authService.changePassword(BaseContext.getCurrentUserId(), oldPassword, newPassword);
        return Result.success();
    }
}
