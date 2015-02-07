package org.imixs.microservice.test;

import java.util.logging.Level;

import junit.framework.Assert;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.test.WorkflowTestSuite;
import org.junit.Before;
import org.junit.Test;

public class SimpleTest {
	WorkflowTestSuite testSuite = null;

	@Before
	public void setup() {
		testSuite = WorkflowTestSuite.getInstance();
		testSuite
				.setHost("http://localhost:8080/imixs-microservice/rest-service/");
		testSuite.joinParty("admin", "adminadmin");
		testSuite.joinParty("Anonymous", null);
	}

	/**
	 * create Ticket
	 * 
	 * @throws Exception
	 */
	@Test
	public void createNewWorkitemTest() throws Exception {
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
}