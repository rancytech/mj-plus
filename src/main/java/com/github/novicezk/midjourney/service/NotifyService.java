package com.github.novicezk.midjourney.service;


import com.github.novicezk.midjourney.domain.Task;

public interface NotifyService {

	void notifyTaskChange(Task task);

}
