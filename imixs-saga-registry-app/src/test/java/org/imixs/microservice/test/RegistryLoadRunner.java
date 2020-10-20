package org.imixs.microservice.test;

import java.util.Arrays;

import org.imixs.melman.BasicAuthenticator;
import org.imixs.melman.WorkflowClient;
import org.imixs.workflow.ItemCollection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * This test simmulates batch processing with high load
 * 
 * @author rsoika
 *
 */
public class RegistryLoadRunner {

	static String BASE_URL = "http://localhost:8081/api";
	static String USERID = "admin";
	static String PASSWORD = "adminadmin";
	static String MODEL_VERSION = "1.0";

	WorkflowClient workflowCLient = null;

	private IntegrationTest integrationTest = new IntegrationTest(BASE_URL);

	/**
	 * The setup method deploys the ticket workflow into the running workflow
	 * instance.
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
	 * Run 100 creations of a process instance...
	 */
	@Test
	public void create100Test() {

		int MAX_COUNT = 100;
		int count = 0;
		long l = System.currentTimeMillis();
		System.out.println("Start creat10Test...");
		while (count < MAX_COUNT) {
			createNewWorkitemTest("Some data...." + count);
			count++;
		}
		long time = System.currentTimeMillis() - l;
		System.out.println("Completed creat10Test in " + (time) + "ms");

		System.out.println("  - processing time ~" + (time / 100) + "ms");

	}

	/**
	 * create new Ticket based on a Imixs ItemCollection object using the Imixs
	 * RestClient.
	 * 
	 */
	void createNewWorkitemTest(String subject) {

		ItemCollection ticket = new ItemCollection();
		ticket.replaceItemValue("type", "workitem");
		ticket.replaceItemValue("$ModelVersion", MODEL_VERSION);
		ticket.replaceItemValue("$taskid", 1000);
		ticket.replaceItemValue("$eventid", 10);
		ticket.replaceItemValue("txtName", subject);
		ticket.replaceItemValue("namTeam", Arrays.asList(new String[] { "admin", "alex", "marty" }));
		try {
			ticket = workflowCLient.processWorkitem(ticket);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(ticket);

	}

	

}