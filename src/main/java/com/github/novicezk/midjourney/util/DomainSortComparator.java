package com.github.novicezk.midjourney.util;

import com.github.novicezk.midjourney.domain.DomainObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Comparator;

@Slf4j
public class DomainSortComparator<T extends DomainObject> implements Comparator<T> {
	private final Sort sort;

	public DomainSortComparator(Sort sort) {
		this.sort = sort;
	}

	@Override
	public int compare(T o1, T o2) {
		if (this.sort.isUnsorted()) {
			return 0;
		}
		for (Sort.Order order : this.sort) {
			Object o1v = getFieldValue(o1, order.getProperty());
			Object o2v = getFieldValue(o2, order.getProperty());
			int i = compareValue(o1v, o2v);
			i = order.isAscending() ? i : i * -1;
			if (i != 0) {
				return i;
			}
		}
		return 0;
	}

	private int compareValue(final Object c1, final Object c2) {
		if (c1 == c2) {
			return 0;
		} else if (c1 == null) {
			return 1;
		} else if (c2 == null) {
			return -1;
		} else if (c1 instanceof Comparable) {
			return ((Comparable) c1).compareTo(c2);
		}
		return 0;
	}

	private Object getFieldValue(T o, String name) {
		Field field = ReflectionUtils.findField(o.getClass(), name);
		if (field == null) {
			log.warn("field[{}:{}] not found", o.getClass().getName(), name);
			return null;
		}
		try {
			field.setAccessible(true);
			return field.get(o);
		} catch (IllegalAccessException e) {
			log.warn("get field value error", e);
			return null;
		}
	}

}
