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
import java.util.logging.Level;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.services.rest.RestClient;
import org.imixs.workflow.test.WorkflowTestSuite;
import org.imixs.workflow.util.Base64;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import junit.framework.Assert;

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
	static String MODEL_VERSION="1.0.1";
	RestClient restClient = null;
	WorkflowTestSuite testSuite = null;

	/**
	 * The setup method deploys the ticket workflow into the running workflow
	 * instance.
	 * 
	 * @throws Exception 
	 */
	@Before
	public void setup() throws Exception {

		try {
			deployBPMNModel("/ticket.bpmn");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}

	/**
	 * create new Ticket based on a Imixs ItemCollection object using the Imixs
	 * WorkflowTestSuite.
	 * 
	 */
	@Ignore
	@Test
	public void createNewWorkitemTest() {

		testSuite = WorkflowTestSuite.getInstance();
		testSuite.setHost(BASE_URL);
		testSuite.joinParty(USERID, PASSWORD);
		testSuite.joinParty("Anonymous", null);

		ItemCollection ticket = new ItemCollection();
		ticket.replaceItemValue("type", "workitem");
		ticket.replaceItemValue("$ModelVersion",MODEL_VERSION);
		ticket.replaceItemValue("$processid", 1000);
		ticket.replaceItemValue("$activityid", 10);
		ticket.replaceItemValue("txtName", "Test");
		ticket.replaceItemValue("namTeam",Arrays.asList(new String[] { "admin", "alex", "marty" }));

		ticket = testSuite.processWorkitem(ticket, "admin");

		// TODO: bug need to be fixed- see issue #259
		Assert.assertNotNull(ticket);
		String uid = ticket.getItemValueString("$UniqueID");
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
		String json = "{\"item\":[" + "     {\"name\":\"type\",\"value\":{\"@type\":\"xs:string\",\"$\":\"workitem\"}},"
				+ "     {\"name\":\"$modelversion\",\"value\":{\"@type\":\"xs:string\",\"$\":\"" + MODEL_VERSION + "\"}},"
				+ "     {\"name\":\"$processid\",\"value\":{\"@type\":\"xs:int\",\"$\":\"1000\"}},"
				+ "     {\"name\":\"$activityid\",\"value\":{\"@type\":\"xs:int\",\"$\":\"10\"}},"
			    + "     {\"name\":\"namteam\",\"value\":[{\"@type\":\"xs:string\",\"$\":\"admin\"},"
			    + "	{\"@type\":\"xs:string\",\"$\":\"eddy\"},{\"@type\":\"xs:string\",\"$\":\"john\"}]},"
				+ "     {\"name\":\"txtname\",\"value\":{\"@type\":\"xs:string\",\"$\":\"test-json\"}}" + "]}";

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

	/**
	 * This method deploys a BPMN model into the running workflow instance via
	 * the Imixs-Rest API
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
	 * This method decodes the user id and password used for basic
	 * authentication
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