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
package com.nominanuda.lang;


import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.nominanuda.codec.Hex;

public class MathsTest {

	@Test
	public void testGetBytes() {
		long l = 0xFF00EE11DD22CC33L;
		byte[] b = Maths.getBytes(l);
		assertEquals("FF00EE11DD22CC33".toLowerCase(), Hex.encode(b));
	}

	@Test
	public void testAsUnsignedByte() {
		assertEquals(128, Maths.asUnsignedByte((byte)-128));
		assertEquals(131, Maths.asUnsignedByte((byte)-125));
		assertEquals(0  , Maths.asUnsignedByte((byte)0));
		assertEquals(127, Maths.asUnsignedByte((byte)127));
		assertEquals(254, Maths.asUnsignedByte((byte)-2));
		assertEquals(255, Maths.asUnsignedByte((byte)-1));
	}
}
