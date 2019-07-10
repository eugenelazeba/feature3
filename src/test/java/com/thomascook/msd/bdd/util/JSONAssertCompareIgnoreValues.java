package com.thomascook.msd.bdd.util;

import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JSONAssertCompareIgnoreValues extends DefaultComparator {

    private Set<String> ignoredPaths = new HashSet<>();

    public JSONAssertCompareIgnoreValues(JSONCompareMode lenient, String... ignorePath) {
        super(JSONCompareMode.STRICT);
        ignoredPaths.addAll(Arrays.asList(ignorePath));
    }

    @Override
    public void compareValues(String prefix, Object expectedValue, Object actualValue, JSONCompareResult result) throws JSONException {
        if (!ignoredPaths.contains(prefix)) {
            super.compareValues(prefix, expectedValue, actualValue, result);
        }
    }
}