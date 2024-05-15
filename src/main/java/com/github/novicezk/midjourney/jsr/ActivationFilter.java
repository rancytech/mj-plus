package com.github.novicezk.midjourney.jsr;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import com.github.novicezk.midjourney.support.ActivationHelper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ActivationFilter extends OncePerRequestFilter {
	private final ActivationHelper helper;

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws ServletException, IOException {
		if (helper.checkActivationCode()) {
			chain.doFilter(req, resp);
		} else {
			resp.sendError(402, "请激活后再使用");
		}
	}
}
