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

import static com.nominanuda.zen.common.Str.STR;
import static com.nominanuda.zen.seq.Seq.SEQ;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import com.nominanuda.zen.common.Check;

public class JQueryAptProcessor implements Processor {
	private ProcessingEnvironment env;
	String prologue1 = //"(function() {var dummy = function(){};var uritpl = function(spec, model) {var pathAndQuery = spec.split('?');var re = /\\{[^\\}]+\\}/g;var result = pathAndQuery[0].replace(re,function(str, p1, p2, offset, s) {return model[str.replace(/\\{(\\w+).*/, '$1')];});if(pathAndQuery.length > 1) {result += '?' + pathAndQuery[1].replace(re,function(str, offset) {var x = str.replace(/\\{(\\w+).*/, '$1');var p = model[x];var isPname = offset === 0 || pathAndQuery[1].charAt(offset - 1) ==! '=';return ''+ (isPname ? x+'='+p : p);});};return result;};";
"(function() {\n"+
"var dummy = function(){};\n"+
"var uritpl = function(spec, model) {\n"+
"  var globalObj = window.";
	String prologue2 = ";\n" +
"  var uriPrefix = globalObj.globalPrefix || '';\n" +
"  var pathAndQuery = spec.split('?');\n"+
"  var re = /\\(?\\{[^\\}]+\\}\\)?/g;\n"+
"  var result = pathAndQuery[0].replace(re,function(str, p1, p2, offset, s) {\n"+
"    return model[str.replace(/\\{(\\w+).*/, '$1')];\n"+
"  });\n"+
"  if(pathAndQuery.length > 1) {\n"+
"    result += '?' + pathAndQuery[1].replace(re,function(str, offset) {\n"+
"      var x = str.replace(/\\(?\\{(\\w+).*/, '$1');\n"+
"      var p = model[x];\n"+
"      if(!((p === null||typeof p === 'undefined') && pathAndQuery[1].charAt(offset)==='(')){\n"+
"      var isPname = offset === 0 || pathAndQuery[1].charAt(offset - 1) ==! '=';\n"+
"      return ''+ (isPname ? x+'='+p : p);\n"+
"    }}).replace(/\\s/g,'&');\n"+
"  }\n"+
"  return uriPrefix+result;\n"+
"};\n";

	
	
	String epilogue = "})();";

	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {
		log("process");
		for(Element el :  roundEnv.getElementsAnnotatedWith(HyperApi.class)) {
			processApi((TypeElement)el, roundEnv);
			//log(el.getSimpleName().toString());
		}
		return true;
	}
	//		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	//		StandardJavaFileManager fileManager = compiler.
	//		        getStandardFileManager(null, null, null);


