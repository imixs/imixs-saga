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
import org.imixs.workflow.services.rest.BasicAuthenticator;
import org.imixs.workflow.services.rest.RestClient;

import org.imixs.workflow.util.Base64;
import org.imixs.workflow.xml.XMLDocumentAdapter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import junit.framework.Assert;

/**
 * This test simmulates batch processing with high load
 * 
 * @author rsoika
 *
 */
public class WorkflowLoadRunner {

	static String BASE_URL = "http://localhost:8080/api/";
	static String USERID = "admin";
	static String PASSWORD = "adminadmin";
	static String MODEL_VERSION = "1.0.1";
	RestClient restClient = null;
	
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
	 * Run 100 creatins...
	 */
	@Ignore
	@Test
	public void create100Test() {

		int MAX_COUNT = 100;
		int count = 0;
		long l=System.currentTimeMillis();
		System.out.println("Start creat100Test...");
		while (count < MAX_COUNT) {
			createNewWorkitemTest("Some data...." + count);
			count++;
		}
		long time=System.currentTimeMillis()-l;
		System.out.println("Completed creat100Test in " + (time) + "ms");
		
		System.out.println("  - processing time ~" + (time/100) + "ms");
		

	}

	/**
	 * create new Ticket based on a Imixs ItemCollection object using the Imixs
	 * RestClient.
	 * 
	 */
	void createNewWorkitemTest(String subject) {

		RestClient restClient = new RestClient(BASE_URL);
		// create a default basic authenticator
		BasicAuthenticator basicAuth = new BasicAuthenticator(USERID, PASSWORD);
		// register the authenticator
		restClient.registerRequestFilter(basicAuth);

		ItemCollection ticket = new ItemCollection();
		ticket.replaceItemValue("type", "workitem");
		ticket.replaceItemValue("$ModelVersion", MODEL_VERSION);
		ticket.replaceItemValue("$taskid", 1000);
		ticket.replaceItemValue("$eventid", 10);
		ticket.replaceItemValue("txtName", subject);
		ticket.replaceItemValue("namTeam", Arrays.asList(new String[] { "admin", "alex", "marty" }));
		try {
			ticket = restClient.postXMLDocument(BASE_URL + "workflow/workitem", XMLDocumentAdapter.getDocument(ticket));

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(ticket);
		
	}

	/**
	 * create new Ticket based on a JSON String
	 * 
	 * 
	 */
	void createNewWorkitemJSONTest(String subject) {
		ItemCollection ticket = null;
		RestClient restClient = new RestClient();
		// create a default basic authenticator
		BasicAuthenticator basicAuth = new BasicAuthenticator(USERID, PASSWORD);
		// register the authenticator
		restClient.registerRequestFilter(basicAuth);

		// create a json test string
		String json = "{\"item\":[" + "     {\"name\":\"type\",\"value\":{\"@type\":\"xs:string\",\"$\":\"workitem\"}},"
				+ "     {\"name\":\"$modelversion\",\"value\":{\"@type\":\"xs:string\",\"$\":\"" + MODEL_VERSION
				+ "\"}}," + "     {\"name\":\"$taskid\",\"value\":{\"@type\":\"xs:int\",\"$\":\"1000\"}},"
				+ "     {\"name\":\"$eventid\",\"value\":{\"@type\":\"xs:int\",\"$\":\"10\"}},"
				+ "     {\"name\":\"namteam\",\"value\":[{\"@type\":\"xs:string\",\"$\":\"admin\"},"
				+ "	{\"@type\":\"xs:string\",\"$\":\"eddy\"},{\"@type\":\"xs:string\",\"$\":\"john\"}]},"
				+ "     {\"name\":\"txtname\",\"value\":{\"@type\":\"xs:string\",\"$\":\"" +
				 subject + "\"}}" + "]}";

		try {
			ticket = restClient.postJSON(BASE_URL + "workflow/workitem", json);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		Assert.assertNotNull(ticket);
		
	

	}

	/**
	 * This helper method deploys a BPMN model into the running workflow instance
	 * via the Imixs-Rest API
	 * 
	 * @throws Exception
	 */
	void deployBPMNModel(String modelFilePath) throws Exception {
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
	String getAccessByUser() {
		String sURLAccess = "";
		String sUserCode = USERID + ":" + PASSWORD;
		char[] authcode = Base64.encode(sUserCode.getBytes());
		sURLAccess = String.valueOf(authcode);
		return sURLAccess;
	}

}