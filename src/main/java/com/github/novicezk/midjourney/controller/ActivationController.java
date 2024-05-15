package com.github.novicezk.midjourney.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.novicezk.midjourney.jsr.ActivationDTO;
import com.github.novicezk.midjourney.jsr.CheckActivationDTO;
import com.github.novicezk.midjourney.support.ActivationHelper;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;

@Api(tags = "服务激活")
@RestController
@RequestMapping("/mj/act")
@RequiredArgsConstructor
public class ActivationController {
	private final ActivationHelper helper;

	@PostMapping("/check")
	public String check(@RequestBody CheckActivationDTO checkActivationDTO) {
		long a = System.currentTimeMillis();
		String activeCode = checkActivationDTO.getKey();
		boolean check = helper.checkActivationCode();
		String code = activeCode + a + check;

		try {
			String key = "M2YyN2JkZmUyNzgyZGFjZQ==";
			return Base64.encode((new SymmetricCrypto(SymmetricAlgorithm.AES, Base64.decode(key))).encrypt(code));
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	@ApiOperation("激活服务")
	@PostMapping("/activate")
	public String activate(@RequestBody ActivationDTO activationDTO) {
		try {
			helper.active(activationDTO.getCode());
		} catch (Exception e) {
			return "服务激活失败，" + e.getMessage();
		}

		return "激活成功，请继续使用";
	}

	@ApiOperation("获取机器码")
	@GetMapping("/machine")
	public String getMachineCode() {
		return this.helper.getMachineCode();
	}
}