	//window.hyperapi_<type> = {
//  <method> : function(x,y,z,okcb,errcb) {
//    var uri = uritpl('<tpl>',{x:x,y:y...});//TODO nested...
//    var _errcb = errcb || dummy;
////TODO uri PREFIX
//    jQuery.ajax({
//      url:uri,
//      
//    });
//  },
//}
	private void processApi(TypeElement el, RoundEnvironment roundEnv) {
		try {
			String targetType = el.getQualifiedName().toString();
			log("targetType:"+targetType);
			String pkg = targetType.substring(0, targetType.lastIndexOf('.'));
			String ns = "hyperapi_"+targetType.replace('.', '_');
			log("detected package:"+pkg);
			FileObject jsSrc = env.getFiler().createResource(StandardLocation.CLASS_OUTPUT, pkg, ns+".js", el);
			Writer w = jsSrc.openWriter();
			w.write(prologue1);
			w.write(ns);
			w.write(prologue2);
			w.write("window."+ns+" = {");
			for(Element member : el.getEnclosedElements()) {
				if(member.getKind() == ElementKind.METHOD) {
					log(member.getSimpleName().toString());
					writeMethodCall((ExecutableElement)member, w, ns);
				}
			}
			w.write("};\n");
			w.write(epilogue);
			w.close();
			log("generating:"+jsSrc.toUri().toString());
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeMethodCall(ExecutableElement member, Writer w, String ns) throws IOException {
		List<? extends VariableElement> params = member.getParameters();
		Path uriTpl = member.getAnnotation(Path.class);
		if(uriTpl == null) {
			log("skipping "+member.getSimpleName()+" because misses @Path annotation");
			return;
		}
		String method = member.getAnnotation(GET.class) != null
			? "GET" : member.getAnnotation(POST.class) != null
			? "POST" : member.getAnnotation(PUT.class) != null
			? "PUT" : member.getAnnotation(DELETE.class) != null
			? "DELETE" : (String)Check.illegalstate.fail();
		List<String> pNames = new LinkedList<String>();
		StringBuilder uriTplParams = new StringBuilder("{");
		String jsonBody = null;
		for(VariableElement p: params) {
			String jsonPath = null;
			PathParam pp = p.getAnnotation(PathParam.class);
			if(pp != null) {
				jsonPath = pp.value();
			} else {
				QueryParam qp = p.getAnnotation(QueryParam.class);
				if(qp != null) {
					jsonPath = qp.value();
				}
			}
			@SuppressWarnings("unused")
			String clsType = null;//p.asType().getClass().getName();
			TypeMirror tm = p.asType();
			if(tm instanceof DeclaredType) {
				clsType = ((DeclaredType)tm).asElement().getSimpleName().toString();
			} else if(tm instanceof PrimitiveType) {
				clsType = "PRIMITIVE";//((PrimitiveType)tm).asElement().getSimpleName().toString();
			} else {
				Check.illegalstate.fail();
			}
			String pname = p.getSimpleName().toString();
			pNames.add(pname);
			if(jsonPath != null) {
				//TODO a.b.c
				uriTplParams.append(jsonPath+":"+pname+",");
			} else {
				jsonBody = pname;
			}
		}
		uriTplParams.append("}");
		String sig = STR.join(", ", pNames);
		if(pNames.size() > 0) {
			sig = sig + ", ";
		}
		String content = "";
		if(jsonBody != null && ("POST".equals(method) || "PUT".equals(method))) {
			content = ",contentType:'application/json;charset=UTF-8',data:JSON.stringify("+jsonBody+")";
		}
		w.write("\n"+member.getSimpleName().toString()+" : function("+sig+"okcb,errcb)  {\n" +
			"var uri = uritpl('"+uriTpl.value()+"',"+uriTplParams.toString()+");\n" +
			"var _errcb = errcb || dummy;\n" +
			"jQuery.ajax({type:'"+method+"',\n url:uri,\n success:okcb,\n error:_errcb" + content +
			"\n});\n},");
	}


	public void init(ProcessingEnvironment processingEnv) {
		log("init");
		env = processingEnv;
	}

	public Set<String> getSupportedOptions() {
		log("getSupportedOtions");
		return SEQ.hashSet();
	}

	public Set<String> getSupportedAnnotationTypes() {
		log("getSupportedAnnotationTypes");
		return SEQ.hashSet(HyperApi.class.getName());
	}

	public SourceVersion getSupportedSourceVersion() {
		log("getSupportedSourceVersion");
		return SourceVersion.RELEASE_6;
	}

	public Iterable<? extends Completion> getCompletions(Element element,
			AnnotationMirror annotation, ExecutableElement member,
			String userText) {
		log("getCompletions");
		return SEQ.hashSet();
	}

	private void log(String msg) {
		System.err.println(msg);
		//System.out.println(msg);
	}
}
//(function() {
//var dummy = function(){};
//var uritpl = function(spec, model) {
//  var pathAndQuery = spec.split('?');
//  var re = /\{[^\}]+\}/g;
//  var result = pathAndQuery[0].replace(re,function(str, p1, p2, offset, s) {
//    return model[str.replace(/\{(\w+).*/, '$1')];
//  });
//  if(pathAndQuery.length > 1) {
//    result += '?' + pathAndQuery[1].replace(re,function(str, offset) {
//      var x = str.replace(/\{(\w+).*/, '$1');
//      var p = model[x];
//      var isPname = offset === 0 || pathAndQuery[1].charAt(offset - 1) ==! '=';
//      return ''+ (isPname ? x+'='+p : p);
//    });
//  }
//  return result;
//};
//window.hyperapi_<type> = {
//  <method> : function(x,y,z,okcb,errcb) {
//    var uri = uritpl('<tpl>',{x:x,y:y...});//TODO nested...
//    var _errcb = errcb || dummy;
////TODO uri PREFIX
//    jQuery.ajax({
//      url:uri,
//      
//    });
//  },
//}
//})();
