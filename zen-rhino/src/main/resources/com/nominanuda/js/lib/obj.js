function CAST_toArray(o, fnc) { // redefined here just to avoid require
	var arr = ((o !== undefined && o !== null) ? o.splice ? o : [o] : []);
	return fnc ? arr.map(fnc) : arr;
}


function nth(obj, pos) {
	var k = keys(obj)[pos];
	return k ? obj[k] : undefined;
}

function first(obj) {
	return nth(obj, 0);
}

function last(obj) {
	var ks = keys(obj);
	var k = ks[ks.length - 1];
	return k ? obj[k] : undefined;
}

function keys(obj) {
	return Object.keys(obj);
}

function vals(obj) {
	var vals = [];
	for (var k in obj) {
		vals.push(obj[k]);
	}
	return vals;
}

function map(obj, valueFnc, keyFnc) { // first valueFnc because keyFnc is normally unused
	if (obj) {
		var map = {};
		(keyFnc == null) && (keyFnc = function(k, v) { // if null/undefined use key
			return k;
		});
		(valueFnc == null) && (valueFnc = function(k, v) { // if null/undefined use value
			return v;
		});
		for (var k in obj) {
			var v = obj[k];
			map[keyFnc(k, v)] = valueFnc(k, v);
		}
		return map;
	}
	return obj;
}

function arr(obj, fnc) {
	return obj ? Object.keys(obj).map(function(key) {
		return fnc(key, obj[key]);
	}) : [];
}

function _eval(obj) { // with "_" to keep eval() visible
	cycle(obj, function(k, v) {
		delete obj[k];
		obj[eval(k)] = v;
	});
	return obj;
}

function cycle(obj, fnc) {
	if (obj) {
		for (var k in obj) {
			fnc(k, obj[k]);
		}
	}
}

function filter(obj, fnc) {
	if (obj) {
		var result = {};
		for (var k in obj) {
			fnc(k, obj[k]) && (result[k] = obj[k]);
		}
		return result;
	}
	return null;
}

function slice(src1, props, src2) { // shrink object to selected properties
	var dst = {};
	src1 && CAST_toArray(props).forEach(function(p) {
		dst[p] = src1[p];
	});
	return merge(src2, dst);
}

function merge(src, dst, fnc) { // add all properties (override)
	if (src) {
		dst = dst || {};
		for (var p in src) {
			dst[p] = (fnc ? fnc(src[p], p) : src[p]);
		}
	}
	return dst;
}

function expand(src, dst, fnc) { // add missing properties (no override)
	if (src) {
		dst = dst || {};
		for (var p in src) {
			if (dst[p] === undefined) {
				dst[p] = (fnc ? fnc(src[p], p) : src[p]);
			}
		}
	}
	return dst;
}

function override(src, dst, fnc) { // override existing properties
	if (src) {
		dst = dst || {};
		for (var p in dst) {
			dst[p] = (fnc ? fnc(src[p], p) : src[p]);
		}
	}
	return dst;
}

function replace(src, dst, fnc) { // replace dst properties with src properties
	if (src) {
		if (dst) {
			for (var p in dst) {
				delete dst[p];
			}
		}
		return merge(src, dst, fnc);
	}
	return dst;
}

function build() {
	var obj = {};
	for (var i=0; i<arguments.length;) {
		obj[arguments[i++]] = arguments[i++];
	}
	return obj;
}

function flatten(prefix, obj, map) {
	map = map || {};
	if (obj) {
		(function paramize(name, value) {
			if (value) {
				if (value.splice) {
					value.forEach(function(v) {
						paramize(name, v);
					});
				} else if (typeof value == 'object') {
					for (var p in value) {
						paramize((name && name + '.' || '') + p, value[p]);
					}
				} else {
					map[name] = value;
				}
			}
		})(prefix, obj);
	}
	return map;
}

function contains(obj, value) {
	for (var p in obj) {
		if (obj[p] === value) {
			return p
		}
	}
	return null;
}

function prefix(obj, prefix) { // adds prefix to all keys
	if (obj) {
		for (var k in obj) {
			obj[prefix + k] = obj[k];
			delete obj[k];
		}
	}
	return obj;
}

function evict(obj, key) {
	var v = obj[key];
	delete obj[key];
	return v;
}


exports = {
	nth: nth,
	first: first,
	last: last,
	keys: keys,
	vals: vals,
	map: map,
	arr: arr,
	eval: _eval,
	cycle: cycle,
	filter: filter,
	slice: slice,
	merge: merge,
	expand: expand,
	override: override,
	replace: replace,
	build: build,
	flatten: flatten,
	contains: contains,
	prefix: prefix,
	evict: evict
};