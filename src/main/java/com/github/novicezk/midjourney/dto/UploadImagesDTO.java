package com.github.novicezk.midjourney.dto;

import java.util.List;
import java.util.function.Predicate;

import com.github.novicezk.midjourney.loadbalancer.DiscordInstance;

import cn.hutool.core.text.CharSequenceUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("文件上传参数")
public class UploadImagesDTO {
	@ApiModelProperty(value = "base64数组", position = 1)
	private List<String> base64Array;

	@ApiModelProperty(value = "筛选账号上传，不需要筛选时设置为null或移除该参数", allowEmptyValue = true, position = 2)
	private UploadImagesDTO.CustomAccountFilter filter;

	@Data
	public static class CustomAccountFilter implements Predicate<DiscordInstance> {
		@ApiModelProperty(value = "账号实例ID", allowEmptyValue = true)
		private String instanceId;

		@ApiModelProperty(value = "频道ID", allowEmptyValue = true)
		private String channelId;

		@ApiModelProperty(value = "备注包含", allowEmptyValue = true)
		private String remark;

		public boolean test(DiscordInstance instance) {
			if (CharSequenceUtil.isNotBlank(this.instanceId) && !this.instanceId.equals(instance.getInstanceId())) {
				return false;
			}
			if (CharSequenceUtil.isNotBlank(this.channelId) && !this.channelId.equals(instance.getChannelId())) {
				return false;
			}
			if (CharSequenceUtil.isNotBlank(this.remark)
					&& !CharSequenceUtil.contains(instance.account().getRemark(), this.remark)) {
				return false;
			}
			return true;
		}
	}
}
