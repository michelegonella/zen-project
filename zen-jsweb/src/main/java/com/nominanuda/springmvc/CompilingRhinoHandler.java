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

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

import com.nominanuda.lang.Tuple2;

public class CompilingRhinoHandler extends RhinoHandler {
	private boolean develMode = false;
	private Map<String, Script> scriptCache = new HashMap<String, Script>();

	@Override
	protected void evaluateScript(Context cx, Scriptable controllerScope,
			String uri) throws IOException {
		if(develMode) {
			super.evaluateScript(cx, controllerScope, uri);
		} else {
			Script s1 = getOrCompile(cx, uri);
			rhino.evaluateScript(s1, cx, controllerScope);
		}

	}
	private Script getOrCompile(Context cx, String uri) throws IOException {
		if(scriptCache.containsKey(uri)) {
			return scriptCache.get(uri);
		} else {
			Tuple2<String,Reader> script = getSource(uri);
			String jsLocation = script.get0();
			Reader src = script.get1();
			Script s = rhino.compileScript(src, jsLocation, null, cx);
			scriptCache.put(uri, s);
			return s;
		}
	}
	public void setDevelMode(boolean develMode) {
		this.develMode = develMode;
	}
}
