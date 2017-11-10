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
package com.nominanuda.web.http;

public class Http5xxException extends HttpAppException {
	private static final long serialVersionUID = 8974587487945387945L;

	public Http5xxException(Exception e, int status) {
		super(e, status);
	}

	public Http5xxException(String msg, int status) {
		super(msg, status);
	}

	public Http5xxException(IApiError apiError, int status) {
		super(apiError, status);
	}


	public Http5xxException(Exception e) {
		super(e);
	}

	public Http5xxException(String msg) {
		super(msg);
	}

	public Http5xxException(IApiError apiError) {
		super(apiError);
	}
}
