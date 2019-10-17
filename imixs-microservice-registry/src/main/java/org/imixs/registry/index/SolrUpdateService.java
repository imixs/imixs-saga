package org.imixs.registry.index;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.IntPredicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.services.rest.BasicAuthenticator;
import org.imixs.workflow.services.rest.RestAPIException;
import org.imixs.workflow.services.rest.RestClient;

/**
 * The SolrUpdateService is used to update the index and the solr schema
 * 
 * 
 * @version 1.0
 * @author rsoika
 * 
 */
@Stateless
public class SolrUpdateService implements Serializable {
	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(SolrUpdateService.class.getName());
	public static List<String> DEFAULT_STORE_FIELD_LIST = Arrays.asList("type", "$taskid", "$writeaccess",
			"$workflowsummary", "$workflowabstract", "$workflowgroup", "$workflowstatus", "$modified", "$created",
			"$lasteventdate", "$creator", "$editor", "$lasteditor", "$owner", "namowner");

	private List<String> schemaFieldList = null;

	@Inject
	@ConfigProperty(name = "solr.configset", defaultValue = "_default")
	private String configset;

	@Inject
	@ConfigProperty(name = "solr.user", defaultValue = "")
	private String user;

	@Inject
	@ConfigProperty(name = "solr.password", defaultValue = "")
	private String password;

	@Inject
	@ConfigProperty(name = "solr.api", defaultValue = "")
	private String api;

	@Inject
	@ConfigProperty(name = "solr.core", defaultValue = "imixs-registry")
	private String core;
	
	@Inject
	@ConfigProperty(name = "imixs.registry.index.fields", defaultValue = "")
	String imixsIndexFieldList;

	private RestClient restClient;

	/**
	 * Create a solr rest client instance
	 * @throws org.imixs.workflow.services.rest.RestAPIException 
	 */
	@PostConstruct
	public void init()  {

		if (api.isEmpty()) {
			// no solr index!
			return;
		}
		// crate unique index fields
		schemaFieldList = new ArrayList<String>();
		schemaFieldList.add(WorkflowKernel.UNIQUEID);
		schemaFieldList.addAll(DEFAULT_STORE_FIELD_LIST);
		if (imixsIndexFieldList != null && !imixsIndexFieldList.isEmpty()) {
			StringTokenizer st = new StringTokenizer(imixsIndexFieldList, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase().trim();
				// do not add internal fields
				if (!schemaFieldList.contains(sName)) {
					schemaFieldList.add(sName);
				}
			}
		}
		// create rest client
		restClient = new RestClient(api);
		if (user != null && !user.isEmpty()) {
			BasicAuthenticator authenticator = new BasicAuthenticator(user, password);
			restClient.registerRequestFilter(authenticator);
		}

		// try to get the schma of the core...
		try {
			String existingSchema = restClient.get(api + "/api/cores/" + core + "/schema");
			logger.info("...core   - OK ");

			// update schema
			updateSchema(existingSchema);
		} catch ( org.imixs.workflow.services.rest.RestAPIException e) {
			// no schema found
			logger.severe("...no solr core '" + core + "' found - " + e.getMessage() + ": verify the solr instance!");
			
		}

	}

