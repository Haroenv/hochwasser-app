package de.bitdroid.flooding.utils;


import junit.framework.TestCase;

import de.bitdroid.flooding.ods.data.SQLiteType;

public class SQLiteTypeTest extends TestCase {

    public void testTypes() {
        assertEquals("INTEGER", SQLiteType.INTEGER.toString());
        assertEquals("REAL", SQLiteType.REAL.toString());
        assertEquals("TEXT", SQLiteType.TEXT.toString());
        assertEquals("BLOB", SQLiteType.BLOB.toString());
        assertEquals("NULL", SQLiteType.NULL.toString());
    }

}
