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
package com.nominanuda.hyperapi;

import static com.nominanuda.zen.io.Uris.URIS;
import static com.nominanuda.zen.obj.JsonPath.JPATH;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nominanuda.springmvc.HttpContext;
import com.nominanuda.urispec.URISpec;
import com.nominanuda.web.http.Http500Exception;
import com.nominanuda.web.http.HttpProtocol;
import com.nominanuda.web.mvc.ObjURISpec;
import com.nominanuda.zen.common.Check;
import com.nominanuda.zen.obj.Arr;
import com.nominanuda.zen.obj.Obj;
import com.nominanuda.zen.obj.Stru;

public class HyperApiHttpInvocationHandler implements InvocationHandler {
	private final static String USER_AGENT = "curl/7.22.0 (x86_64-pc-linux-gnu) libcurl/7.22.0 OpenSSL/1.0.1 zlib/1.2.3.4 libidn/1.23 librtmp/2.3";
	
	private final RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
	private final EntityCodec entityCodec = EntityCodec.createBasic();
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final IHttpAppExceptionRenderer exceptionRenderer;
	private final HttpClient client;
	private final String uriPrefix;
	private final String userAgent;


	public HyperApiHttpInvocationHandler(HttpClient client, String uriPrefix, String userAgent, IHttpAppExceptionRenderer exceptionRenderer) {
		this.client = client;
		this.uriPrefix = uriPrefix;
		this.userAgent = Check.ifNullOrEmpty(userAgent, USER_AGENT);
		this.exceptionRenderer = exceptionRenderer;
	}
	

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		HttpRequestBase request = null;
		try {
			request = encode(uriPrefix, method, args);
		} catch (Exception e) {
			log.error(method.getName() + e);
			return null;
		}
		return executeDecodeAndRelease(method, request);
	}
	

	private HttpRequestBase encode(String uriPrefix, Method method, Object[] args) {
		HttpEntity entity = null;
		Obj uriParams = Obj.make();
		List<Header> requestHeaders = new ArrayList<>();
		List<NameValuePair> formParams = new ArrayList<>();
		
		Class<?>[] parameterTypes = method.getParameterTypes();
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		for (int i = 0; i < parameterTypes.length; i++) {
			Class<?> parameterType = parameterTypes[i];
			Annotation[] annotations = parameterAnnotations[i];
			Object arg = args[i];
			
			boolean annotationFound = false;
			for (Annotation annotation : annotations) {
				if (annotation instanceof HeaderParam) {
					annotationFound = true;
					if (arg != null) {
						requestHeaders.add(new BasicHeader(((HeaderParam) annotation).value(), arg.toString()));
					}
					break;
					
				} else if (annotation instanceof PathParam) {
					annotationFound = true;
					if (arg != null) {
						uriParams.put(((PathParam) annotation).value(), arg.toString());
					}
					break;
					
				} else if (annotation instanceof QueryParam) {
					annotationFound = true;
					if (arg != null) {
						uriParams.put(((QueryParam) annotation).value(), arg instanceof Collection
							? toArr((Collection<?>) arg)
							: arg.toString()
						);
					}
					break;
					
				} else if (annotation instanceof FormParam) {
					annotationFound = true;
					if (arg != null) {
						String name = ((FormParam) annotation).value();
						if (arg instanceof Obj) {
							Map<String, Object> map = new HashMap<String, Object>();
							JPATH.toFlatMap(Obj.make(name, arg), map);
							for (Entry<String, Object> entry : map.entrySet()) {
								if (entry.getValue() != null) {
									formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
								}
							}
						} else if (arg instanceof Collection) {
							for (Object v : (Collection<?>) arg) {
								if (v != null) {
									formParams.add(new BasicNameValuePair(name, v.toString()));
								}
							}
						} else {
							formParams.add(new BasicNameValuePair(name, arg.toString()));
						}
					}
					break;
				}
			}
			if (!annotationFound) {
				Check.unsupportedoperation.assertNull(entity, "no annotations found and no entity to send");
				entity = entityCodec.encode(arg, new AnnotatedType(parameterType, annotations));
			}
		}
		
		String httpMethod = null;
		URISpec<Obj> spec = null;
		for (Annotation a : method.getAnnotations()) {
			if (a instanceof POST) {
				httpMethod = HttpProtocol.POST;
			} else if (a instanceof GET) {
				httpMethod = HttpProtocol.GET;
			} else if (a instanceof PUT) {
				httpMethod = HttpProtocol.PUT;
			} else if (a instanceof DELETE) {
				httpMethod = HttpProtocol.DELETE;
			} else if (a instanceof Path) {
				spec = new ObjURISpec(URIS.pathJoin(uriPrefix, ((Path) a).value()));
//			} else if (a instanceof Consumes) {
//				consumedMediaTypes = ((Consumes) a).value();
			}
		}
		
		if (!formParams.isEmpty()) {
			try {
				entity = new UrlEncodedFormEntity(formParams, HttpProtocol.UTF_8);
				if (HttpProtocol.GET.equals(httpMethod)) {
					// just to prevent a common mistake when writing hyperapis...
					httpMethod = HttpProtocol.POST;
				}
			} catch (UnsupportedEncodingException e) {
				throw new Http500Exception("Unsupported parameter encoding");
			}
		}
		
		HttpRequestBase request = buildRequest(httpMethod, spec.template(uriParams), requestHeaders, method.getReturnType());
		if (entity != null && request instanceof HttpEntityEnclosingRequest) {
			((HttpEntityEnclosingRequest) request).setEntity(entity);
		}
		return request;
	}
	
	
	private Arr toArr(Collection<?> arg) {
		Arr arr = Arr.make();
		for (Object obj : arg) {
			if (obj != null) {
				arr.add(obj.toString());
			}
		}
		return arr;
	}
	
	
	private HttpRequestBase buildRequest(String httpMethod, String uri, List<Header> headers, Class<?> returnType) {
		HttpRequestBase request = createRequest(httpMethod, uri);
		request.setConfig(requestConfig);
		request.setHeaders(headers.toArray(new Header[headers.size()]));
		if (Stru.class.isAssignableFrom(returnType)) {
			request.setHeader(HttpProtocol.HDR_ACCEPT, HttpProtocol.CT_APPLICATION_JSON);
		} else {
			request.setHeader(HttpProtocol.HDR_ACCEPT, HttpProtocol.CT_ANY);
		}
		request.setHeader(HttpProtocol.HDR_CACHE_CONTROL, "no-cache");
		request.setHeader(HttpProtocol.HDR_CONNECTION, "close"); // TODO remove?
		request.setHeader(HttpProtocol.HDR_PRAGMA, "no-cache");
		request.setHeader(HttpProtocol.HDR_USER_AGENT, userAgent);
		return request;
	}

	private HttpRequestBase createRequest(String httpMethod, String uri) {
		switch (httpMethod) {
		case HttpProtocol.GET:
			return new HttpGet(uri);
		case HttpProtocol.POST:
			return new HttpPost(uri);
		case HttpProtocol.PUT:
			return new HttpPut(uri);
		case HttpProtocol.DELETE:
			return new HttpDelete(uri);
		}
		throw new IllegalArgumentException("unknown http method " + httpMethod);
	}
	


	private Object executeDecodeAndRelease(Method method, HttpRequestBase request) throws Exception {
		log.info(request.getMethod() + " " + request.getURI().toString());
		HttpContext httpContext = HttpContext.getInstance();
//		long startTime = System.currentTimeMillis();
		HttpResponse response = null;

		try {
			Object result = null;
			httpContext.writeTo(request);
			response = client.execute(request);
			httpContext.update(response);
			
			HttpEntity entity = response.getEntity();
			if (entity != null && entity.getContent() != null) {
				result = entityCodec.decode(entity, new AnnotatedType(method.getReturnType(), method.getAnnotations()));
			}
			if (exceptionRenderer != null) { // can be null if we don't want to throw exceptions
				exceptionRenderer.parseAndThrow(response.getStatusLine().getStatusCode(), result);
			}
			return result;
			
		} catch (Exception e) {
			log.error(e.toString());
			throw e;
			
		} finally {
			if (response != null) {
				EntityUtils.consume(response.getEntity());
			}
			request.releaseConnection();
		}
	}
}
