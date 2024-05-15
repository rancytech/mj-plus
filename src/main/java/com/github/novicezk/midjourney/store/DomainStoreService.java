package com.github.novicezk.midjourney.store;


import com.github.novicezk.midjourney.condition.DomainCondition;
import com.github.novicezk.midjourney.domain.DomainObject;
import com.github.novicezk.midjourney.util.DomainSortComparator;
import com.github.novicezk.midjourney.util.PageUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Stream;

public interface DomainStoreService<T extends DomainObject> {

	void save(T domain);

	void delete(String id);

	T get(String id);

	default T findOne(DomainCondition<T> condition) {
		return list(condition).stream().findFirst().orElse(null);
	}

	List<T> listAll();

	default long count(DomainCondition<T> condition) {
		return listAll().stream().filter(condition).count();
	}

	default List<T> list(DomainCondition<T> condition) {
		return list(condition, Sort.unsorted());
	}

	default List<T> list(DomainCondition<T> condition, Sort sort) {
		Stream<T> tStream = listAll().stream().filter(condition);
		if (sort.isUnsorted()) {
			return tStream.toList();
		}
		return tStream.sorted(new DomainSortComparator<>(sort)).toList();
	}

	default Page<T> search(DomainCondition<T> condition, Pageable pageable) {
		List<T> list = list(condition, pageable.getSort());
		return PageUtils.getPageFromAll(list, pageable);
	}

}
