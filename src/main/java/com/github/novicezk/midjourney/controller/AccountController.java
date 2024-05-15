package com.github.novicezk.midjourney.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.condition.AccountCondition;
import com.github.novicezk.midjourney.domain.DiscordAccount;
import com.github.novicezk.midjourney.dto.AccountCreateDTO;
import com.github.novicezk.midjourney.dto.AccountQueryDTO;
import com.github.novicezk.midjourney.dto.AccountUpdateDTO;
import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.exception.DiscordInstanceStartException;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.result.Message;
import com.github.novicezk.midjourney.store.AccountStoreService;
import com.github.novicezk.midjourney.support.DiscordAccountHelper;
import com.github.novicezk.midjourney.support.DomainHelper;
import com.github.novicezk.midjourney.support.MessageButton;
import com.github.novicezk.midjourney.util.AsyncLockUtils;
import com.github.novicezk.midjourney.util.PageUtils;
import com.github.novicezk.midjourney.util.SnowFlake;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Api(tags = "账号管理")
@RestController
@RequestMapping("/mj/account")
@RequiredArgsConstructor
public class AccountController {
	private final DiscordLoadBalancer loadBalancer;
	private final DiscordAccountHelper discordAccountHelper;
	private final AccountStoreService accountStoreService;
	private final DomainHelper domainHelper;

	@ApiOperation(value = "指定ID获取账号")
	@GetMapping("/{id}/fetch")
	public Map<String, Object> fetch(@ApiParam(value = "账号ID") @PathVariable String id) {
		DiscordAccount account = this.accountStoreService.get(id);
		if (account == null) {
			return null;
		}
		return this.domainHelper.convertDomainVO(account);
	}

	@ApiOperation(value = "分页查询账号")
	@PostMapping("/query")
	public Page<Map<String, Object>> query(@RequestBody AccountQueryDTO conditionDTO) {
		Sort sort = PageUtils.convertSort(conditionDTO.getSort(), Sort.by(Sort.Direction.ASC, "dateCreated"));
		Pageable pageable = PageRequest.of(conditionDTO.getPageNumber(), conditionDTO.getPageSize(), sort);
		Page<DiscordAccount> pageResult = this.accountStoreService.search(conditionDTO.getCondition(), pageable);
		List<Map<String, Object>> voList = pageResult.getContent().stream().map(this.domainHelper::convertDomainVO).toList();
		return new PageImpl<>(voList, pageable, pageResult.getTotalElements());
	}

	@ApiOperation(value = "创建账号")
	@PostMapping("/create")
	public Message<Void> create(@RequestBody AccountCreateDTO createDTO) {
		if (!CharSequenceUtil.isAllNotBlank(createDTO.getGuildId(), createDTO.getChannelId(), createDTO.getUserToken())) {
			return Message.of(ReturnCode.VALIDATION_ERROR, "服务器ID、频道ID、用户Token不能为空");
		}

		AccountCondition condition = new AccountCondition().setUserToken(createDTO.getUserToken());
		if (this.accountStoreService.count(condition) > 0L) {
			return Message.of(ReturnCode.VALIDATION_ERROR, "账号已存在，相同的用户token");
		}

		DiscordAccount account = new DiscordAccount();
		BeanUtil.copyProperties(createDTO, account);
		account.setId(SnowFlake.INSTANCE.nextId());
		account.setDateCreated(new Date());

		try {
			DiscordInstance instance = this.discordAccountHelper.createDiscordInstance(account);
			instance.start();
			this.accountStoreService.save(account);
			this.loadBalancer.getAllInstances().add(instance);
		} catch (Exception e) {
			log.warn("Create account fail: {}", e.getMessage());
			return Message.of(ReturnCode.FAILURE, "wss连接失败: " + e.getMessage());
		}

		Message<DiscordAccount> message = syncInfo(account.getId());
		if (message.getCode() == ReturnCode.SUCCESS) {
			return Message.of(ReturnCode.SUCCESS, "创建成功，wss连接成功，账号信息同步成功");
		}
		return Message.of(message.getCode(), "创建成功，wss连接成功，账号信息同步失败: " + message.getDescription());
	}

