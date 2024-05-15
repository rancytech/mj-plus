package com.github.novicezk.midjourney.store;


import com.github.novicezk.midjourney.condition.DomainCondition;
import com.github.novicezk.midjourney.domain.DomainObject;
import com.github.novicezk.midjourney.store.mapper.DomainRecordMapper;
import com.github.novicezk.midjourney.util.NamingStrategyUtils;
import lombok.val;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSON;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public abstract class AbstractMySQLService<T extends DomainObject> implements DomainStoreService<T>, InitializingBean {
	@Autowired
	protected DSLContext dslContext;

	protected abstract String tableName();

	protected abstract DomainRecordMapper<T> recordMapper();

	protected abstract Map<Field<?>, Object> domainDSLMap(T domain);

	protected abstract void initTable();

	@Override
	public void afterPropertiesSet() {
		initTable();
	}

	@Override
	public void save(T domain) {
		this.dslContext.transaction(configuration -> {
			DSLContext context = DSL.using(configuration);
			boolean exists = context.fetchExists(table(tableName()), field("id").eq(domain.getId()));
			Map<Field<?>, Object> fieldObjectMap = domainDSLMap(domain);
			fieldObjectMap.put(field("properties", JSON.class), JSON.valueOf(new JSONObject(domain.getProperties()).toString()));
			if (exists) {
				try (val query = context.update(table(tableName())).set(fieldObjectMap)) {
					query.where(field("id").eq(domain.getId())).execute();
				}
			} else {
				fieldObjectMap.put(field("id"), domain.getId());
				try (val query = context.insertInto(table(tableName())).set(fieldObjectMap)) {
					query.execute();
				}
			}
		});
	}

	@Override
	public void delete(String key) {
		this.dslContext.transaction(configuration -> {
			try (val query = DSL.using(configuration).deleteFrom(table(tableName()))) {
				query.where(field("id").eq(key)).execute();
			}
		});
	}

	@Override
	public T get(String key) {
		try (val query = this.dslContext.selectFrom(tableName())) {
			return query.where(field("id").eq(key)).fetchOne(recordMapper());
		}
	}

	@Override
	public List<T> listAll() {
		try (val query = this.dslContext.selectFrom(tableName())) {
			return query.fetch(recordMapper());
		}
	}

	@Override
	public T findOne(DomainCondition<T> condition) {
		try (val query = this.dslContext.selectFrom(tableName())) {
			return query.where(condition.getSQLCondition()).limit(1).fetchOne(recordMapper());
		}
	}

	@Override
	public List<T> list(DomainCondition<T> condition, Sort sort) {
		try (val query = this.dslContext.selectFrom(tableName())) {
			return query.where(condition.getSQLCondition()).orderBy(convertOrders(sort)).fetch(recordMapper());
		}
	}

	@Override
	public long count(DomainCondition<T> condition) {
		try (val query = this.dslContext.selectCount()) {
			Long count = query.from(tableName()).where(condition.getSQLCondition()).fetchOneInto(Long.class);
			return Optional.ofNullable(count).orElse(0L);
		}
	}

	@Override
	public Page<T> search(DomainCondition<T> condition, Pageable pageable) {
		long count = count(condition);
		if (count == 0) {
			return Page.empty(pageable);
		}
		try (val query = this.dslContext.selectFrom(tableName())) {
			List<T> result = query.where(condition.getSQLCondition()).orderBy(convertOrders(pageable.getSort()))
					.offset(pageable.getOffset()).limit(pageable.getPageSize())
					.fetch(recordMapper());
			return PageableExecutionUtils.getPage(result, pageable, () -> count);
		}
	}

	protected List<OrderField<?>> convertOrders(Sort sort) {
		if (sort == null || sort.isUnsorted()) {
			return new ArrayList<>();
		}
		List<OrderField<?>> orders = new ArrayList<>(4);
		sort.forEach(o -> {
			String property = NamingStrategyUtils.convert(o.getProperty());
			orders.add(o.isAscending() ? field(property).asc() : field(property).desc());
		});
		return orders;
	}
}
