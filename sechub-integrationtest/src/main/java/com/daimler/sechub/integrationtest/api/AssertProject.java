// SPDX-License-Identifier: MIT
package com.daimler.sechub.integrationtest.api;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import com.daimler.sechub.adapter.AdapterException;
import com.daimler.sechub.adapter.support.JSONAdapterSupport;
import com.daimler.sechub.integrationtest.internal.TestJSONHelper;
import com.daimler.sechub.sharedkernel.project.ProjectAccessLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import junit.framework.AssertionFailedError;

public class AssertProject extends AbstractAssert {

    private TestProject project;

    AssertProject(TestProject project) {
        this.project = project;
    }

    /**
     * Assert the project does not exist in administration domain
     * 
     * @return
     */
    public AssertProject doesNotExist() {
        return doesNotExist(1);
    }
    
    /**
     * Check user does exists 
     * @param tries - amount of retries . Every retry will wait 1 second
     * @return
     */
    public AssertProject doesNotExist(int tries) {
        AssertionFailedError failure = null;
        for (int i=0;i<tries && failure==null;i++) {
            try {
                if (i>0) {
                    /* we wait before next check */
                    TestAPI.waitSeconds(1);
                }
                expectHttpClientError(HttpStatus.NOT_FOUND, () -> fetchProjectDetails(), project.getProjectId() + " found!");
            }catch(AssertionFailedError e) {
                failure=e;
            }
        }
        if (failure!=null) {
            throw failure;
        }
        return this;
    }

    public AssertProject doesExist() {
        try {
            fetchProjectDetails();// will fail with http error when not available
        }catch (HttpClientErrorException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                fail("There is no project existing with id:"+project.getProjectId());
            }else {
                throw e;
            }
        }
        return this;

    }

    /**
     * Asserts the project has expected scan domain access entries
     * 
     * @param expected
     * @return count
     */
    public AssertProject hasAccessEntriesInDomainScan(long expected) {
        String projectId = project.getProjectId();
        long value = getRestHelper().getLongFromURL(getUrlBuilder().buildCountProjectScanAccess(projectId));

        assertEquals("Scan Access amount for project " + projectId + " is not as expected!", value, expected);

        return this;

    }

    /**
     * Asserts the project has expected schedule domain access entries
     * 
     * @param expected
     * @return count
     */
    public AssertProject hasAccessEntriesInDomainSchedule(long expected) {
        String projectId = project.getProjectId();
        long value = getRestHelper().getLongFromURL(getUrlBuilder().buildCountProjectScanAccess(projectId));

        assertEquals("Scan Access amount for project " + projectId + " is not as expected!", value, expected);

        return this;

    }

    public AssertProject hasProductResultsInDomainScan(int expected) {
        String projectId = project.getProjectId();
        long value = getRestHelper().getLongFromURL(getUrlBuilder().buildCountProjectProductResults(projectId));

        if (value != expected) {
            String json = getRestHelper().getJSon(getUrlBuilder().buildFetchAllProjectProductResultsButShrinked(projectId,200));
            fail("Expected product results for project " + projectId + "were : "+expected+" but resulted in: "+ value+"\n\nProduct Results:"+TestJSONHelper.get().beatuifyJSON(json));
        }
        

        return this;
    }

    public AssertProject hasScanReportsInDomainScan(int expected) {
        String projectId = project.getProjectId();
        long value = getRestHelper().getLongFromURL(getUrlBuilder().buildCountProjectScanReports(projectId));

        assertEquals("Product results amount for project " + projectId + " is not as expected!", value, expected);

        return this;
    }

    private String fetchProjectDetails() {
        return getRestHelper().getJSon(getUrlBuilder().buildAdminGetProjectDetailsUrl(project.getProjectId()));
    }

    public AssertProject hasOwner(TestUser user) {
        return hasOwner(user, true);
    }

    private AssertProject hasOwner(TestUser user, boolean expected) {
        String content = fetchProjectDetails();
        String owner = "<undefined>";
        try {
            owner = JSONAdapterSupport.FOR_UNKNOWN_ADAPTER.fetch("owner", content).asText();
        } catch (AdapterException e) {
            e.printStackTrace();
            fail("adapter json failure:" + e.getMessage());
        }
        if (expected && !user.getUserId().equals(owner)) {
            fail("User:" + user.getUserId() + " is NOT owner of project:" + project.getProjectId() + " but:" + owner);
        } else if (!expected && user.getUserId().equals(owner)) {
            fail("User:" + user.getUserId() + " is owner of project:" + project.getProjectId());
        }
        return this;
    }

    public AssertProject hasNotOwner(TestUser user) {
        return hasOwner(user, false);
    }

    public AssertProject hasNoWhiteListEntries() {
        return hasWhiteListEntries();

    }

    public AssertProject hasWhiteListEntries(String... expectedArray) {

        String content = fetchProjectDetails();

        List<String> found = new ArrayList<>();
        ArrayNode whiteListArray;
        try {
            whiteListArray = JSONAdapterSupport.FOR_UNKNOWN_ADAPTER.fetch("whiteList", content).asArray();
            for (JsonNode node : whiteListArray) {
                found.add(node.asText());
            }
        } catch (AdapterException e) {
            e.printStackTrace();
            fail("adapter json failure:" + e.getMessage());
        }
        assertArrayEquals(expectedArray, found.toArray(new String[found.size()]));
        return this;
    }
    
    public AssertProject hasNoMetaData() {
        return hasMetaData(new HashMap<>());
    }
    
    public AssertProject hasMetaData(Map<String, String> expectedMap) {
        String content = fetchProjectDetails();
        
        Map<String, String> found = new HashMap<>();
        JsonNode metaDataNode;
        try {
            metaDataNode = JSONAdapterSupport.FOR_UNKNOWN_ADAPTER.fetch("metaData", content).asNode();
            metaDataNode.fieldNames().forEachRemaining(key -> {
                
                JsonNode element = metaDataNode.get(key);
                
                if (element.isTextual()) {
                    String value = element.textValue();
                    found.put(key, value);
                }
                
            });
        }
        catch (AdapterException e) {
            e.printStackTrace();
            fail("adapter jso failure:" + e.getMessage());
        }
        
        assertEquals(expectedMap, found);
        
        return this;
    }

    public AssertProject hasAccessLevel(ProjectAccessLevel level) {
        Objects.requireNonNull(level,"Your test is wrong implemented! Given project access level may not be null!");
        
        String content = fetchProjectDetails();
        try {
            String id = JSONAdapterSupport.FOR_UNKNOWN_ADAPTER.fetch("accessLevel", content).asText();
            assertEquals("Id: "+id+" was not as " + level.getId(), level, ProjectAccessLevel.fromId(id));
        } catch (AdapterException e) {
            e.printStackTrace();
            fail("adapter json failure:" + e.getMessage());
        }
        return this;
    }

}
