package com.github.novicezk.midjourney.store;

import org.jooq.ExecuteContext;
import org.jooq.ExecuteType;
import org.jooq.impl.DefaultExecuteListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JooqExecuteListener extends DefaultExecuteListener {
	private static final long serialVersionUID = 5675272630833212172L;

	private static final String START_TIME = "startTime";
	public static final long SLOW_SQL_TIME = 200L;

	protected Long getStartTime(ExecuteContext context) {
		return (Long) context.data(START_TIME);
	}

	@Override
	public void end(ExecuteContext context) {
		long now = System.currentTimeMillis();
		Long starTime = this.getStartTime(context);
		String sql = context.sql();

		if (starTime != null && sql != null) {
			long cost = now - starTime;
			if (context.type() == ExecuteType.DDL) {
				log.trace("{} cost {} ms: {}", context.type(), cost, sql);
			} else if (cost >= SLOW_SQL_TIME) {
				log.debug("{} slowly, cost {} ms: {}", context.type(), cost, sql);
			} else {
				log.trace("{} cost {} ms: {}", context.type(), cost, sql);
			}
		}
	}

	@Override
	public void start(ExecuteContext context) {
		context.data(START_TIME, System.currentTimeMillis());
	}
}
