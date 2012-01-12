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
 */package com.nominanuda.solr;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import com.nominanuda.dataobject.DataArray;
import com.nominanuda.dataobject.DataArrayImpl;
import com.nominanuda.dataobject.DataObject;
import com.nominanuda.dataobject.DataStructHelper;

public class SolrHelper {
	private static final DataStructHelper struct = new DataStructHelper();

	public DataArray listResultsDotAware(SolrServer solr, SolrQuery sq, int start, int count) throws SolrServerException {
		DataArray res = new DataArrayImpl();
		sq.setStart(start);
		sq.setRows(count);
		QueryResponse qr = solr.query(sq);
		qr.getResults();
		SolrDocumentList sdl = qr.getResults();
		for(Map<String,Object> d : sdl) {
			DataObject o = (DataObject)struct.fromFlatMap(d);
			res.add(o);
		}
		return res;
	}
	

	public String escAndQuote(String s) {
		return '"' + esc(s) + '"';
	}

	public String esc(String s) {
		return ClientUtils.escapeQueryChars(s);
	}

	//TODO more than one level + nested fields naming convention
	public SolrInputDocument sid(DataObject obj, Set<String> solrFields) {
		SolrInputDocument sid = new SolrInputDocument();
		for(String k : obj.getKeys()) {
			Object val = obj.get(k);
			if(val == null) {
				continue;
			} else if(struct.isPrimitiveOrNull(val)) {
				addField(sid, k, val, solrFields);
			} else if(struct.isDataObject(val)) {
				DataObject o = (DataObject) val;
				for(String k1 : o.getKeys()) {
					Object val1 = o.get(k1);
					if(val1 == null) {
						continue;
					} else if(struct.isPrimitiveOrNull(val1)) {
						addField(sid, k+"."+k1, val1, solrFields);
					}
				}
			} else if(struct.isDataArray(val)) {
				DataArray a = (DataArray) val;
				Collection<Object> vals = new LinkedList<Object>();
				for(Object val1 : a) {
					if(val1 == null) {
						continue;
					} else if(struct.isPrimitiveOrNull(val1)) {
						vals.add(val1);
					}
				}
				setField(sid, k, vals, solrFields);
			} 
		}
		return sid;
	}
	private void addField(SolrInputDocument sid, String key, Object val, Set<String> solrFields) {
		if(solrFields.contains(key)) {
			sid.addField(key, val);
		}
	}
	private void setField(SolrInputDocument sid, String key, Object val, Set<String> solrFields) {
		if(solrFields.contains(key)) {
			sid.setField(key, val);
		}
	}

}