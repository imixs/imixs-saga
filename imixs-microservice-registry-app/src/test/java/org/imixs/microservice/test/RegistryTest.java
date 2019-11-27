package org.imixs.microservice.test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import org.imixs.melman.BasicAuthenticator;
import org.imixs.melman.WorkflowClient;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.services.rest.RestClient;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * This test shows an example how to post a workitem against the registry to
 * start a new process instance running on a Imixs-Microservice
 * <p>
 * Note: The test asumes that a model is already deployed on the
 * Imixs-Microservice. Run the WorkflowTest in the imixs-microservice-app module
 * first!
 * <p>
 * To verify the registerd services on the Imixs-Registy endpoint, you can call
 * the API endpoint: http://localhost:8081/api/services
 * 
 * @author rsoika
 */
public class RegistryTest {

	static String BASE_URL = "http://localhost:8081/api";
	static String USERID = "admin";
	static String PASSWORD = "adminadmin";
	static String MODEL_VERSION = "1.0";

	WorkflowClient workflowCLient = null;

	private IntegrationTest integrationTest = new IntegrationTest(BASE_URL);

	private static Logger logger = Logger.getLogger(RegistryTest.class.getName());

	/**
	 * The setup method creates a melman workflowClient.
	 * 
	 * @throws Exception
	 */
	@Before
	public void setup() throws Exception {
		// Assumptions for integration tests
		org.junit.Assume.assumeTrue(integrationTest.connected());

		workflowCLient = new WorkflowClient(BASE_URL);
		// create a default basic authenticator
		BasicAuthenticator basicAuth = new BasicAuthenticator(USERID, PASSWORD);
		// register the authenticator
		workflowCLient.registerClientRequestFilter(basicAuth);

	}

	/**
	 * Post a BusinessEvent with the workflowGroup 'Ticket'
	 * 
	 */
	@Test
	public void createNewWorkitemTest() {
		long l = System.currentTimeMillis();
		ItemCollection ticket = new ItemCollection();
		ticket.replaceItemValue("type", "workitem");
		ticket.replaceItemValue("$workflowGroup", "Ticket");
		ticket.replaceItemValue("_ticketID", 1000);
		ticket.replaceItemValue("_subject", "Some Test Ticket Data");
		try {
			ticket = workflowCLient.processWorkitem(ticket);
			logger.info("process instance created in " + (System.currentTimeMillis() - l) + "ms....");

			Assert.assertNotNull(ticket);
			String uid = ticket.getUniqueID();
			Assert.assertFalse(uid.isEmpty());

			// test get method....
			// wait 2 seconds for the indexer...
			logger.info("...waiting 2 seconds...");
			TimeUnit.SECONDS.sleep(2);

			
			ticket = workflowCLient.getWorkitem(uid);
			Assert.assertNotNull(ticket);
			
			
			
			// get tasklist
			logger.info("...get tasklist/creator/" + USERID);
			List<ItemCollection> tasklist = workflowCLient.getTaskListByCreator(USERID);
			Assert.assertNotNull(tasklist);
			Assert.assertTrue(tasklist.size()>0);
			
			logger.info(tasklist.size() + " workitems found...");
			
			logger.info("...finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/**
	 * create new Ticket based on a JSON String
	 * <p>
	 * The method post a JSON string and accepts XML. The returned XML Content is
	 * converted back into a ItemCollection.
	 * <p>
	 * This code is just to verify the JSON call. Use the Imixs-Melman
	 * WorkflowClient to hide content convention.
	 */
	// @Ignore
	@Test
	public void createNewWorkitemJSONTest() {

		ItemCollection ticket = null;
		RestClient restClient = new RestClient();
		// create a default basic authenticator
		org.imixs.workflow.services.rest.BasicAuthenticator basicAuth = new org.imixs.workflow.services.rest.BasicAuthenticator(
				USERID, PASSWORD);
		// register the authenticator
		restClient.registerRequestFilter(basicAuth);

		// create a json test string
		String json = "{\"item\":" + " [" + "{\"name\": \"type\",\"value\": [\"workitem\"]},"
				+ "{\"name\": \"$modelversion\",\"value\": [\"" + MODEL_VERSION + "\"]},"
				+ "{\"name\": \"$taskid\",\"value\": [1000]}," + "{\"name\": \"$eventid\",\"value\": [10]},"
				+ "{\"name\": \"txtname\",\"value\": [\"test\"]}" + "]}" + "";

		try {
			// post json request, accept XML
			String result = restClient.post(BASE_URL + "/workflow/workitem", json.getBytes(),
					MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML);

			List<ItemCollection> tickets = XMLDataCollectionAdapter.readCollection(result.getBytes());

			// extract 1st workitem...
			Assert.assertNotNull(tickets);
			Assert.assertTrue(tickets.size() > 0);
			ticket = tickets.get(0);

			Assert.assertNotNull(ticket);
			String uid = ticket.getUniqueID();
			Assert.assertFalse(uid.isEmpty());

			// date check
			Date created = ticket.getItemValueDate("$created");
			Assert.assertNotNull(created);
			Assert.assertTrue(created instanceof Date);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

	}

	/**
	 * create new Ticket based on a JSON String
	 * <p>
	 * The method post a JSON string and accepts XML. The returned XML Content is
	 * converted back into a ItemCollection.
	 * <p>
	 * This code is just to verify the typed JSON call. Use the Imixs-Melman
	 * WorkflowClient to hide content convention.
	 */
	@Test
	public void createNewWorkitemJSONTestTyped() {

		ItemCollection ticket = null;
		RestClient restClient = new RestClient();
		// create a default basic authenticator
		org.imixs.workflow.services.rest.BasicAuthenticator basicAuth = new org.imixs.workflow.services.rest.BasicAuthenticator(
				USERID, PASSWORD);
		// register the authenticator
		restClient.registerRequestFilter(basicAuth);

		// create a json test string
		String json = "{\"item\":[" + "     {\"name\":\"type\",\"value\":{\"@type\":\"xs:string\",\"$\":\"workitem\"}},"
				+ "     {\"name\":\"$modelversion\",\"value\":{\"@type\":\"xs:string\",\"$\":\"" + MODEL_VERSION
				+ "\"}}," + "     {\"name\":\"$taskid\",\"value\":{\"@type\":\"xs:int\",\"$\":\"1000\"}},"
				+ "     {\"name\":\"$eventid\",\"value\":{\"@type\":\"xs:int\",\"$\":\"10\"}},"
				+ "     {\"name\":\"namteam\",\"value\":[{\"@type\":\"xs:string\",\"$\":\"admin\"},"
				+ "	{\"@type\":\"xs:string\",\"$\":\"eddy\"},{\"@type\":\"xs:string\",\"$\":\"john\"}]},"
				+ "     {\"name\":\"txtname\",\"value\":{\"@type\":\"xs:string\",\"$\":\"test-json\"}}" + "]}";

		try {
			// post json request, accept XML
			String result = restClient.post(BASE_URL + "/workflow/workitem/typed", json, MediaType.APPLICATION_JSON,
					MediaType.APPLICATION_XML);

			List<ItemCollection> tickets = XMLDataCollectionAdapter.readCollection(result.getBytes());

			// extract 1st workitem...
			Assert.assertNotNull(tickets);
			Assert.assertTrue(tickets.size() > 0);
			ticket = tickets.get(0);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(ticket);
		String uid = ticket.getUniqueID();

		Assert.assertFalse(uid.isEmpty());

	}

}