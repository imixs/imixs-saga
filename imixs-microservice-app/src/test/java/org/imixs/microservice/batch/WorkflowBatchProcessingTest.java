package org.imixs.microservice.batch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.imixs.melman.BasicAuthenticator;
import org.imixs.melman.WorkflowClient;
import org.imixs.microservice.test.IntegrationTest;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.util.Base64;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * This test shows an example for batch events.
 * 
 * @author rsoika
 */
public class WorkflowBatchProcessingTest {

	static String BASE_URL = "http://localhost:8080/api/";
	static String USERID = "admin";
	static String PASSWORD = "adminadmin";
	static String MODEL_VERSION = "order-en-1.0";

	WorkflowClient workflowCLient = null;

	private IntegrationTest integrationTest = new IntegrationTest(BASE_URL);

	private static Logger logger = Logger.getLogger(WorkflowBatchProcessingTest.class.getName());

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

		try {
			deployBPMNModel("/order-en-1.0.0.bpmn");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		workflowCLient = new WorkflowClient(BASE_URL);
		// create a default basic authenticator
		BasicAuthenticator basicAuth = new BasicAuthenticator(USERID, PASSWORD);
		// register the authenticator
		workflowCLient.registerClientRequestFilter(basicAuth);

	}

	/**
	 * create new order based on a Imixs ItemCollection object using the Imixs
	 * WorkflowTestSuite.
	 * <p>
	 * The model creates a batchEvent which will be processed by the
	 * batchEventService with a delay of 2 seconds
	 * 
	 */
	@Test
	public void createNewWorkitemTest() {

		ItemCollection ticket = new ItemCollection();
		ticket.replaceItemValue("type", "workitem");
		ticket.replaceItemValue("$ModelVersion", MODEL_VERSION);
		ticket.replaceItemValue("$taskid", 1000);
		ticket.replaceItemValue("$eventid", 10);
		ticket.replaceItemValue("txtName", "Test");
		ticket.replaceItemValue("namTeam", Arrays.asList(new String[] { "admin", "alex", "marty" }));
		try {
			logger.info("...processing workitem with a batch event...");
			ticket = workflowCLient.processWorkitem(ticket);

			// wait 5 seconds for the BatchEventScheduler...
			logger.info("...waiting 5 seconds...");
			TimeUnit.SECONDS.sleep(5);

			// reload workitem ....
			logger.info("...reloading workitem...");
			ticket = workflowCLient.getWorkitem(ticket.getUniqueID());

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(ticket);
		String uid = ticket.getUniqueID();

		Assert.assertFalse(uid.isEmpty());

		Assert.assertEquals(1200, ticket.getTaskID());

		logger.info("...batch event sucessful processed.");

	}

	/**
	 * This method deploys a BPMN model into the running workflow instance via the
	 * Imixs-Rest API
	 * 
	 * @throws Exception
	 */
	public void deployBPMNModel(String modelFilePath) throws Exception {
		PrintWriter printWriter = null;
		HttpURLConnection urlConnection = null;
		try {
			String serviceEndpoint = BASE_URL + "model/bpmn";

			urlConnection = (HttpURLConnection) new URL(serviceEndpoint).openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.setAllowUserInteraction(false);

			// set Authorization HEADER
			urlConnection.setRequestProperty("Authorization", "Basic " + this.getAccessByUser());

			// set Content_Type
			urlConnection.setRequestProperty("Content-Type", "application/xml; charset=" + "UTF-8");

			StringWriter writer = new StringWriter();

			// read model as a resource stream
			InputStream inputStream = getClass().getResourceAsStream(modelFilePath);
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			String read;
			while ((read = br.readLine()) != null) {
				writer.write(read);
			}
			br.close();
			// compute length
			urlConnection.setRequestProperty("Content-Length",
					"" + Integer.valueOf(writer.toString().getBytes().length));
			printWriter = new PrintWriter(
					new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8")));
			printWriter.write(writer.toString());
			printWriter.close();

			String sHTTPResponse = urlConnection.getHeaderField(0);
			int iLastHTTPResult = Integer.parseInt(sHTTPResponse.substring(9, 12));
			if (iLastHTTPResult < 200 || iLastHTTPResult >= 300) {
				throw new ModelException(ModelException.INVALID_MODEL,
						"Deployment of Model '" + modelFilePath + "' failed");

			}

		} catch (Exception ioe) {
			throw ioe;
		} finally {
			// Release current connection
			if (printWriter != null)
				printWriter.close();
		}

	}

	/**
	 * This method decodes the user id and password used for basic authentication
	 * 
	 */
	private String getAccessByUser() {
		String sURLAccess = "";
		String sUserCode = USERID + ":" + PASSWORD;
		char[] authcode = Base64.encode(sUserCode.getBytes());
		sURLAccess = String.valueOf(authcode);
		return sURLAccess;
	}

}