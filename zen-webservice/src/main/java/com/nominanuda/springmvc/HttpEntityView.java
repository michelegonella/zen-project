/*
 * Copyright 2008-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nominanuda.springmvc;

import static com.nominanuda.zen.oio.OioUtils.IO;

import java.io.InputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.springframework.web.servlet.View;

import com.nominanuda.web.http.HttpProtocol;

public class HttpEntityView implements View, HttpProtocol {
	private final HttpEntity entity;
	private boolean assumeServletContainerEnsuresContentLength = false;

	public HttpEntityView(HttpEntity entity) {
		this.entity = entity;
	}

	public String getContentType() {
		Header ct = entity.getContentType();
		return ct == null ? CT_APPLICATION_OCTET_STREAM : ct.getValue();
	}

	public void render(Map<String, ?> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		response.setContentType(getContentType());
		InputStream is = entity.getContent();
		long cl = entity.getContentLength();
		if(cl < 0) {
			if(assumeServletContainerEnsuresContentLength) {
				IO.pipeAndClose(is, response.getOutputStream());
			} else {
				byte[] b = IO.readAndClose(is);
				response.setContentLength(b.length);
				response.getOutputStream().write(b);
			}
		} else {
			response.setContentLength((int)cl);
			IO.pipeAndClose(is, response.getOutputStream());
		}
	}

	public void setAssumeServletContainerEnsuresContentLength(boolean assumeServletContainerEnsuresContentLength) {
		this.assumeServletContainerEnsuresContentLength = assumeServletContainerEnsuresContentLength;
	}

}
