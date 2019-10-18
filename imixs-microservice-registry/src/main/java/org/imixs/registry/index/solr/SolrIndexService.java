package org.imixs.registry.index.solr;

import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.registry.index.DefaultOperator;
import org.imixs.registry.index.SortOrder;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.QueryException;
import org.imixs.workflow.services.rest.BasicAuthenticator;
import org.imixs.workflow.services.rest.RestAPIException;
import org.imixs.workflow.services.rest.RestClient;

/**
 * The SolrIndexService is used to query the index
 * 
 * 
 * @version 1.0
 * @author rsoika
 * 
 */
@Stateless
public class SolrIndexService implements Serializable {
	private static final long serialVersionUID = 1L;
	private SimpleDateFormat luceneDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

	private static Logger logger = Logger.getLogger(SolrIndexService.class.getName());

	// number of hits
	public static final int DEFAULT_PAGE_SIZE = 100; // default docs in one page

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
	private SolrUpdateService solrUpdateService;

	private RestClient restClient = null;

	/**
	 * Create a solr rest client instance
	 * 
	 * @throws org.imixs.workflow.services.rest.RestAPIException
	 */
	@PostConstruct
	public void init() {

		if (api.isEmpty()) {
			// no solr index!
			return;
		}

		// create rest client
		restClient = new RestClient(api);
		if (user != null && !user.isEmpty()) {
			BasicAuthenticator authenticator = new BasicAuthenticator(user, password);
			restClient.registerRequestFilter(authenticator);
		}

	}

	/**
	 * This method post a search query and returns the result list.
	 * <p>
	 * The method will return the documents containing all stored or DocValues
	 * fields.
	 * 
	 * 
	 * @param searchterm
	 * @return List of ItemCollection
	 * @throws QueryException
	 */
	public List<ItemCollection> query(String searchTerm, int pageSize, int pageIndex, SortOrder sortOrder,
			DefaultOperator defaultOperator) throws QueryException {

		logger.fine("...search solr index: " + searchTerm + "...");

		StringBuffer uri = new StringBuffer();

		// URL Encode the query string....
		try {
			uri.append(api + "/solr/" + core + "/query");

			// set default operator?
			if (defaultOperator == DefaultOperator.OR) {
				uri.append("?q.op=" + defaultOperator);
			} else {
				// if not define we default in any case to AND
				uri.append("?q.op=AND");
			}

			// set sort order....
			if (sortOrder != null) {
				// sorted by sortoder
				String sortField = sortOrder.getField();
				if (sortField != null && !sortField.isEmpty()) {
					// for Solr we need to replace the leading $ with _
					if (sortField.startsWith("$")) {
						sortField = "_" + sortField.substring(1);
					}
					if (sortOrder.isReverse()) {
						uri.append("&sort=" + sortField + "%20desc");
					} else {
						uri.append("&sort=" + sortField + "%20asc");
					}
				}
			}

			// page size of 0 is allowed here - this will be used by the getTotalHits method
			// of the SolrSearchService
			if (pageSize < 0) {
				pageSize = DEFAULT_PAGE_SIZE;
			}

			if (pageIndex < 0) {
				pageIndex = 0;
			}

			uri.append("&rows=" + (pageSize));
			if (pageIndex > 0) {
				uri.append("&start=" + (pageIndex * pageSize));
			}

			// append query
			uri.append("&q=" + URLEncoder.encode(searchTerm, "UTF-8"));

			logger.finest("...... uri=" + uri.toString());
			String result = restClient.get(uri.toString());

			return parseQueryResult(result);

		} catch (RestAPIException | UnsupportedEncodingException e) {

			logger.severe("Solr search error: " + e.getMessage());
			throw new QueryException(QueryException.QUERY_NOT_UNDERSTANDABLE, e.getMessage(), e);
		}

	}

	/**
	 * This method extracts the docs from a Solr JSON query result
	 * 
	 * @param json
	 *            - solr query response (JSON)
	 * @return List of ItemCollection objects
	 */
	private List<ItemCollection> parseQueryResult(String json) {
		long l = System.currentTimeMillis();
		List<ItemCollection> result = new ArrayList<ItemCollection>();
		JsonParser parser = Json.createParser(new StringReader(json));
		Event event = null;
		while (true) {

			try {
				event = parser.next(); // START_OBJECT
				if (event == null) {
					break;
				}

				if (event.name().equals(Event.KEY_NAME.toString())) {
					String jsonkey = parser.getString();
					if ("docs".equals(jsonkey)) {
						event = parser.next(); // docs array
						if (event.name().equals(Event.START_ARRAY.toString())) {
							event = parser.next();
							while (event.name().equals(Event.START_OBJECT.toString())) {
								// a single doc..

								logger.finest("......parse doc....");
								ItemCollection itemCol = parseDoc(parser);
								// now take the values
								result.add(itemCol);
								event = parser.next();
							}

							if (event.name().equals(Event.END_ARRAY.toString())) {
								break;

							}

						}

					}

				}
			} catch (NoSuchElementException e) {
				break;
			}
		}

		logger.finest("......total parsing time " + (System.currentTimeMillis() - l) + "ms");
		return result;
	}

