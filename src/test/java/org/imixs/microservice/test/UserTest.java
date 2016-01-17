package org.imixs.microservice.test;

import org.imixs.workflow.services.rest.RestClient;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * This test class contains an example how to create a new User Object trough
 * the RestService API.
 * 
 * @author rsoika
 *
 */
public class UserTest {

	static String USERID = "admin";
	static String PASSWORD = "adminadmin";
	static String BASE_URL = "http://localhost:8080/imixs-microservice/";

	RestClient restClient = null;

	@Before
	public void setup() {
		restClient = new RestClient();
		restClient.setCredentials(USERID, PASSWORD);
	}

	/**
	 * This test create a new user based on a json object. The user object have
	 * to had at least the following properties:
	 * 
	 * type="profile"
	 * 
	 * txtname= [userID]
	 * 
	 * txtpassword= [aPassword]
	 * 
	 * txtgroups= "IMIXS-WORKFLOW-Author"
	 * 
	 * 
	 * The user is !not! stored in the entity list but in the userId list!
	 * @throws Exception
	 */
	@Test
	public void addUser() {

		String uri = BASE_URL + "user";
		String user = "eddy";
		String password = "imixs";
 
		// create a json test string
		String json = "{\"item\":[" + "     {\"name\":\"type\",\"value\":{\"@type\":\"xs:string\",\"$\":\"profile\"}},"
				+ "     {\"name\":\"txtname\",\"value\":{\"@type\":\"xs:string\",\"$\":\"" + user + "\"}},"
				+ "     {\"name\":\"txtpassword\",\"value\":{\"@type\":\"xs:string\",\"$\":\"" + password + "\"}},"
				+ "     {\"name\":\"txtgroups\",\"value\":{\"@type\":\"xs:string\",\"$\":\"IMIXS-WORKFLOW-Author\"}}"
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