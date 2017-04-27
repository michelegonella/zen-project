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
package com.nominanuda.rhino;

import org.mozilla.javascript.Scriptable;

import com.nominanuda.zen.common.Ex.NoException;
import com.nominanuda.zen.obj.Stru;

public class ToStruCoercer implements ObjectCoercer<Scriptable, Stru, NoException> {
	private StruScriptableConvertor convertor = new StruScriptableConvertor();

	public Stru apply(Scriptable x) throws NoException {
		return convertor.fromScriptable(x);
	}

	public boolean canConvert(Object o) {
		return o != null && o instanceof Scriptable;
	}

}