	/**
	 * Builds a ItemCollection from a json doc strcuture
	 * 
	 * @param parser
	 * @return
	 */
	private ItemCollection parseDoc(JsonParser parser) {
		ItemCollection document = new ItemCollection();
		Event event = null;
		event = parser.next(); // a single doc..
		while (event.name().equals(Event.KEY_NAME.toString())) {
			String itemName = parser.getString();
			logger.finest("......found item " + itemName);
			List<?> itemValue = parseItem(parser);
			// convert itemName and value....
			itemName = adaptSolrFieldName(itemName);
			document.replaceItemValue(itemName, itemValue);
			event = parser.next();
		}

		return document;
	}

	/**
	 * This method adapts an Solr field name to the corresponding Imixs Item name.
	 * Because Solr does not accept $ char at the beginning of an field we need to
	 * replace starting _ with $ if the item is part of the Imixs Index Schema.
	 * 
	 * @param itemName
	 * @return adapted Imixs item name
	 */
	private String adaptSolrFieldName(String itemName) {
		if (itemName == null || itemName.isEmpty()) {
			return itemName;
		}
		if (itemName.charAt(0) == '_') {
			String adaptedName = "$" + itemName.substring(1);
			if (solrUpdateService.getSchemaFieldList().contains(adaptedName)) {
				return adaptedName;
			}
		}
		return itemName;
	}

	/**
	 * parses a single item value
	 * 
	 * @param parser
	 * @return
	 */
	private List<Object> parseItem(JsonParser parser) {

		List<Object> result = new ArrayList<Object>();
		Event event = null;
		while (true) {
			event = parser.next(); // a single doc..
			if (event.name().equals(Event.START_ARRAY.toString())) {

				while (true) {
					event = parser.next(); // a single doc..
					if (event.name().equals(Event.VALUE_STRING.toString())) {
						// just return the next json object here

						result.add(convertLuceneValue(parser.getString()));
					}
					if (event.name().equals(Event.VALUE_NUMBER.toString())) {
						// just return the next json object here
						// result.add(parser.getBigDecimal());

						result.add(convertLuceneValue(parser.getString()));
					}
					if (event.name().equals(Event.VALUE_TRUE.toString())) {
						// just return the next json object here
						result.add(true);
					}
					if (event.name().equals(Event.VALUE_FALSE.toString())) {
						// just return the next json object here
						result.add(false);
					}
					if (event.name().equals(Event.END_ARRAY.toString())) {
						break;
					}
				}

			}

			if (event.name().equals(Event.VALUE_STRING.toString())) {
				// single value!
				result.add(parser.getString());
			}
			if (event.name().equals(Event.VALUE_NUMBER.toString())) {
				// just return the next json object here
				result.add(parser.getBigDecimal());
			}
			if (event.name().equals(Event.VALUE_TRUE.toString())) {
				// just return the next json object here
				result.add(true);
			}
			if (event.name().equals(Event.VALUE_FALSE.toString())) {
				// just return the next json object here
				result.add(false);
			}

			break;
		}

		return result;
	}

	/**
	 * This
	 * 
	 * @param stringValue
	 * @return
	 */
	private Object convertLuceneValue(String stringValue) {
		Object objectValue = null;
		// check for numbers....
		if (isNumeric(stringValue)) {
			// is date?
			if (stringValue.length() == 14 && !stringValue.contains(".")) {
				try {
					objectValue = luceneDateFormat.parse(stringValue);
				} catch (java.text.ParseException e) {
					// no date!
				}
			}
			// lets see if it is a number..?
			if (objectValue == null) {
				try {
					Number number = NumberFormat.getInstance().parse(stringValue);
					objectValue = number;
				} catch (java.text.ParseException e) {
					// no number - should not happen
				}
			}
		}
		if (objectValue == null) {
			objectValue = stringValue;
		}
		return objectValue;
	}

	/**
	 * Helper method to check for numbers.
	 * 
	 * @see https://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java
	 * @param str
	 * @return
	 */
	private static boolean isNumeric(String str) {
		boolean dot = false;
		if (str == null || str.isEmpty()) {
			return false;
		}
		for (char c : str.toCharArray()) {
			if (c == '.' && dot == false) {
				dot = true; // first dot!
				continue;
			}
			if (c < '0' || c > '9') {
				return false;
			}
		}
		return true;

	}

}