package com.github.novicezk.midjourney.condition;

import com.github.novicezk.midjourney.domain.DomainObject;
import org.jooq.Condition;

import java.util.function.Predicate;


public interface DomainCondition<T extends DomainObject> extends Predicate<T> {
	Condition getSQLCondition();

}
