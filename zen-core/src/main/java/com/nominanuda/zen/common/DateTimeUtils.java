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
package com.nominanuda.zen.common;

import java.util.regex.Pattern;

import javax.annotation.concurrent.ThreadSafe;
import javax.xml.datatype.DatatypeFactory;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;


@ThreadSafe
public class DateTimeUtils { 
	public static final DateTimeUtils DT = new DateTimeUtils();
	private final DatatypeFactory dtf;
	{
		try {
			dtf = DatatypeFactory.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	private static final String DATE = "\\d{4}-\\d{2}-\\d{2}";
	private static final String TIMESEC = "T\\d{2}:\\d{2}:\\d{2}";
	private static final String MILLIS = "\\.\\d{3}";
	private static final String TZ = "(?:Z|[\\+-]\\d{2}[:]?(?:\\d{2})?)?";
	private static final Pattern DATE_RE = Pattern.compile("^"+DATE+"$");
	private static final Pattern SECS_TZ_RE = Pattern.compile("^" + DATE + TIMESEC+TZ+"$");
	private static final Pattern MILLIS_TZ_RE = Pattern.compile("^" + DATE + TIMESEC+MILLIS+TZ+"$");
	public final String ISO_EPOCH_DT_SEC = "1970-01-01T00:00:00Z";

	private static final DateTimeFormatter isoUtcSecs = ISODateTimeFormat
		.dateTimeNoMillis().withZone(DateTimeZone.UTC);
	private static final DateTimeFormatter isoUtcMillis = ISODateTimeFormat
		.dateTime()
		.withZone(DateTimeZone.UTC);
	private static final DateTimeFormatter isoUtcDate = ISODateTimeFormat
		.date()
		.withZone(DateTimeZone.UTC);

	private String toISO8601UtcSecs(DateTime dt) {
		return isoUtcSecs.print(dt);
	}
	private String toISO8601UtcMillis(DateTime dt) {
		return isoUtcMillis.print(dt);
	}
	private String toISO8601UtcDate(DateTime dt) {
		return isoUtcDate.print(dt);
	}

	public String toISO8601UtcSecs(long utcEpochMillis) {
		return toISO8601UtcSecs(new DateTime(utcEpochMillis, DateTimeZone.UTC));
	}
	public String toISO8601UtcMillis(long utcEpochMillis) {
		return toISO8601UtcMillis(new DateTime(utcEpochMillis, DateTimeZone.UTC));
	}
	public String toISO8601UtcDate(long utcEpochMillis) {
		return toISO8601UtcDate(new DateTime(utcEpochMillis, DateTimeZone.UTC));
	}

	public String nowToISO8601UtcSecs() {
		return toISO8601UtcSecs(new DateTime(DateTimeZone.UTC));
	}
	public String nowToISO8601UtcMillis() {
		return toISO8601UtcMillis(new DateTime(DateTimeZone.UTC));
	}
	public String nowToISO8601UtcDate() {
		return toISO8601UtcSecs(new DateTime(DateTimeZone.UTC));
	}

	public long fromISO8601UtcDate(String isoDt) throws IllegalArgumentException {
		return isoUtcDate.parseDateTime(isoDt).getMillis();
	}
	public long fromISO8601UtcSecs(String isoDt) throws IllegalArgumentException {
		return isoUtcSecs.parseDateTime(isoDt).getMillis();
	}
	public long fromISO8601UtcMillis(String isoDt) throws IllegalArgumentException {
		return isoUtcMillis.parseDateTime(isoDt).getMillis();
	}
	public long fromISO8601Any(String isoDt) throws IllegalArgumentException {
		try {
			if(DATE_RE.matcher(isoDt).matches()) {
				return fromISO8601UtcDate(isoDt);
			} else if(SECS_TZ_RE.matcher(isoDt).matches()) {
				return fromISO8601UtcSecs(isoDt);
			} else if(MILLIS_TZ_RE.matcher(isoDt).matches()) {
				return fromISO8601UtcMillis(isoDt);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("unrecognized ISO 8601 date:"+isoDt);
		}
		throw new IllegalArgumentException("unrecognized ISO 8601 date:"+isoDt);
	}
	public boolean isISO8601(String isoDt) {
		try {
			fromISO8601Any(isoDt);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @return the difference, measured in milliseconds, between
	 *          the current time and midnight, January 1, 1970 UTC.
	 */
	public long epochMillis() {
		return System.currentTimeMillis();
	}

	public boolean epochMillisBefore(long millis) {
		return epochMillis() < millis;
	}

	public String msecToXsdDur(long msDur) {
		return dtf.newDuration(msDur).toString();
	}
}