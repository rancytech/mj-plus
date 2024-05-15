package com.github.novicezk.midjourney.util;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PageUtils {

	public static Sort convertSort(String sortStr, Sort defaultSort) {
		if (CharSequenceUtil.isBlank(sortStr)) {
			return defaultSort;
		}
		String[] split = sortStr.split(";");
		List<Sort.Order> orders = new ArrayList<>();
		for (String str : split) {
			String[] sortArr = str.split(",");
			if (sortArr.length != 2) {
				continue;
			}
			orders.add(sortArr[1].equalsIgnoreCase("desc") ? Sort.Order.desc(sortArr[0]) : Sort.Order.asc(sortArr[0]));
		}
		return Sort.by(orders);
	}

	public static Sort convertSort(String sortStr) {
		return convertSort(sortStr, Sort.unsorted());
	}

	public static Pageable convertPageableWithDefaultSort(Pageable pageable, Sort defaultSort) {
		if (pageable.getSort().isSorted()) {
			return pageable;
		}
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort);
	}

	public static <T> Page<T> getPageFromAll(List<T> all, Pageable pageable) {
		int count = all.size();
		if (all.isEmpty()) {
			return Page.empty(pageable);
		}
		if (pageable.isUnpaged()) {
			return new PageImpl<>(all, pageable, count);
		}
		int startIndex = (int) pageable.getOffset();
		int limit = pageable.getPageSize();
		if (startIndex == 0 && limit >= count) {
			return new PageImpl<>(all, pageable, count);
		}
		List<T> content = all.subList(startIndex, Math.min(startIndex + limit, count));
		return new PageImpl<>(content, pageable, count);
	}

}
