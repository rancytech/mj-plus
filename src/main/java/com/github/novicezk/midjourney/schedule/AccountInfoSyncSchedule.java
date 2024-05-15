package com.github.novicezk.midjourney.schedule;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.enums.BotType;
import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.util.SnowFlake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountInfoSyncSchedule implements ApplicationRunner {
	private final DiscordLoadBalancer discordLoadBalancer;
	private final ProxyProperties properties;
	private final TaskScheduler taskScheduler;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String accountSyncCron = this.properties.getAccountSyncCron();
		if (CharSequenceUtil.isBlank(accountSyncCron) || CharSequenceUtil.equalsAny(accountSyncCron, "0", "false", "off", "disable", "null", "none")) {
			log.info("Disabled account info sync schedule.");
			return;
		}
		log.info("Account info sync at [{}].", accountSyncCron);
		this.taskScheduler.schedule(() -> {
			List<DiscordInstance> aliveInstances = this.discordLoadBalancer.getAliveInstances();
			log.debug("Start sync account info... alive account count: {}.", aliveInstances.size());
			for (DiscordInstance instance : aliveInstances) {
				try {
					instance.info(BotType.MID_JOURNEY, SnowFlake.INSTANCE.nextId());
					ThreadUtil.sleep(3000L);
					instance.settings(BotType.MID_JOURNEY, SnowFlake.INSTANCE.nextId());
					ThreadUtil.sleep(3000L);
					instance.settings(BotType.NIJI_JOURNEY, SnowFlake.INSTANCE.nextId());
				} catch (UnsupportedOperationException e) {
					// do nothing
				} catch (Exception e) {
					log.warn("Sync account [{}] info error: {}.", instance.getInstanceId(), e.getMessage());
				}
			}
		}, new CronTrigger(accountSyncCron));
	}
}
