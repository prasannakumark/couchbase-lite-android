/**
 * Copyright (c) 2016 Couchbase, Inc. All rights reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.couchbase.lite.mockserver;

import com.couchbase.lite.Manager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

/*

    Mock response to calling GET on a checkpoint where it returns a valid lastSequence
    (as opposed to returning a 404)

    {
       "_id":"_local/7d3186e30a82a3312fc2c54098a25ce568cd7dfb",
       "ok":true,
       "_rev":"0-1",
       "lastSequence":"2"
    }

 */
public class MockCheckpointGet implements SmartMockResponse {

    private String id;
    private String ok;
    private String rev;
    private String lastSequence;
    private boolean sticky;
    private boolean is404;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOk() {
        return ok;
    }

    public void setOk(String ok) {
        this.ok = ok;
    }

    public String getRev() {
        return rev;
    }

    public void setRev(String rev) {
        this.rev = rev;
    }

    public String getLastSequence() {
        return lastSequence;
    }

    public void setLastSequence(String lastSequence) {
        this.lastSequence = lastSequence;
    }

    private Map<String, Object> generateMap() {
        Map<String, Object> docMap = new HashMap<String, Object>();
        docMap.put("_id", getId());
        docMap.put("ok", getOk());
        docMap.put("_rev", getRev());
        docMap.put("lastSequence", getLastSequence());
        return docMap;
    }

    private String generateBody() {
        Map documentMap = generateMap();
        try {
            return Manager.getObjectMapper().writeValueAsString(documentMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MockResponse generateMockResponse(RecordedRequest request) {

        if (!request.getMethod().equals("GET")) {
            throw new RuntimeException("Expected GET, but was not a GET");
        }

        MockResponse mockResponse = new MockResponse();
        if (is404) {
            MockHelper.set404NotFoundJson(mockResponse);
            return mockResponse;
        }

        // extract id from request
        // /db/_local/e11a8567a2ecaf27c52d02899fa82258a343d720 -> _local/e11a8567a2ecaf27c52d02899fa82258a343d720
        String path = request.getPath();
        String localDocId = "";
        Pattern pattern = Pattern.compile("/db/_local/(.*)");
        Matcher matcher = pattern.matcher(path);
        if (matcher.find()) {
            localDocId = matcher.group(1);
        } else {
            throw new RuntimeException(String.format(Locale.ENGLISH, "Could not extract local doc id from: %s", path));
        }

        // call setId
        setId(String.format(Locale.ENGLISH, "_local/%s", localDocId));
        mockResponse.setBody(generateBody());
        MockHelper.set200OKJson(mockResponse);
        return mockResponse;
    }

    @Override
    public boolean isSticky() {
        return this.sticky;
    }

    @Override
    public long delayMs() {
        return 0;
    }

    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    public void set404(boolean is404) {
        this.is404 = is404;
    }
}
