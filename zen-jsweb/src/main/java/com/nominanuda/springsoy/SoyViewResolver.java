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
package com.nominanuda.springsoy;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.tofu.SoyTofu;
import com.nominanuda.dataobject.DataObject;
import com.nominanuda.dataobject.MapsAndListsObjectDecorator;
import com.nominanuda.web.http.HttpProtocol;

import static com.nominanuda.dataobject.DataStructHelper.STRUCT;

public class SoyViewResolver implements ViewResolver {
	private SoySource soySource;

	public View resolveViewName(String viewName, Locale locale)
			throws Exception {
		return soySource.hasFunction(viewName, locale.getLanguage())
				? new SoyView(viewName, soySource.getSoyTofu(locale.getLanguage()), soySource.getBundle(locale.getLanguage()))
				: null;
	}

	public void setSoySource(SoySource soySource) {
		this.soySource = soySource;
	}

	public static class SoyView implements View {
		private final SoyTofu tofu;
		private final String name;
		private final SoyMsgBundle bundle;
		private static final LongToInt longToInt = new LongToInt();

		public SoyView(String name, SoyTofu tofu, SoyMsgBundle bundle) {
			this.name = name;
			this.tofu = tofu;
			this.bundle = bundle;
		}

		public String getContentType() {
			return "text/html;charset=UTF-8";
		}

		public void render(Map<String, ?> model, HttpServletRequest request,
				HttpServletResponse response) throws Exception {
			if(model instanceof MapsAndListsObjectDecorator) {
				DataObject o = ((MapsAndListsObjectDecorator)model).unwrap();
				Map<String, ? super Object> mm = STRUCT.toMapsAndLists(o);
				model = mm;
			}
			@SuppressWarnings("unchecked")
			String s = tofu.newRenderer(name)
						.setData(longToInt.longToInt((Map<String, Object>)model))
						.setMsgBundle(bundle)
						.render();
			byte[] b = s.getBytes(HttpProtocol.CS_UTF_8);
			response.setContentType(getContentType());
			response.setContentLength(b.length);
			response.getOutputStream().write(b);
		}
		static class LongToInt {
			@SuppressWarnings("unchecked")
			public Map<String, ?> longToInt(Map<String, Object> m) {
				for(Entry<String, Object> e : m.entrySet()) {
					Object o = e.getValue();
					if(o != null) {
						if(o instanceof Long) {
							e.setValue(((Long) o).intValue());
						} else if(o instanceof Map) {
							longToInt((Map<String, Object>)o);
						} else if(o instanceof List) {
							longToInt((List<Object>)o);
						}
					}
				}
				return m;
			}
			@SuppressWarnings("unchecked")
			public void longToInt(List<Object> l) {
				int len = l.size();
				for(int i = 0; i < len; i++) {
					Object o = l.get(i);
					if(o != null) {
						if(o instanceof Long) {
							l.set(i, ((Long)o).intValue());
						} else if(o instanceof Map) {
							longToInt((Map<String, Object>)o);
						} else if(o instanceof List) {
							longToInt((List<Object>)o);
						}
					}
				}
			}
		}
	}
}
