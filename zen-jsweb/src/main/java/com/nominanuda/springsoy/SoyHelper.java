package com.nominanuda.springsoy;

import static com.nominanuda.dataobject.DataStructHelper.STRUCT;

import java.util.List;
import java.util.Map;

import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.nominanuda.dataobject.DataObject;

public class SoyHelper {
	
	public static SoyMapData model2soy(DataObject json) {
		return model2soy(STRUCT.toMapsAndLists(json));
	}
	
	@SuppressWarnings("unchecked")
	public static SoyMapData model2soy(Map<String, ? super Object> m) {
		SoyMapData result = new SoyMapData();
		for (String k : m.keySet()) {
			Object o = m.get(k);
			if (o instanceof Map) {
				result.put(k, model2soy((Map<String, Object>)o));
			} else if (o instanceof List) {
				result.put(k, model2soy((List<Object>)o));
			} else if (o instanceof Long) {
				result.put(k, ((Long)o).floatValue());
			} else {
				result.put(k, o);
			}
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static SoyListData model2soy(List<Object> l) {
		SoyListData result = new SoyListData();
		for (Object o : l) {
			if (o instanceof Map) {
				result.add(model2soy((Map<String, Object>)o));
			} else if (o instanceof List) {
				result.add(model2soy((List<Object>)o));
			} else if (o instanceof Long) {
				result.add(((Long)o).floatValue());
			} else {
				result.add(o);
			}
		}
		return result;
	}
}