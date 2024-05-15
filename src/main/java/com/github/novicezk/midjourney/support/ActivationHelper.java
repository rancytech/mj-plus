package com.github.novicezk.midjourney.support;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

@Slf4j
@Component
public class ActivationHelper {
	/**
	 * 机器码
	 */
	@Getter
	private String machineCode;

	/**
	 * 激活标志
	 */
	private boolean active = false;

	/**
	 * 验证是否激活
	 */
	public boolean checkActive() {
		File file = new File(getKeyFile());
		if (!file.exists()) {
			return false;
		}

		String key = FileUtil.readUtf8String(file);

		try {
			active(key);
			return true;
		} catch (IllegalAccessException e) {
			log.warn(e.getMessage());
		}

		return false;
	}

	/**
	 * 激活操作
	 */
	public void active(String encryptActKey) throws IllegalAccessException {
		String keys = crypto().decryptStr(encryptActKey, CharsetUtil.CHARSET_UTF_8);
		List<String> strs = CharSequenceUtil.split(keys, '&');

		// 验证激活码格式
		if (strs.size() != 2 || !machineCode.equals(strs.get(0))) {
			throw new IllegalAccessException("激活码错误");
		}

		try {
			// 验证激活码有效期
			DateTime date = DateUtil.parse(strs.get(1), "yyyyMMdd");
			if (date.isBefore(new Date())) {
				throw new IllegalAccessException("激活码已过期");
			}
		} catch (Exception e) {
			throw new IllegalAccessException("激活码错误");
		}

		// 保存激活码到文件
		FileUtil.writeUtf8String(encryptActKey, getKeyFile());
	}

	/**
	 * 验证激活码
	 */
	public boolean checkActivationCode() {
		if (this.active) {
			return true;
		}

		synchronized (this) {
			if (active) {
				return true;
			}

			active = checkActive();
		}

		return active;
	}

	@PostConstruct
	void init() {
		HardwareAbstractionLayer hwal = new SystemInfo().getHardware();
		String uuid = hwal.getComputerSystem().getHardwareUUID();
		String pid = hwal.getProcessor().getProcessorIdentifier().getProcessorID();
		// String sn = hwal.getComputerSystem().getBaseboard().getSerialNumber();
		Stream<NetworkIF> nets = hwal.getNetworkIFs(false).stream();
		Optional<String> sn = nets.map(NetworkIF::getMacaddr).filter(StrUtil::isNotEmpty).findFirst();
		String code = uuid + "&" + pid + "&" + (sn.isPresent() ? sn.get() : "");
		log.debug(code);

		// 硬件信息读取失败的场合使用随机码
		if (CharSequenceUtil.contains(code, "unknown")) {
			File file = new File("/home/spring/logs/.gc");
			if (file.exists()) {
				code = FileUtil.readUtf8String(file);
			} else {
				code = IdUtil.randomUUID();
				FileUtil.writeUtf8String(code, file);
			}
		}

		machineCode = MD5.create().digestHex(code);
	}

	/**
	 * 激活文件路径
	 */
	private String getKeyFile() {
		return System.getProperty("user.dir") + "/config/.act_key";
	}

	/**
	 * 对称加密算法
	 */
	private SymmetricCrypto crypto() {
		byte[] key = Base64.decode("NzY1YWFiY3hkZWZnaD09Mg==");
		return new SymmetricCrypto(SymmetricAlgorithm.AES, key);
	}
}