	@ApiOperation(value = "更新账号并重连")
	@PutMapping("/{id}/update-reconnect")
	public Message<Void> updateAndReconnect(@ApiParam(value = "账号ID") @PathVariable String id, @RequestBody AccountUpdateDTO updateDTO) {
		if (!CharSequenceUtil.isAllNotBlank(updateDTO.getGuildId(), updateDTO.getChannelId(), updateDTO.getUserToken())) {
			return Message.of(ReturnCode.VALIDATION_ERROR, "服务器ID、频道ID、用户Token不能为空");
		}

		DiscordAccount account = this.accountStoreService.get(id);
		if (account == null) {
			return Message.notFound();
		}

		if (!CharSequenceUtil.equals(account.getUserToken(), updateDTO.getUserToken())) {
			AccountCondition a = (new AccountCondition()).setUserToken(updateDTO.getUserToken());
			if (this.accountStoreService.count(a) > 0L) {
				return Message.of(ReturnCode.VALIDATION_ERROR, "账号已存在，相同的用户Token");
			}
		}

		BeanUtil.copyProperties(updateDTO, account, "enable");
		if (updateDTO.getEnable() != null) {
			account.setEnable(updateDTO.getEnable());
		}

		List<DiscordInstance> allInstances = this.loadBalancer.getAllInstances();
		DiscordInstance instance = allInstances.stream().filter(i -> CharSequenceUtil.equals(id, i.getInstanceId()))
				.findFirst().orElse(null);
		if (instance != null) {
			allInstances.remove(instance);
			instance.stop();
		}
		instance = this.discordAccountHelper.createDiscordInstance(account);
		this.accountStoreService.save(account);
		allInstances.add(instance);
		if (!account.isEnable()) {
			return Message.of(ReturnCode.SUCCESS, "修改成功，账号已禁用");
		}
		try {
			instance.start();
			return Message.of(ReturnCode.SUCCESS, "修改成功，wss连接成功");
		} catch (DiscordInstanceStartException e) {
			account.setEnable(false);
			this.accountStoreService.save(account);
			return Message.of(ReturnCode.FAILURE, "修改成功，wss连接失败: " + e.getMessage());
		}
	}

	@ApiOperation(value = "删除账号")
	@DeleteMapping("/{id}/delete")
	public Message<Void> delete(@ApiParam(value = "账号ID") @PathVariable String id) {
		var account = this.accountStoreService.get(id);
		if (account == null) {
			return Message.notFound();
		}
		List<DiscordInstance> allInstances = this.loadBalancer.getAllInstances();
		DiscordInstance instance = allInstances.stream().filter(i -> CharSequenceUtil.equals(id, i.getInstanceId()))
				.findFirst().orElse(null);
		if (instance != null) {
			instance.account().setEnable(false);
			allInstances.remove(instance);
			instance.stop();
		}
		this.accountStoreService.delete(id);
		return Message.success();
	}

	@ApiOperation(value = "同步账号信息")
	@PostMapping("/{id}/sync-info")
	public Message<DiscordAccount> syncInfo(@ApiParam(value = "账号ID") @PathVariable String id) {
		DiscordInstance discordInstance = this.loadBalancer.getDiscordInstance(id);
		if (discordInstance == null || !discordInstance.isAlive()) {
			return Message.of(ReturnCode.NOT_FOUND, "账号不可用");
		}
		Message<Void> message = discordInstance.info(BotType.MID_JOURNEY, SnowFlake.INSTANCE.nextId());
		if (message.getCode() != ReturnCode.SUCCESS) {
			return Message.of(message.getCode(), message.getDescription());
		}
		try {
			AsyncLockUtils.waitForLock("info:" + discordInstance.account().getId(), Duration.ofMinutes(1));
			this.accountStoreService.save(discordInstance.account());
		} catch (TimeoutException e) {
			return Message.failure("获取/info信息超时");
		}
		message = discordInstance.settings(BotType.MID_JOURNEY, SnowFlake.INSTANCE.nextId());
		if (message.getCode() != ReturnCode.SUCCESS) {
			return Message.of(message.getCode(), message.getDescription());
		}
		try {
			AsyncLockUtils.waitForLock("settings-" + BotType.MID_JOURNEY.getValue() + ":" + discordInstance.account().getId(), Duration.ofMinutes(1));
			this.accountStoreService.save(discordInstance.account());
		} catch (TimeoutException e) {
			return Message.failure("获取mid_journey-settings信息超时");
		}
		try {
			message = discordInstance.settings(BotType.NIJI_JOURNEY, SnowFlake.INSTANCE.nextId());
			if (message.getCode() != ReturnCode.SUCCESS) {
				return Message.of(message.getCode(), message.getDescription());
			}
			AsyncLockUtils.waitForLock("settings-" + BotType.NIJI_JOURNEY.getValue() + ":" + discordInstance.account().getId(), Duration.ofMinutes(1));
			this.accountStoreService.save(discordInstance.account());
		} catch (TimeoutException e) {
			return Message.failure("获取niji_journey-settings信息超时");
		} catch (UnsupportedOperationException e) {
			return Message.success(discordInstance.account());
		}
		return Message.success(discordInstance.account());
	}

