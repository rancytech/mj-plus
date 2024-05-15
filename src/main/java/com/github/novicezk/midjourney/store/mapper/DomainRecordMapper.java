package com.github.novicezk.midjourney.store.mapper;


import com.github.novicezk.midjourney.domain.DomainObject;
import org.jooq.Record;
import org.jooq.RecordMapper;

public abstract class DomainRecordMapper<T extends DomainObject> implements RecordMapper<Record, T> {

}
