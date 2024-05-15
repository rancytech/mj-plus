package com.github.novicezk.midjourney.util;


import cn.hutool.core.exceptions.ValidateException;
import eu.maxschuster.dataurl.DataUrl;
import lombok.experimental.UtilityClass;

import java.text.DecimalFormat;
import java.util.List;

@UtilityClass
public class FileUtils {
	private static final long FILE_SIZE_MB_LIMIT = 20; // 文件限制大小，单位MB

	public void checkFileSizeAndThrows(List<DataUrl> dataUrls) {
		long fileSize = 0;
		for (DataUrl dataUrl : dataUrls) {
			fileSize += dataUrl.getData().length;
		}
		double fileSizeMB = (double) fileSize / (1024 * 1024);
		DecimalFormat df = new DecimalFormat("#.#");
		fileSizeMB = Double.parseDouble(df.format(fileSizeMB));
		if (fileSizeMB > FILE_SIZE_MB_LIMIT) {
			throw new ValidateException("文件总大小: " + fileSizeMB + "MB，超过限制[" + FILE_SIZE_MB_LIMIT + "MB]");
		}
	}

}