	@ApiOperation(value = "更改mj绘图版本")
	@PostMapping("/{id}/change-version")
	public Message<Map<String, Object>> changeVersion(@ApiParam(value = "账号ID") @PathVariable String id, @RequestParam String version) {
		DiscordInstance discordInstance = this.loadBalancer.getDiscordInstance(id);
		if (discordInstance == null || !discordInstance.isAlive()) {
			return Message.of(ReturnCode.NOT_FOUND, "账号不可用");
		}
		Message<Void> message = discordInstance.changeVersion(version, SnowFlake.INSTANCE.nextId());
		if (message.getCode() != ReturnCode.SUCCESS) {
			return Message.of(message.getCode(), message.getDescription());
		}
		try {
			AsyncLockUtils.waitForLock("settings-" + BotType.MID_JOURNEY.getValue() + ":" + discordInstance.account().getId(), Duration.ofMinutes(1));
			this.accountStoreService.save(discordInstance.account());
		} catch (TimeoutException e) {
			return Message.failure("获取settings信息超时");
		}
		return Message.success(this.domainHelper.convertDomainVO(discordInstance.account()));
	}

	@ApiOperation(value = "执行账号相关动作")
	@PostMapping("/{id}/action")
	public Message<Map<String, Object>> action(@ApiParam(value = "账号ID") @PathVariable String id, @RequestParam String customId,
			@RequestParam(required = false, defaultValue = "MID_JOURNEY") BotType botType) {
		DiscordInstance discordInstance = this.loadBalancer.getDiscordInstance(id);
		if (discordInstance == null || !discordInstance.isAlive()) {
			return Message.of(ReturnCode.NOT_FOUND, "账号不可用");
		}
		DiscordAccount account = discordInstance.account();
		MessageButton button = account.getButtons().stream().filter(b -> CharSequenceUtil.equals(b.getCustomId(), customId)).findFirst().orElse(null);
		if (button == null) {
			button = account.getNijiButtons().stream().filter(b -> CharSequenceUtil.equals(b.getCustomId(), customId)).findFirst().orElse(null);
		}
		if (button == null) {
			return Message.of(ReturnCode.VALIDATION_ERROR, "未找到相关动作");
		}
		String messageId = account.getProperty(botType.getValue() + ":messageId", String.class, account.getPropertyGeneric("messageId"));
		int flags = account.getProperty(botType.getValue() + ":flags", Integer.class, 64);
		Message<Void> message = discordInstance.action(botType, messageId, flags, button.getType(), customId, SnowFlake.INSTANCE.nextId());
		if (message.getCode() != ReturnCode.SUCCESS) {
			return Message.of(message.getCode(), message.getDescription());
		}
		try {
			AsyncLockUtils.LockObject lock = AsyncLockUtils.waitForLock("settings-" + botType.getValue() + ":" + discordInstance.account().getId(), Duration.ofMinutes(1));
			String error = lock.getPropertyGeneric("error");
			if (CharSequenceUtil.isNotBlank(error)) {
				return Message.failure(error);
			}
			this.accountStoreService.save(discordInstance.account());
		} catch (TimeoutException e) {
			return Message.failure("获取settings信息超时");
		}
		return Message.success(this.domainHelper.convertDomainVO(account));
	}

}