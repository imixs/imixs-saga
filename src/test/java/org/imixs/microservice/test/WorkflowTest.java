package org.imixs.microservice.test;

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
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.imixs.melman.BasicAuthenticator;
import org.imixs.melman.WorkflowClient;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.services.rest.RestClient;
import org.imixs.workflow.util.Base64;
import org.imixs.workflow.xml.XMLDataCollectionAdapter;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * This test shows an example how to post a workitem and how to get the worklist
 * from the RestService API.
 * <p>
 * The test class uses the jUnit concept of assumptions. Before each test, a set
 * of assumptions can be made. If one of these assumptions is not met, the test
 * should be skipped.
 * <p>
 * In this case, we make the assumption that a connection to the imixs-workflow
 * rest service can be established.
 * <p>
 * See also: https://reflectoring.io/conditional-junit4-junit5-tests/
 * 
 * @author rsoika
 */
public class WorkflowTest {

	static String BASE_URL = "http://localhost:8080/api/";
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

		try {
			deployBPMNModel("/ticket-en-1.0.0.bpmn");
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
	 * create new Ticket based on a Imixs ItemCollection object using the Imixs
	 * WorkflowTestSuite.
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
			ticket = workflowCLient.processWorkitem(ticket);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(ticket);
		String uid = ticket.getUniqueID();

		Assert.assertFalse(uid.isEmpty());
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
		String json = "{\"item\":[" + "     {\"name\":\"type\",\"value\":{\"@type\":\"xs:string\",\"$\":\"workitem\"}},"
				+ "     {\"name\":\"$modelversion\",\"value\":{\"@type\":\"xs:string\",\"$\":\"" + MODEL_VERSION
				+ "\"}}," + "     {\"name\":\"$taskid\",\"value\":{\"@type\":\"xs:int\",\"$\":\"1000\"}},"
				+ "     {\"name\":\"$eventid\",\"value\":{\"@type\":\"xs:int\",\"$\":\"10\"}},"
				+ "     {\"name\":\"namteam\",\"value\":[{\"@type\":\"xs:string\",\"$\":\"admin\"},"
				+ "	{\"@type\":\"xs:string\",\"$\":\"eddy\"},{\"@type\":\"xs:string\",\"$\":\"john\"}]},"
				+ "     {\"name\":\"txtname\",\"value\":{\"@type\":\"xs:string\",\"$\":\"test-json\"}}" + "]}";

		try {
			// post json request, accept XML
			String result = restClient.post(BASE_URL + "workflow/workitem.json", json, MediaType.APPLICATION_JSON,
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