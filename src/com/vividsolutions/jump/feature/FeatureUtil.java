package com.vividsolutions.jump.feature;

public class FeatureUtil {

	private static int lastID = 0;

	public static int nextID() { return ++lastID; }
}
