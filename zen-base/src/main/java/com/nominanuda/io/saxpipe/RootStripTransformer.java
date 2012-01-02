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
package com.nominanuda.io.saxpipe;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class RootStripTransformer extends ForwardingTransformerHandlerBase {
	private int depth = 0;

	@Override
	public void startDocument() throws SAXException {};
	@Override
	public void endDocument() throws SAXException {};
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		if(depth > 0) {
			super.startElement(uri, localName, qName, atts);
		}
		depth++;
	}
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		depth--;
		if(depth > 0) {
			super.endElement(uri, localName, qName);
		}
	}

}
