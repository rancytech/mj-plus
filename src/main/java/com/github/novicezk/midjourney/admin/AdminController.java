package com.github.novicezk.midjourney.admin;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.*;

import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.dto.LoginDTO;
import com.github.novicezk.midjourney.result.Message;
import com.github.novicezk.midjourney.support.ActivationHelper;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mj/admin")
@RequiredArgsConstructor
public class AdminController {
    private final ProxyProperties properties;
    private final HttpSession session;
    private final ActivationHelper helper;

    @PostMapping("/login")
    public Message<Void> login(@RequestBody LoginDTO loginDTO) {
        String username = properties.getUsername();
        String password = properties.getPassword();
        if (CharSequenceUtil.isBlank(password)) {
            password = properties.getApiSecret();
        }
        if (CharSequenceUtil.isBlank(password)) {
            password = "admin";
        }
        if (CharSequenceUtil.equals(username, loginDTO.getUsername())
            && CharSequenceUtil.equals(password, loginDTO.getPassword())) {
            session.setAttribute("username", username);
            return Message.success();
        }
        return Message.of(ReturnCode.VALIDATION_ERROR, "用户名或密码错误");
    }

    @PostMapping("/logout")
    public Message<Void> logout() {
        session.removeAttribute("username");
        return Message.success();
    }

    @GetMapping("/current")
    public Map<String, Object> current() {
        String username = (String)session.getAttribute("username");
        if (CharSequenceUtil.isBlank(username)) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("name", username);
        result.put("apiSecret", properties.getApiSecret());
        result.put("access", "admin");
        result.put("avatar", "https://gw.alipayobjects.com/zos/antfincdn/XAosXuNZyF/BiazfanxmamNRoxxVxka.png");
        result.put("imagePrefix", properties.getAdminImagePrefix());
        result.put("active", helper.checkActive());
        return result;
    }

}
