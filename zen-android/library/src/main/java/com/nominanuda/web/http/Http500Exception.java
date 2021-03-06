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



public class Http500Exception extends Http5xxException {
	private static final long serialVersionUID = 343943985438591L;

	public Http500Exception(Exception e) {
		super(e, 500);
	}

	public Http500Exception(String msg) {
		super(msg, 500);
	}

	public Http500Exception(IApiError apiError) {
		super(apiError, 500);
	}
}
