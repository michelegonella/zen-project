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

/*
 * AZ: this version merges with the model the request's body, if it is a json,
 * and initializes every object of type Location among the scope's java objects.
 */

package com.nominanuda.springmvc;

import static com.nominanuda.dataobject.DataStructHelper.STRUCT;
import static com.nominanuda.rhino.DataStructScriptableConvertor.DSS_CONVERTOR;
import static com.nominanuda.web.http.HttpCoreHelper.HTTP;
import static org.mozilla.javascript.RhinoHelper.RHINO;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoEmbedding;
import org.mozilla.javascript.ScopeFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SpringScopeFactory;

import com.nominanuda.dataobject.DataObject;
import com.nominanuda.dataobject.DataStruct;
import com.nominanuda.hyperapi.AnnotatedType;
import com.nominanuda.hyperapi.DataStructJsonDecoder;
import com.nominanuda.hyperapi.EntityDecoder;
import com.nominanuda.jsweb.host.JsHttpRequest;
import com.nominanuda.jsweb.host.Location;
import com.nominanuda.lang.Tuple2;
import com.nominanuda.urispec.URISpec;
import com.nominanuda.web.mvc.CommandRequestHandler;
import com.nominanuda.web.mvc.DataObjectURISpec;

public class RhinoHandler implements CommandRequestHandler {
	private final static String ENTITY_ARRAY_CMD_KEY = "_entity";
	private final static String REQUEST_EXTRA_PATTERNID = "patternId";
	
	protected final EntityDecoder jsonDecoder = new DataStructJsonDecoder();
	
	protected Sitemap sitemap;
	protected String patternId;
	protected URISpec<DataObject> uriSpec;
	protected RhinoEmbedding rhinoEmbedding;
	protected ScriptableObject cachedScope;
	protected ScopeFactory scopeFactory;
	protected boolean allowJavaPackageAccess = true; // TODO security turn to false default policy
	protected boolean mergeGetAndPostFormParams = true;
	protected boolean mergeEntityDataObject = true;
	protected String function = "handle";
	
	
	public void init() {
		// configure locations
		for (Object o : scopeFactory.getJavaObjects().values()) {
			if (o instanceof Location) {
				((Location)o).setSitemap(sitemap);
			}
		}
		// if the resource location doesn't depend from request's data, execute the script once (for errors spotting, caching,...)
		Context cx = rhinoEmbedding.enterContext();
		try {
			if (uriSpec.toString().equals(calcScriptUri(STRUCT.newObject(), null))) {
				evaluateScript(cx, buildScope(cx), uriSpec.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Context.exit();
		}
	}
	
	
	@Override
	public DataStruct handle(DataStruct cmd, HttpRequest request) throws Exception {
		Context cx = rhinoEmbedding.enterContext();
		try {
			HttpEntity entity = HTTP.getEntity(request);
			List<NameValuePair> pairs = Collections.emptyList();
			if (mergeGetAndPostFormParams) {
				DataObject cmdFromReq = HTTP.getQueryParams(request).asObject();
				if (entity != null) {
					pairs = HTTP.parseEntityWithDefaultUtf8(entity);
					STRUCT.copyPush(HTTP.toDataStruct(pairs).asObject(), cmdFromReq);
				}
				// TODO 
				// here request params hide uriparams  
				STRUCT.copyOverwite(cmd.asObject(), cmdFromReq);
				cmd = cmdFromReq;
			}
			
			if (mergeEntityDataObject && entity != null && pairs.isEmpty()) { // if pairs isn't empty -> entity was already consumed
				try {
					// DataStruct because it could be an array
					DataStruct structFromEntity = (DataStruct) jsonDecoder.decode(new AnnotatedType(DataStruct.class, new Annotation[] {}), entity);
					DataObject cmdFromEntity = (structFromEntity.isArray() // nees to be a DataObject before merging with cmd
							? STRUCT.newObject().with(ENTITY_ARRAY_CMD_KEY, structFromEntity)
							: structFromEntity.asObject());
					STRUCT.copyOverwite(cmd.asObject(), cmdFromEntity);
					cmd = cmdFromEntity;
				} catch (Exception e) {
					// better way than try/catch?
				}
			}
			
			Scriptable controllerScope = buildScope(cx);
			evaluateScript(cx, controllerScope, calcScriptUri(cmd, request));
			Object res = executeFunction(cx, controllerScope, function, cmd, request);
			return DSS_CONVERTOR.fromScriptable((Scriptable)res);
		} finally {
			Context.exit();
		}
	}
	
	
	protected String calcScriptUri(DataStruct cmd, HttpRequest request) throws IOException {
		return uriSpec.template(cmd.asObject());
	}
	
	protected void evaluateScript(Context cx, Scriptable controllerScope, String scriptUri) throws IOException {
		Tuple2<String,Reader> script = getSource(scriptUri);
		RHINO.evaluateReader(cx, script.get1(), script.get0(), controllerScope);
	}

	protected Tuple2<String, Reader> getSource(String uri) throws IOException {
		String jsLocation = uri.replace("classpath:", ""); // allows JSDT remote rhino debugging
		return new Tuple2<String, Reader>(jsLocation, new InputStreamReader(new URL(uri).openStream(), "UTF-8"));
	}
	
	protected Object executeFunction(Context cx, Scriptable controllerScope, String function, DataStruct cmd, HttpRequest request) {
		Scriptable jsCmd = DSS_CONVERTOR.toScriptable(cx, cmd, controllerScope);
		JsHttpRequest jsReq = (JsHttpRequest) cx.newObject(controllerScope, "HttpRequest", new Object[] {
			request,
			STRUCT.buildObject(
				REQUEST_EXTRA_PATTERNID, patternId
			)
		});
		return RHINO.callFunctionInScope(cx, controllerScope, function, new Object[] { jsCmd, jsReq });
	}
	

	private Scriptable buildScope(Context cx) throws Exception {
		if (scopeFactory == null) {
			if (cachedScope == null) {
				cachedScope = RHINO.createTopScope(cx, allowJavaPackageAccess);
			}
			return RHINO.protocloneScriptable(cx, cachedScope);
		} else {
			return scopeFactory.createInContext(cx);
		}
	}
	
	
	
	/* setters */

	public void setMergeGetAndPostFormParams(boolean mergeGetAndPostFormParams) {
		this.mergeGetAndPostFormParams = mergeGetAndPostFormParams;
	}
	
	public void setMergeEntityDataObject(boolean mergeEntityDataObject) {
		this.mergeEntityDataObject = mergeEntityDataObject;
	}
	
	public void setSpringScopeFactory(SpringScopeFactory scopeFactory) {
		allowJavaPackageAccess = scopeFactory.isAllowJavaPackageAccess();
		rhinoEmbedding = scopeFactory.getEmbedding();
		this.scopeFactory = scopeFactory;
	}

	public void setSitemap(Sitemap sitemap) {
		this.sitemap = sitemap;
	}
	
	public void setFunction(String function) {
		this.function = function;
	}
	
	public void setPatternId(String patternId) {
		this.patternId = patternId;
	}

	public void setUriSpec(String uriSpecTemplate) {
		this.uriSpec = new DataObjectURISpec(uriSpecTemplate);
	}
}