	/**
	 * This method adds a collection of documents to the Lucene Solr index. The
	 * documents are added immediately to the index. Calling this method within a
	 * running transaction leads to a uncommitted reads in the index. For
	 * transaction control, it is recommended to use instead the the method
	 * solrUpdateService.updateDocuments() which takes care of uncommitted reads.
	 * <p>
	 * This method is used by the JobHandlerRebuildIndex only.
	 * 
	 * @param documents
	 *            of ItemCollections to be indexed
	 * @throws org.imixs.workflow.services.rest.RestAPIException
	 */
	public void indexDocuments(Collection<ItemCollection> documents)
			throws RestAPIException {
		long ltime = System.currentTimeMillis();
	
		if (documents == null || documents.size() == 0) {
			// no op!
			return;
		} else {
	
			String xmlRequest = buildAddDoc(documents);
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest(xmlRequest);
			}
	
			String uri = api + "/solr/" + core + "/update?commit=true";
			restClient.post(uri, xmlRequest, "text/xml");
		}
	
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("... update index block in " + (System.currentTimeMillis() - ltime) + " ms (" + documents.size()
					+ " workitems total)");
		}
	}

	/**
	 * This method removes a collection of documents from the Lucene Solr index.
	 * 
	 * @param documents
	 *            of collection of UniqueIDs to be removed from the index
	 * @throws org.imixs.workflow.services.rest.RestAPIException
	 */
	public void removeDocuments(Set<String> documents) throws RestAPIException {
		long ltime = System.currentTimeMillis();
	
		if (documents == null || documents.size() == 0) {
			// no op!
			return;
		} else {
			StringBuffer xmlDelete = new StringBuffer();
			xmlDelete.append("<delete>");
			for (String id : documents) {
				xmlDelete.append("<id>" + id + "</id>");
			}
			xmlDelete.append("</delete>");
			String xmlRequest = xmlDelete.toString();
			String uri = api + "/solr/" + core + "/update";
			logger.finest("......delete documents '" + core + "':");
			restClient.post(uri, xmlRequest, "text/xml");
		}
	
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("... update index block in " + (System.currentTimeMillis() - ltime) + " ms (" + documents.size()
					+ " workitems total)");
		}
	}

	/**
	 * Updates the schema definition of an existing Solr core.
	 * <p>
	 * The schema definition is build by the method builUpdateSchema(). The
	 * updateSchema adds or replaces field definitions depending on the fieldList
	 * definitions provided by the Imixs SchemaService. See the method
	 * builUpdateSchema() for details.
	 * <p>
	 * The method asumes that a core already exits. Otherwise an exception is
	 * thrown.
	 * 
	 * @param schema
	 *            - existing schema defintion
	 * @return - an update Schema definition to be POST to the Solr rest api.
	 * @throws RestAPIException
	 * @throws org.imixs.workflow.services.rest.RestAPIException
	 */
	private void updateSchema(String schema) throws  RestAPIException {
	
		// create the schema....
		String schemaUpdate = buildUpdateSchema(schema);
		// test if the schemaUdpate contains instructions....
		if (!"{}".equals(schemaUpdate)) {
			String uri = api + "/api/cores/" + core + "/schema";
			logger.info("...updating schema '" + core + "':");
			logger.finest("..." + schemaUpdate);
			restClient.post(uri, schemaUpdate, "application/json");
			logger.info("...schema update - successfull ");
		} else {
			logger.info("...schema - OK ");
		}
	}

	/**
	 * This method builds a JSON structure to be used to update an existing Solr
	 * schema. The method adds or replaces field definitions into a solr update
	 * schema.
	 * <p>
	 * The schema for the imixs-registry contains only stored fields. The field list
	 * is defined by the DEFAULT_STORE_FIELD_LIST and the optional index.fields
	 * 
	 * <p>
	 * 
	 * The param oldSchema contains the current schema definition of the core.
	 * <p>
	 *
	 * <code>{"add-field":{name=field1, type=text_general, stored=true, docValues=true}}</code>
	 * <p>
	 * 
	 * 
	 * Stored fields (stored=true) are row orientated. That means that like in a sql
	 * table the values are stored based on the ID of the document. Find also
	 * details in the Imixs-Workflow-Solr Index implementation.
	 * 
	 * <p>
	 * 
	 * @see https://lucene.apache.org/solr/guide/8_0/docvalues.html
	 * @return
	 */
	private String buildUpdateSchema(String oldSchema) {

		StringBuffer updateSchema = new StringBuffer();
		List<String> fieldListStore = schemaFieldList;

		// remove white space from oldSchema to simplify string compare...
		oldSchema = oldSchema.replace(" ", "");

		logger.finest("......old schema=" + oldSchema);

		updateSchema.append("{");

		// add each field from the fieldListAnalyze
		for (String field : fieldListStore) {
			addFieldDefinitionToUpdateSchema(updateSchema, oldSchema, field, "strings", true, false);
		}

		// remove last ,
		int lastComma = updateSchema.lastIndexOf(",");
		if (lastComma > -1) {
			updateSchema.deleteCharAt(lastComma);
		}
		updateSchema.append("}");
		return updateSchema.toString();
	}

	/**
	 * This method adds a field definition object to an updateSchema.
	 * <p>
	 * In case the same field already exists in the oldSchema then the method will
	 * replace the field. In case id does not exist, the field definition is added
	 * to the update schema.
	 * <p>
	 * Example:
	 * <p>
	 * <code>add-field:{name:"$workflowsummary", type:"text_general", stored:true, docValues:false}</code><br
	 * />
	 * <code>replace-field:{name:"$workflowstatus", type:"strings", stored:true, docValues:true}</code>
	 * <p>
	 * To verify the existence of the field we parse the fieldname in the old schema
	 * definition.
	 * <p>
	 * Note: In Solr field names must not start with $ symbol. For that reason we
	 * adapt the $ with _ for all known index fields
	 *
	 * @param updateSchema
	 *            - a stringBuffer to build the update schema
	 * @param oldSchema
	 *            - the existing schema definition
	 * @param name
	 *            - field name
	 * @param type
	 *            - field type
	 * @param store
	 *            - boolean store field
	 * @param docValue
	 *            - true if docValues should be set to true
	 * 
	 */
	private void addFieldDefinitionToUpdateSchema(StringBuffer updateSchema, String oldSchema, String _name,
			String type, boolean store, boolean docvalue) {

		// replace $ with _
		String name = adaptImixsItemName(_name);

		String fieldDefinition = "{\"name\":\"" + name + "\",\"type\":\"" + type + "\",\"stored\":" + store
				+ ",\"docValues\":" + docvalue + "}";

		// test if this field already exists in the old schema. If not we add the new
		// field to the schema with the 'add-field' command.
		// If it already exits, than we need to replace the definition with
		// 'replace-field'.
		String testSchemaField = "{\"name\":\"" + name + "\",";
		if (oldSchema == null || !oldSchema.contains(testSchemaField)) {
			// add new field to updateSchema....
			updateSchema.append("\"add-field\":" + fieldDefinition + ",");
		} else {
			// the field exists in the schema - so replace it if the definition has changed
			if (!oldSchema.contains(fieldDefinition)) {
				updateSchema.append("\"replace-field\":" + fieldDefinition + ",");
			}
		}
	}

	/**
	 * This method adds a field value into a xml update request.
	 * <p>
	 * In case the value is a date or calendar object, then the value will be
	 * converted into a lucene time format.
	 * <p>
	 * The value will always be wrapped with a CDATA tag to avoid invalid XML.
	 * 
	 * @param doc
	 *            an existing lucene document
	 * @param workitem
	 *            the workitem containing the values
	 * @param _itemName
	 *            the item name inside the workitem
	 */
	private void addFieldValuesToUpdateRequest(StringBuffer xmlContent, final ItemCollection workitem,
			final String _itemName) {

		SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmmss");

		if (_itemName == null) {
			return;
		}

		List<?> vValues = workitem.getItemValue(_itemName);
		if (vValues.size() == 0) {
			return;
		}
		if (vValues.get(0) == null) {
			return;
		}

		String itemName = _itemName.toLowerCase().trim();
		for (Object singleValue : vValues) {
			String convertedValue = "";
			if (singleValue instanceof Calendar || singleValue instanceof Date) {
				// convert calendar to lucene string representation
				String sDateValue;
				if (singleValue instanceof Calendar) {
					sDateValue = dateformat.format(((Calendar) singleValue).getTime());
				} else {
					sDateValue = dateformat.format((Date) singleValue);
				}
				convertedValue = sDateValue;
			} else {
				// default
				convertedValue = singleValue.toString();
			}

			// remove existing CDATA...
			convertedValue = stripCDATA(convertedValue);
			// strip control codes..
			convertedValue = stripControlCodes(convertedValue);
			// wrapp value into CDATA
			convertedValue = "<![CDATA[" + stripControlCodes(convertedValue) + "]]>";

			xmlContent.append("<field name=\"" + adaptImixsItemName(itemName) + "\">" + convertedValue + "</field>");
		}

	}

	/**
	 * This method returns a XML structure to add new documents into the solr index.
	 * 
	 * @return xml content to update documents
	 */
	protected String buildAddDoc(Collection<ItemCollection> documents) {

		StringBuffer xmlContent = new StringBuffer();

		xmlContent.append("<add overwrite=\"true\">");

		for (ItemCollection document : documents) {

			// if no UniqueID is defined we need to skip this document
			if (document.getUniqueID().isEmpty()) {
				continue;
			}

			xmlContent.append("<doc>");

			// now add all fields...
			for (String aFieldname : schemaFieldList) {
				addFieldValuesToUpdateRequest(xmlContent, document, aFieldname);
			}

			xmlContent.append("</doc>");
		}

		xmlContent.append("</add>");

		return xmlContent.toString();
	}

	/**
	 * This helper method is to strip control codes and extended characters from a
	 * string. We can not put those chars into the XML request send to solr.
	 * <p>
	 * Background:
	 * <p>
	 * In ASCII, the control codes have decimal codes 0 through to 31 and 127. On an
	 * ASCII based system, if the control codes are stripped, the resultant string
	 * would have all of its characters within the range of 32 to 126 decimal on the
	 * ASCII table.
	 * <p>
	 * On a non-ASCII based system, we consider characters that do not have a
	 * corresponding glyph on the ASCII table (within the ASCII range of 32 to 126
	 * decimal) to be an extended character for the purpose of this task.
	 * </p>
	 * 
	 * @see https://rosettacode.org/wiki/Strip_control_codes_and_extended_characters_from_a_string
	 * 
	 * @param s
	 * @param include
	 * @return
	 */
	protected String stripControlCodes(String s) {

		// control codes stripped (but extended characters not stripped)
		// IntPredicate include=c -> c > '\u001F' && c != '\u007F';

		// control codes and extended characters stripped
		IntPredicate include = c -> c > '\u001F' && c < '\u007F';
		return s.codePoints().filter(include::test)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
	}

	/**
	 * This helper method strips CDATA blocks from a string. We can not post
	 * embedded CDATA in an alredy existing CDATA when we post the xml to solr.
	 * <p>
	 * 
	 * @param s
	 * @return
	 */
	protected String stripCDATA(String s) {

		if (s.contains("<![CDATA[")) {
			String result = s.replaceAll("<!\\[CDATA\\[", "");
			result = result.replaceAll("]]>", "");
			return result;
		} else {
			return s;
		}
	}

	/**
	 * This method adapts an Imixs item name to the corresponding Solr field name.
	 * Because Solr does not accept $ char at the beginning of an field we need to
	 * replace starting $ with _ if the item is part of the Imixs Index Schema.
	 * 
	 * @param itemName
	 * @return adapted Solr field name
	 */
	private String adaptImixsItemName(String itemName) {
		if (itemName == null || itemName.isEmpty()) {
			return itemName;
		}
		if (itemName.charAt(0) == '$') {
			if (schemaFieldList.contains(itemName)) {
				String adaptedName = "_" + itemName.substring(1);
				return adaptedName;
			}
		}
		return itemName;
	}

}