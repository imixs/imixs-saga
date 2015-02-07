package org.imixs.microservice.test;

import java.util.logging.Level;

import junit.framework.Assert;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.services.rest.RestClient;
import org.imixs.workflow.test.WorkflowTestSuite;
import org.junit.Test;

/**
 * This test shows an example how to post a workitem and how to get the worklist
 * from the RestService API.
 * 
 * The test uses the Imixs WorkflowTestSuite and shows also how to post a JSON
 * Object to the Rest API.
 * 
 * 
 * @author rsoika
 *
 */
public class WorkflowTest {
	
	static String BASE_URL = "http://localhost:8080/imixs-microservice/";
	static String USERID = "admin";
	static String PASSWORD = "adminadmin";
	
	WorkflowTestSuite testSuite = null;

	/**
	 * create new Ticket based on a Imxis ItemCollection object using the Imixs
	 * WorkflowTestSuite.
	 * 
	 */
	@Test
	public void createNewWorkitemTest() {
		
		testSuite = WorkflowTestSuite.getInstance();
		testSuite.setHost(BASE_URL);
		testSuite.joinParty(USERID, PASSWORD);
		testSuite.joinParty("Anonymous", null);
		
		
		ItemCollection registration = new ItemCollection();
		registration.replaceItemValue("type", "workitem");
		registration.replaceItemValue("$ModelVersion", "1.0.0");
		registration.replaceItemValue("$processid", 10);
		registration.replaceItemValue("$activityid", 10);
		registration.replaceItemValue("txtName", "Test");

		registration = testSuite.processWorkitem(registration, "admin");

		Assert.assertNotNull(registration);
		String uid = registration.getItemValueString("$UniqueID");
		WorkflowTestSuite.log(Level.INFO, "UID=" + uid);
		Assert.assertFalse(uid.isEmpty());
	}
	
	
	
	
	
	
	
	
	
	/**
	 * create new Ticket based on a JSON String
	 * 
	 * 
	 */
	@Test
	public void createNewWorkitemJSONTest() {
		
		RestClient restClient = new RestClient();
		restClient.setCredentials(USERID, PASSWORD);
		
		
		String uri = BASE_URL + "workflow/workitem.json";

		// create a json test string
		String json = "{\"item\":["
				+ "     {\"name\":\"type\",\"value\":{\"@type\":\"xs:string\",\"$\":\"workitem\"}},"
				+ "     {\"name\":\"$modelversion\",\"value\":{\"@type\":\"xs:string\",\"$\":\"1.0.0\"}},"
				+ "     {\"name\":\"$processid\",\"value\":{\"@type\":\"xs:int\",\"$\":\"10\"}},"
				+ "     {\"name\":\"$activityid\",\"value\":{\"@type\":\"xs:int\",\"$\":\"10\"}},"
				+ "     {\"name\":\"txtname\",\"value\":{\"@type\":\"xs:string\",\"$\":\"test-json\"}}"
				+ "]}";

		try {
			int httpResult = restClient.postJsonEntity(uri, json);
			String sContent = restClient.getContent();
			// expected result 200
			Assert.assertEquals(200, httpResult);

			Assert.assertTrue(sContent.indexOf("txtname") > -1);
		} catch (Exception e) {

			e.printStackTrace();
			Assert.fail();
		}

	}
	
	
	
}