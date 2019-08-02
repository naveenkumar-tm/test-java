/**
 * This package contain the Service class of  GlobeTouch Application
 */
package org.orchestration.services;

import java.lang.reflect.Type;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.orchestration.constant.HttpStatusParameterMapping;
import org.orchestration.constant.OrchestrationProcessParameter;
import org.orchestration.genericService.OrchestrationGenericService;
import org.orchestration.http.client.OrchestrationHttpURLCalling;
import org.orchestration.http.client.OrchestrationSoapApiCalling;
import org.orchestration.resources.JsonOlcoreModification;
import org.orchestration.response.model.OrchestrationMessage;

/**
 * 
 * This class work as a Service class for Generic Method for API Calling .
 * 
 * @author Ankita Shrothi
 *
 */
@Service
@PropertySource(value = "classpath:/OrchestrationApiConfiguration.properties")
@SuppressWarnings({ "unchecked", "rawtypes" })

public class GenericMethodService {
	/*
	 * Autowired is used to inject the object dependency implicitly.It's a specific
	 * functionality of spring which requires less code.
	 */
	static Logger logger = Logger.getLogger(GenericMethodService.class);
	/**
	 * To Access the respective class Methods as their services
	 */
	@Autowired
	Environment environment;
	/**
	 * To Access the respective class Methods as their services
	 */
	@Autowired
	private OrchestrationGenericProcess orchestrationGenericProcess;
	/**
	 * To Access the respective class Methods as their services
	 */
	@Autowired
	private OrchestrationHttpURLCalling urlCalling;
	/**
	 * To Access the respective class Methods as their services
	 */
	@Autowired
	private OrchestrationSoapApiCalling urlSoapCalling;
	/**
	 * To Access the respective class Methods as their services
	 */
	@Autowired
	private OrchestrationGenericService genericService;
	/**
	 * To Access the respective class Methods as their services
	 */
	@Autowired
	private OrchestrationProcessParameter processParameter;

	/**
	 * To Access the respective class Methods as their services
	 */
	@Autowired
	private HttpStatusParameterMapping httpStatusParameter;

	/**
	 * To Validate the Authentication Parameter of each API defined in Group.The
	 * Purpose of this method is:
	 * 
	 * 1.To Check If Authentication Parameter required.
	 * 
	 * 2.If Token is required than get Authentication Parameter from database with
	 * its active bit.
	 * 
	 * 3.If Authentication Parameter is active than add it into requestParameterMap.
	 * 
	 * 4.If Authentication Parameter is not active than it will call authentication
	 * API of End Node and get the Authentication Parameter and insert it into
	 * database as well as add it into requestParameterMap.
	 * 
	 * 5.If First Time API is calling and Database does'nt have Authentication
	 * Parameter in that case also we have to follow point 4.
	 * 
	 * @param urlResponse         -Set of APIs exist in their respective group
	 * @param requestParameterMap -Set of input Parameters
	 * @param request             -HttpServletRequest request to get requested
	 *                            machines urlPassingParameter like host ,url
	 *                            ,headers
	 * @param response
	 * @return urlResponse with Authentication Parameter
	 * @throws Exception
	 */
	public static final String IS_TOKEN_REQUIRED = "api_is_token_required";
	public static final String API_GROUP_ID = "api_group_id";
	public static final String API_ID = "api_id";
	public static final String TOKEN_RESPONSE = "token_response";
	public static final String NODE_ADDRESS_ID = "node_address_id";
	public static final String API_TOKEN_API = "api_token_api";
	public static final String TRACKING_MESSAGE_HEADER = "tracking_message_header";
	public static final String TOKEN = "token";
	public static final String TEMPLATE_PARAM_PATTERN = "<.+?>";
	public static final String REPLACE_PARAM_PATTERN = "[<,>]";
	public static final String LOG_SUCCESS_BIT = "success.bit";
	public static final String LOG_FAILURE_BIT = "failure.bit";
	public static final String REQUEST_ID = "requestId";
	public static final String ROUTER_CONFIG_NAME = "router.config.name";
	public static final String HEADER_PARAM = "header_parameter";
	public static final String API_TEMPLATE = "api_template";
	public static final String API_HOST_ADDRESS = "api_host_address";
	public static final String API_URL = "api_url";
	public static final String API_TYPE = "api_api_type";
	public static final String BODY_PARAM = "body_parameter";
	public static final String PROCESS_FAIL = "Process Fail ";
	public static final String PRIORITY = "priority";
	public static final String LOG_FAILURE_RESPONSE_BIT = "log.status.response.failure";
	public static final String DESCRIPTION = "description";
	public static final String ERROR = "errors";
	public static final String DATABASE_DELIMETER = "\\\\'";
	public static final String RESPONSE_CODE = "response_code";
	public static final String TEMPLATE = "template";
	public static final String ERROR_STRING = "Error ";
	public static final String LOG_KAFKA_FAILURE_STATUS = "log.status.kafka.execution.failure";
	public static final String NOTIFICATION_TEMPLATE = "notifiation_template_template";
	public static final String API_CONTENT_TYPE = "api_body_parameter_type";
	public static final String END_NODE_ERROR_KEY = "error_code_key";
	public static final String SUCCESS_RESPONSE_STRING = "responseCode:200";
	public static final String ERROR_RESPONSE_STRING = "responseCode:500";
	public static final String MANDATORY_VALIDATION_PARAM = "mandatoryParameterMap";
	public static LinkedHashMap<String, String> getResponseKeyMap = new LinkedHashMap<>();
	static String groupLogID = "";
	public static String groupName = "";

	public List<Map<String, Object>> tokenValid(List<Map<String, Object>> urlResponse,
			Map<String, String> requestParameterMap, HttpServletRequest request, HttpServletResponse response)
			throws ConnectException, Exception {
		/**
		 * Initializing response
		 */

		List<Map<String, Object>> urlWithTokenResponse = new LinkedList<>();

		for (Map<String, Object> apiMap : urlResponse) {
			try {
				/**
				 * To Check if API need Authentication Parameter
				 */

				if (String.valueOf(apiMap.get(IS_TOKEN_REQUIRED)).trim().equalsIgnoreCase("true")) {
					/**
					 * If API need Authentication Parameter than setting urlPassingParameter in
					 * passingMap to call Procedure to get Authentication Parameter from database
					 */
					Map<String, String> passingMap = new LinkedHashMap<>();
					passingMap.put(API_GROUP_ID, String.valueOf(apiMap.get(API_GROUP_ID)).trim());
					passingMap.put(API_ID, String.valueOf(apiMap.get(API_ID)).trim());
					passingMap.put(TOKEN_RESPONSE, String.valueOf(apiMap.get(TOKEN_RESPONSE)).trim());
					passingMap.put(NODE_ADDRESS_ID, String.valueOf(apiMap.get(NODE_ADDRESS_ID)).trim());
					/**
					 * Calling Procedure to get Authentication Parameter
					 */
					OrchestrationMessage tokenMessage = orchestrationGenericProcess
							.GenericOrchestrationProcedureCalling("2", passingMap, null, request, response);
					/**
					 * If response from GenericOrchestrationProcedureCalling is valid
					 */
					if (tokenMessage.isValid()) {
						/**
						 * usse Casting response in List<Map<String, Object>> format
						 */
						List<Map<String, Object>> tokenResponse = (List<Map<String, Object>>) tokenMessage.getObject();
						/**
						 * Checking if list is not empty then, Check the parameterVAlidationMap of token
						 * it is active or not If it is 1 than it is active.
						 */
						if (!tokenResponse.isEmpty() && String.valueOf(tokenResponse.get(0).get("token_status")).trim()
								.equalsIgnoreCase("2")) {
							String[] string = String.valueOf(apiMap.get(TOKEN_RESPONSE)).split(",");
							for (String token : string) {
								logger.info("Token Response Key " + token);
								apiMap.put(token, tokenResponse.get(0).get(TOKEN));
								requestParameterMap.put(token, String.valueOf(tokenResponse.get(0).get(TOKEN)).trim());
							}
						}
						if (tokenResponse.isEmpty() || !String.valueOf(tokenResponse.get(0).get("token_status")).trim()
								.equalsIgnoreCase("2")) {
							/**
							 * For Making end-node api url
							 */
							String[] inputParameterName = String.valueOf(apiMap.get("authorization_parameters_name"))
									.trim().split(",");
							String[] parameterDataGMParameterValue = String
									.valueOf(apiMap.get("authorization_parameters_value")).trim().split(",");
							String[] isHeaderParameterValue = String
									.valueOf(apiMap.get("authorization_parameters_is_header")).split(",");

							String[] isQueryParameterValue = String
									.valueOf(apiMap.get("authorization_parameters_is_query")).trim().split(",");
							String[] isPathParameterValue = String
									.valueOf(apiMap.get("authorization_parameters_is_path")).trim().split(",");
							String[] isBodyParameterValue = String
									.valueOf(apiMap.get("authorization_parameters_is_body")).trim().split(",");
							Map<String, String> tokenInputParam = new LinkedHashMap<>();
							StringBuffer sb = new StringBuffer();
							String urlPassingParameter = "";
							StringBuilder builder = new StringBuilder();
							Map<String, String> headerParameterMap = new LinkedHashMap<>();

							for (int i = 0; i < inputParameterName.length; i++) {
								tokenInputParam.put(inputParameterName[i].trim().trim(),
										parameterDataGMParameterValue[i].trim().trim());

								if (isQueryParameterValue[i].trim().equalsIgnoreCase("1")) {

									builder.append(String.valueOf(inputParameterName[i]).trim() + "="
											+ String.valueOf(parameterDataGMParameterValue[i]).trim() + "&");

								}
								if (isHeaderParameterValue[i].trim().equalsIgnoreCase("1")) {
									headerParameterMap.put(String.valueOf(inputParameterName[i]).trim(),
											String.valueOf(parameterDataGMParameterValue[i]).trim());
								}
							}

							if (builder.length() > 0) {
								String newUrl = String.valueOf(apiMap.get(API_TOKEN_API)).trim();
								apiMap.remove(API_TOKEN_API);
								builder.deleteCharAt(builder.lastIndexOf("&"));
								apiMap.put(API_TOKEN_API, newUrl + "?" + builder.toString().trim());

							}
							if (Arrays.toString(isPathParameterValue).contains("1")
									|| Arrays.toString(isBodyParameterValue).contains("1")) {

								Pattern pattern = Pattern.compile(TEMPLATE_PARAM_PATTERN);
								StringBuffer url = new StringBuffer();

								// New Condition added of checking API_TEMPLATE

								if (!String.valueOf(apiMap.get("authorization_parameters_value"))
										.contains("application/x-www-form-urlencoded")) {

									Matcher matcher = pattern
											.matcher(String.valueOf(apiMap.get("token_api_template")).trim());
									Matcher matcherUrl = pattern
											.matcher(String.valueOf(apiMap.get(API_TOKEN_API)).trim());

									if (matcherUrl.find()) {

										String matchCaseUrl = matcherUrl.group(0);
										String matchCaseValue = "";

										if (tokenInputParam
												.containsKey(matchCaseUrl.replaceAll(REPLACE_PARAM_PATTERN, ""))) {
											matchCaseValue = String
													.valueOf(tokenInputParam
															.get(matchCaseUrl.replaceAll(REPLACE_PARAM_PATTERN, "")))
													.trim();

										} else {
											matchCaseValue = "";

										}

										matcherUrl.appendReplacement(url, matchCaseValue.trim());
										apiMap.put(API_TOKEN_API, String.valueOf(url).trim());
									}

									if (sb.length() == 0) {
										while (matcher.find()) {
											String matchCase = matcher.group(0);

											if (tokenInputParam
													.containsKey(matchCase.replaceAll(REPLACE_PARAM_PATTERN, ""))) {
												String matchCaseValue = String
														.valueOf(tokenInputParam
																.get(matchCase.replaceAll(REPLACE_PARAM_PATTERN, "")))
														.trim();
												matcher.appendReplacement(sb, matchCaseValue);
											} else {
												String matchCaseValue = "";
												matcher.appendReplacement(sb, matchCaseValue.trim());
											}

										}
										matcher.appendTail(sb);
									}

									urlPassingParameter = String.valueOf(sb).trim();

								}

								// If Api_template is Empty in that case we are post request with different
								// Content-Type eg. application/x-www-form-urlencoded
								else {
									StringBuilder stringBuilder = new StringBuilder();

									// urlPassingParameter="username=apitest&password=apiTest@32!&grant_type=password";
									for (int i = 0; i < inputParameterName.length; i++) {
										if (isBodyParameterValue[i].trim().equalsIgnoreCase("1")) {
											headerParameterMap.put(String.valueOf(inputParameterName[i]).trim(),
													String.valueOf(parameterDataGMParameterValue[i]).trim());
											stringBuilder.append(String.valueOf(inputParameterName[i]).trim() + "="
													+ String.valueOf(parameterDataGMParameterValue[i]).trim() + "&");

										}

									}

									int lastIndex = stringBuilder.lastIndexOf("&");
									urlPassingParameter = stringBuilder.substring(0, lastIndex);

								}

							}

							passingMap.put(API_GROUP_ID, String.valueOf(apiMap.get(API_GROUP_ID)).trim());
							passingMap.put(API_ID, String.valueOf(apiMap.get(API_ID)).trim());
							String hostUrl = String.valueOf(apiMap.get(API_HOST_ADDRESS)).trim()
									+ String.valueOf(apiMap.get(API_TOKEN_API)).trim();

							/**
							 * urlPassingParameter->parameter to call end-node API
							 */

							// urlPassingParameter = String.valueOf(sb).trim();

							/**
							 * Method To get Authorization Parameter
							 */

							passingMap.putAll(requestParameterMap);
							passingMap.put(API_GROUP_ID, String.valueOf(apiMap.get(API_GROUP_ID)).trim());
							passingMap.put(API_ID, String.valueOf(apiMap.get(API_ID)).trim());

							Map<String, String> authToken = null;
							try {
								authToken = getToken(hostUrl, urlPassingParameter, request, response, passingMap,
										String.valueOf(apiMap.get("token_api_method_type")).trim(),
										String.valueOf(apiMap.get("authorization_parameters_data_type")).trim(),
										headerParameterMap);
							} catch (ConnectException connectionError) {
								throw new ConnectException();
							}
							/**
							 * To Check if authToken is empty
							 */
							if (authToken != null) {
								/**
								 * Remove token key from Map and add Authorization Parameter
								 */

								apiMap.putAll(authToken);
								requestParameterMap.putAll(authToken);
								/**
								 * To insert Audit Log
								 */
								auditLogInsert(String.valueOf(requestParameterMap.get(TRACKING_MESSAGE_HEADER)).trim(),
										String.valueOf(apiMap.get(API_GROUP_ID)).trim(),
										String.valueOf(apiMap.get(API_ID)).trim(),
										Integer.parseInt(String.valueOf(environment.getProperty(LOG_SUCCESS_BIT))),
										"Token Management ",
										String.valueOf(environment.getProperty("log.status.token.success")), "Token",
										String.valueOf(authToken), request);
							} else {
								/**
								 * To insert Audit Log
								 */
								auditLogInsert(String.valueOf(requestParameterMap.get(TRACKING_MESSAGE_HEADER)).trim(),
										String.valueOf(apiMap.get(API_GROUP_ID)).trim(),
										String.valueOf(apiMap.get(API_ID)).trim(),
										Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))),
										"Token Management ",
										String.valueOf(environment.getProperty("log.status.token.failure")), "Token",
										String.valueOf(authToken), request);
								return null;
							}

						}

					}

				}
				urlWithTokenResponse.add(apiMap);
				/**
				 * Inserting Audit for Getting Token
				 */

			} catch (ConnectException connectionError) {
				throw new ConnectException();
			} catch (Exception e) {
				/**
				 * To Print Exception if it comes in console and throw exception
				 */
				logger.setLevel(org.apache.log4j.Level.ERROR);

				logger.error(e);

				urlWithTokenResponse.get(0).put("tokenError", "");
				auditLogInsert(String.valueOf(requestParameterMap.get(TRACKING_MESSAGE_HEADER)).trim(),
						String.valueOf(apiMap.get(API_GROUP_ID)).trim(), String.valueOf(apiMap.get(API_ID)).trim(),
						Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))),
						"Token Management  ", String.valueOf(environment.getProperty("log.status.token.failure")),
						"Token Error", String.valueOf(e.getMessage()), request);

			}
		} /**
			 * Returning Set of APIs with TOken if it required
			 */
		return urlWithTokenResponse;
	}

	/**
	 * This Method is used to call both ASYNC or SYNC Group of OL.The following
	 * Steps will be followed in this Method:
	 * 
	 * 1.It will get the set of APIs to be called as per the group id passed with
	 * respect to country_code and ICCID passed to get their respective host
	 * addresses of End Node APIs.
	 * 
	 * 2.It will check if the Group is ASYNC or SYNC
	 * 
	 * i. If the group is ASYNC it will follow the steps define below:
	 * 
	 * a.Call urlParameterValidator method to validate the client parameters with
	 * its mapping defined in the database.
	 * 
	 * b.Call tokenValid method to get the token for the APIs whose
	 * is_token_required field is true.
	 * 
	 * c.Call dataTransformater method to transform the all API of group with its
	 * parameter values
	 * 
	 * d.Call executeNotificationtoKafka method to push the transformed data into
	 * the kafka Queue.
	 * 
	 * ii.If the group is SYNC it will follow the steps define below:
	 * 
	 * a.Call urlParameterValidator method to validate the client parameters with
	 * its mapping defined in the database.
	 * 
	 * b.Call tokenValid method to get the token for the APIs whose
	 * is_token_required field is true.
	 * 
	 * c.Call getUrlParameters method to transform the all API to get its header and
	 * body parameters.
	 * 
	 * d.Call getOrchestrationData method to call the end node APIs and store the
	 * response parameters in Input Parameter Map.
	 * 
	 * 3.Send the Response of the Method.
	 * 
	 * 
	 * 
	 * @param groupID
	 * @param inputParameterMap
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */

	public ResponseEntity<?> genericExecuteApiMethod(String groupID, Map<String, String> parameterMap,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		/*
		 * Initialization of response message
		 */
		groupName = groupID;

		Map<String, String> responseMap = new LinkedHashMap<>();
		/**
		 * To get Custom error code as per API group.
		 */
		Map<String, Object> errorMAp = getErrorCodes(groupID, request, response);
		Map<String, String> inputParameterMap = new LinkedHashMap<>();
		try {

			/**
			 * To Check if tracking_message_header is not Empty
			 */

			inputParameterMap.putAll(parameterMap);
			if (parameterMap.containsKey("requestid")) {
				responseMap.put(TRACKING_MESSAGE_HEADER, String.valueOf(inputParameterMap.get("requestid")));
			} else if (parameterMap.containsKey(REQUEST_ID)) {
				responseMap.put(TRACKING_MESSAGE_HEADER, String.valueOf(inputParameterMap.get(REQUEST_ID)));
			} else {

				responseMap.put(TRACKING_MESSAGE_HEADER,
						String.valueOf(inputParameterMap.get(TRACKING_MESSAGE_HEADER)));
			}
			inputParameterMap.putAll(responseMap);

			/**
			 * Adding group id to parameter from configuration defined in
			 * ApiConfiguration.properties
			 */
			inputParameterMap.put(API_GROUP_ID, String.valueOf(groupID).trim());

			/**
			 * Calling Procedure to get set of API which need to be validated ,transformed
			 * and forwarded to kafka queue
			 */

			OrchestrationMessage getUrlsMessage = orchestrationGenericProcess.GenericOrchestrationProcedureCalling("1",
					inputParameterMap, null, request, response);

			/**
			 * To check if response is valid
			 */
			if (getUrlsMessage.isValid()) {
				/**
				 * Casting response in List<Map<String, Object>> format
				 */
				List<Map<String, Object>> urlResponse = (List<Map<String, Object>>) getUrlsMessage.getObject();

				if (!urlResponse.isEmpty()) {

					if (urlResponse.get(0).get("api_group_type").toString().trim().equalsIgnoreCase("ASYNC")) {
						groupLogID = urlResponse.get(0).get(API_GROUP_ID).toString().trim();
						return getAsyncRequestData(urlResponse, inputParameterMap, groupID, errorMAp, responseMap,
								request, response);

					} else {
						groupLogID = urlResponse.get(0).get(API_GROUP_ID).toString().trim();
						return getSyncRequestData(urlResponse, inputParameterMap, groupID, errorMAp, request, response);

					}

				} else {
					/**
					 * Response Of API
					 */
					auditLogInsert(String.valueOf(inputParameterMap.get(TRACKING_MESSAGE_HEADER)).trim(), groupLogID,
							null, Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))),
							"Invalid COUNTRY Code ", String.valueOf(environment.getProperty(LOG_FAILURE_RESPONSE_BIT)),
							"Invalid COUNTRY Code ",
							String.valueOf(responseMap).replaceAll(",", "|:").replaceAll("\'", DATABASE_DELIMETER),
							request);

					Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("1");

					LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
					errorMessage.remove(PRIORITY);
					errorMessage.put("code", errorMessage.get("code"));
					errorMessage.put(DESCRIPTION, "Country Code " + errorMessage.get(DESCRIPTION).toString()
							.concat(":" + "Invalid Country Code " + responseMap.toString()));

					List<Map<String, Object>> errorList = new LinkedList<>();
					errorList.add(errorMessage);
					Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
					finalErrorMessageMap.put(ERROR, errorList);
					return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.BAD_REQUEST);
				}

			} else {
				/**
				 * Response Of API
				 */
				auditLogInsert(String.valueOf(inputParameterMap.get(TRACKING_MESSAGE_HEADER)), groupLogID, null,
						Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))),
						"Internal Issue Occured From Database ",
						String.valueOf(environment.getProperty(LOG_FAILURE_RESPONSE_BIT)), ERROR_STRING,
						String.valueOf(responseMap).replaceAll(",", "|:").replaceAll("\'", DATABASE_DELIMETER),
						request);

				Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("3");

				LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
				errorMessage.remove(PRIORITY);
				errorMessage.put("code", errorMessage.get("code"));
				errorMessage.put(DESCRIPTION,
						errorMessage.get(DESCRIPTION).toString().concat(":" + PROCESS_FAIL + responseMap.toString()));
				List<Map<String, Object>> errorList = new LinkedList<>();
				errorList.add(errorMessage);
				Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
				finalErrorMessageMap.put(ERROR, errorList);
				return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);

			}

		} catch (

		Exception e) {
			/**
			 * To print the exception if it comes and return exception
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

			auditLogInsert(inputParameterMap.get(TRACKING_MESSAGE_HEADER),
					String.valueOf(environment.getProperty("group.id.auth")), null,
					Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))),
					"Internal Issue encoutered  ", String.valueOf(environment.getProperty(LOG_KAFKA_FAILURE_STATUS)),
					TEMPLATE, String.valueOf(e.getMessage()).replaceAll(",", "|:").replaceAll("\'", DATABASE_DELIMETER),
					request);
			Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("3");

			LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
			errorMessage.remove(PRIORITY);
			errorMessage.put("code", errorMessage.get("code"));
			errorMessage.put(DESCRIPTION,
					errorMessage.get(DESCRIPTION).toString().concat(":" + PROCESS_FAIL + responseMap.toString()));
			List<Map<String, Object>> errorList = new LinkedList<>();
			errorList.add(errorMessage);
			Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
			finalErrorMessageMap.put(ERROR, errorList);
			return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);

		}
	}

	private ResponseEntity<?> getSyncRequestData(List<Map<String, Object>> urlResponse,
			Map<String, String> inputParameterMap, String groupID, Map<String, Object> errorMAp,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		Object responseFromEndNode = null;

		/**
		 * To Validate Parameter
		 */
		Map<String, Map<String, String>> validatedParameterList = urlParameterValidator(urlResponse, inputParameterMap,
				request);
		/**
		 * To check if validatedParameterList size is zero
		 */
		if (validatedParameterList.size() == 0) {
			/**
			 * To get the Token from end nodes in API's where token is required
			 */
//			List<Map<String, Object>> tokenResponseList = tokenValid(urlResponse, inputParameterMap, request, response);

//			System.out.println("********** New Implementation started to resolve end node unreachable in case of token api ********************");
			ResponseEntity<?> syncResponse = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			List<Map<String, Object>> tokenResponseList = null;

			try {
				tokenResponseList = tokenValid(urlResponse, inputParameterMap, request, response);
			} catch (ConnectException connectionError) {
				StringBuilder responseString = new StringBuilder();
				responseString.append(String.valueOf("responseCode:500"));
				JSONObject responseJson = new JSONObject();
				responseJson.put("errorCode", "500");
				responseJson.put("errorMessage", "connection refused : end node not reachable");
				responseString.append(responseJson.toString());
				String tokenResponse = responseString.toString();

				String endNodeResponseError = String.valueOf(tokenResponse).substring(16);
				String responseCodeError = String.valueOf(tokenResponse).substring(13, 16);

				Map<String, String> endNodeResponseMapError = new LinkedHashMap<>();
				endNodeResponseMapError.put(API_GROUP_ID, String.valueOf(urlResponse.get(0).get(API_GROUP_ID)));
				endNodeResponseMapError.put(API_ID, String.valueOf(urlResponse.get(0).get(API_ID)));
				JsonOlcoreModification.parseOL(endNodeResponseError, endNodeResponseMapError);

				if (!String.valueOf(urlResponse.get(0).get(END_NODE_ERROR_KEY)).equalsIgnoreCase("null")
						&& endNodeResponseMapError
								.containsKey(String.valueOf(urlResponse.get(0).get(END_NODE_ERROR_KEY)))) {

					responseCodeError = endNodeResponseMapError
							.get(String.valueOf(urlResponse.get(0).get(END_NODE_ERROR_KEY)));
					endNodeResponseMapError.put(RESPONSE_CODE, responseCodeError);
				} else {
					endNodeResponseMapError.put(RESPONSE_CODE, responseCodeError);

				}

				OrchestrationMessage orchestrationMessageError = orchestrationGenericProcess
						.GenericOrchestrationProcedureCalling("11", endNodeResponseMapError, null, request, response);
				if (orchestrationMessageError.isValid()) {
					Map<String, String> responseMapParameter = responseParameterParse(
							String.valueOf(tokenResponse).substring(16), new LinkedHashMap<String, String>(),
							String.valueOf(groupID), urlResponse.get(0).get(API_ID).toString());
					inputParameterMap.putAll(responseMapParameter);
					/**
					 * Casting response in List<Map<String, Object>> format
					 */
					List<Map<String, String>> orchestrationMessageResponse = (List<Map<String, String>>) orchestrationMessageError
							.getObject();

					JsonOlcoreModification.parseOL(endNodeResponseError, endNodeResponseMapError);

					responseCodeError = endNodeResponseMapError.get("responseCode");
					inputParameterMap.put(API_ID, String.valueOf(urlResponse.get(0).get(API_ID)));
					inputParameterMap.put(RESPONSE_CODE, responseCodeError);
					auditLogInsert(String.valueOf(inputParameterMap.get(TRACKING_MESSAGE_HEADER)),
							String.valueOf(urlResponse.get(0).get(API_GROUP_ID)),
							String.valueOf(urlResponse.get(0).get(API_ID)),
							Integer.parseInt(String.valueOf(environment.getProperty(LOG_SUCCESS_BIT))),
							"Sync API Calling", String.valueOf(environment.getProperty("log.status.response.success")),
							"End Node API Response ", String.valueOf(tokenResponse).substring(16).replaceAll(",", "|:")
									.replaceAll("\'", DATABASE_DELIMETER),
							request);

					System.out.println("orchestrationMessageResponse SIZE " + orchestrationMessageResponse.size());
					switch (orchestrationMessageResponse.size()) {
					case 1:
						if (String.valueOf(orchestrationMessageResponse.get(0).get("notification_template_is_response"))
								.equalsIgnoreCase("1")) {

							syncResponse = genericApiResponseMethod(endNodeResponseError,
									orchestrationMessageResponse.get(0), response);
							break;
						} else {

							inputParameterMap.put(API_ID, urlResponse.get(0).get(API_ID).toString());

							syncResponse = genericApiFailureNotifactionMethod(orchestrationMessageResponse.get(0),
									inputParameterMap, request, response);
							break;
						}

					case 2:

						inputParameterMap.put(API_ID, String.valueOf(urlResponse.get(0).get(API_ID)));

						ResponseEntity<?> notificatPushStatus = genericApiFailureNotifactionMethod(
								orchestrationMessageResponse.get(0), inputParameterMap, request, response);
						if (notificatPushStatus.getStatusCode().is2xxSuccessful()) {

							syncResponse = genericApiResponseMethod(endNodeResponseError,
									orchestrationMessageResponse.get(1), response);
							break;
						} else {

							syncResponse = notificatPushStatus;
							break;
						}

					default:
						if (orchestrationMessageResponse.isEmpty() && endNodeResponseError
								.contains(String.valueOf(urlResponse.get(0).get(END_NODE_ERROR_KEY)))) {
							syncResponse = new ResponseEntity<>(String.valueOf(endNodeResponseError),
									HttpStatus.INTERNAL_SERVER_ERROR);
						} else {
							syncResponse = new ResponseEntity<>(String.valueOf(endNodeResponseError),
									HttpStatus.INTERNAL_SERVER_ERROR);
						}

						break;
					}
					return syncResponse;
//				System.out.println("********** New Implementation ended to resolve end node unreachable in case of token api ********************");
				}
			}

			List<Map<String, Object>> transformedDataList = dataTransformater(tokenResponseList, inputParameterMap,
					request);
//			ResponseEntity<?> syncResponse = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

			// NEWCODE

			Map<String, Object> responseMergerMap = new LinkedHashMap<>();
			for (Map<String, Object> map : transformedDataList) {

				/**
				 * Get Parameter for API calling
				 */

				Map<String, Object> passindMAp = getUrlParameters(map, inputParameterMap, request);
				/**
				 * Checking passindMAp is not null
				 */

				if (passindMAp != null) {

					Map<String, String> headerParameterMap = (Map<String, String>) passindMAp.get(HEADER_PARAM);

					/**
					 * To set header Parameter
					 */
					if (String.valueOf(map.get(API_TYPE)).equalsIgnoreCase("REST")) {
						if (headerParameterMap != null) {

							String hostUrl="";
							if(String.valueOf(map.get(API_URL))!=null)
							{
								hostUrl = String.valueOf(map.get(API_HOST_ADDRESS))
										.concat(String.valueOf(map.get(API_URL)));
							}
							else
							{
								 hostUrl = String.valueOf(map.get(API_HOST_ADDRESS));
										
							}
							
						
							responseFromEndNode = urlCalling.getOrchestrationData(hostUrl,
									String.valueOf(passindMAp.get(BODY_PARAM)), headerParameterMap,
									map.get("api_method_type").toString(), map.get(API_CONTENT_TYPE).toString());
						} else {
							/**
							 * To get Body Parameter
							 */
							String hostUrl="";
							if(String.valueOf(map.get(API_URL))!=null)
							{
								hostUrl = String.valueOf(map.get(API_HOST_ADDRESS))
										.concat(String.valueOf(map.get(API_URL)));
							}
							else
							{
								 hostUrl = String.valueOf(map.get(API_HOST_ADDRESS));
										
							}
							responseFromEndNode = urlCalling.getOrchestrationData(hostUrl,
									String.valueOf(passindMAp.get(BODY_PARAM)), null,
									map.get("api_method_type").toString(), map.get(API_CONTENT_TYPE).toString());

						}
					} else if (String.valueOf(map.get(API_TYPE)).equalsIgnoreCase("SOAP")) {
						String hostUrl="";
						if(String.valueOf(map.get(API_URL))!=null)
						{
							hostUrl = String.valueOf(map.get(API_HOST_ADDRESS))
									.concat(String.valueOf(map.get(API_URL)));
						}
						else
						{
							 hostUrl = String.valueOf(map.get(API_HOST_ADDRESS));
									
						}
						Object responseFromEndNodeNew = urlSoapCalling.getData(hostUrl,
								String.valueOf(passindMAp.get(BODY_PARAM)), headerParameterMap,
								map.get(API_CONTENT_TYPE).toString());

						if (responseFromEndNodeNew.toString().contains(SUCCESS_RESPONSE_STRING)) {
							JSONObject json = XML.toJSONObject(String.valueOf(responseFromEndNodeNew));

							responseFromEndNode = SUCCESS_RESPONSE_STRING + json.toString();

						} else if (responseFromEndNodeNew.toString().contains(ERROR_RESPONSE_STRING)) {
							responseFromEndNode = responseFromEndNodeNew;

						} else {
							Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("3");

							Map<String, Object> errorMessage = new HashMap<>((Map) errorMessageMap);
							errorMessage.remove(PRIORITY);
							errorMessage.put("code", errorMessage.get("code"));
							errorMessage.put(DESCRIPTION, errorMessage.get(DESCRIPTION).toString()
									.concat(":" + "Process Fail  " + responseFromEndNodeNew.toString()));

							List<Map<String, Object>> errorList = new LinkedList<>();
							errorList.add(errorMessage);
							Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
							finalErrorMessageMap.put(ERROR, errorList);
							return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);
						}

					}
				}

				/**
				 * To cast response in Json
				 */

				String endNodeResponse = String.valueOf(responseFromEndNode).substring(16);
				String responseCode = String.valueOf(responseFromEndNode).substring(13, 16);

				if (responseCode.equalsIgnoreCase("404")) {
					StringBuilder errResponse = new StringBuilder();
					JSONObject responseJson = new JSONObject();
					responseJson.put("errorCode", "404");
					responseJson.put("errorMessage", "resource not found");
					errResponse.append(responseJson.toString());
					endNodeResponse = errResponse.toString();
					responseFromEndNode = "responseCode:404" + endNodeResponse;
				} else if (responseCode.equalsIgnoreCase("301")) {
					StringBuilder errResponse = new StringBuilder();
					JSONObject responseJson = new JSONObject();
					responseJson.put("errorCode", "500");
					responseJson.put("errorMessage", "connection refused : end node not reachable");
					errResponse.append(responseJson.toString());
					endNodeResponse = errResponse.toString();
					responseFromEndNode = "responseCode:500" + endNodeResponse;
				}

				else if (responseCode.equalsIgnoreCase("401")) {
					StringBuilder errResponse = new StringBuilder();
					JSONObject responseJson = new JSONObject();
					responseJson.put("errorCode", "401");
					responseJson.put("errorMessage", "Unauthorized");
					errResponse.append(responseJson.toString());
					endNodeResponse = errResponse.toString();
					responseFromEndNode = "responseCode:401" + endNodeResponse;
				}

				Map<String, Object> endNodeResponseMap = new LinkedHashMap<>();
				Map<String, String> procedureCallingMap = new LinkedHashMap<>();
				endNodeResponseMap.put(API_GROUP_ID, String.valueOf(map.get(API_GROUP_ID)));
				endNodeResponseMap.put(API_ID, String.valueOf(map.get(API_ID)));

				procedureCallingMap.put(API_GROUP_ID, String.valueOf(map.get(API_GROUP_ID)));
				procedureCallingMap.put(API_ID, String.valueOf(map.get(API_ID)));

				JsonOlcoreModification.parseOLObject(endNodeResponse, endNodeResponseMap);

				if (!String.valueOf(map.get(END_NODE_ERROR_KEY)).equalsIgnoreCase("null")
						&& endNodeResponseMap.containsKey(String.valueOf(map.get(END_NODE_ERROR_KEY)))) {

					responseCode = (String) endNodeResponseMap.get(String.valueOf(map.get(END_NODE_ERROR_KEY)));

				}
				endNodeResponseMap.put(RESPONSE_CODE, responseCode);
				procedureCallingMap.put(RESPONSE_CODE, responseCode);

				/*
				 * Map<String, String> endNodeResponseMap2 = new LinkedHashMap<>(); for
				 * (Map.Entry<String, Object> entry : endNodeResponseMap.entrySet()) {
				 * 
				 * endNodeResponseMap2.put(entry.getKey(), (String) entry.getValue());
				 * 
				 * }
				 */

				OrchestrationMessage orchestrationMessage = orchestrationGenericProcess
						.GenericOrchestrationProcedureCalling("11", procedureCallingMap, null, request, response);
				if (orchestrationMessage.isValid()) {
					Map<String, String> responseMapParameter = responseParameterParse(
							String.valueOf(responseFromEndNode).substring(16), new LinkedHashMap<String, String>(),
							String.valueOf(groupID), map.get(API_ID).toString());
					inputParameterMap.putAll(responseMapParameter);
					/**
					 * Casting response in List<Map<String, Object>> format
					 */
					List<Map<String, String>> orchestrationMessageResponse = (List<Map<String, String>>) orchestrationMessage
							.getObject();

					JsonOlcoreModification.parseOLObject(endNodeResponse, endNodeResponseMap);

					responseCode = (String) endNodeResponseMap.get("responseCode");
					inputParameterMap.put(API_ID, String.valueOf(map.get(API_ID)));
					inputParameterMap.put(RESPONSE_CODE, responseCode);
					auditLogInsert(String.valueOf(inputParameterMap.get(TRACKING_MESSAGE_HEADER)),
							String.valueOf(map.get(API_GROUP_ID)), String.valueOf(map.get(API_ID)),
							Integer.parseInt(String.valueOf(environment.getProperty(LOG_SUCCESS_BIT))),
							"Sync API Calling", String.valueOf(environment.getProperty("log.status.response.success")),
							"End Node API Response ", String.valueOf(responseFromEndNode).substring(16)
									.replaceAll(",", "|:").replaceAll("\'", DATABASE_DELIMETER),
							request);

					System.out.println("orchestrationMessageResponse SIZE " + orchestrationMessageResponse.size());
					switch (orchestrationMessageResponse.size()) {
					case 1:
						if (String.valueOf(orchestrationMessageResponse.get(0).get("notification_template_is_response"))
								.equalsIgnoreCase("1")) {
							responseMergerMap.putAll(endNodeResponseMap);

							String gsonResponse = new Gson().toJson(responseMergerMap);

							syncResponse = genericApiResponseMethodNew(gsonResponse, responseMergerMap,
									orchestrationMessageResponse.get(0), response);

							// responseMergerMap.put(String.valueOf(map.get("api_sequence")),syncResponse.getBody());

							break;
						} else {

							inputParameterMap.put(API_ID, map.get(API_ID).toString());

							syncResponse = genericApiFailureNotifactionMethod(orchestrationMessageResponse.get(0),
									inputParameterMap, request, response);
							break;
						}

					case 2:

						inputParameterMap.put(API_ID, String.valueOf(map.get(API_ID)));

						ResponseEntity<?> notificatPushStatus = genericApiFailureNotifactionMethod(
								orchestrationMessageResponse.get(0), inputParameterMap, request, response);
						if (notificatPushStatus.getStatusCode().is2xxSuccessful()) {

							syncResponse = genericApiResponseMethod(endNodeResponse,
									orchestrationMessageResponse.get(1), response);
							break;
						} else {

							syncResponse = notificatPushStatus;
							break;
						}

					default:
						if (orchestrationMessageResponse.isEmpty()
								&& endNodeResponse.contains(String.valueOf(map.get(END_NODE_ERROR_KEY)))) {
							syncResponse = new ResponseEntity<>(String.valueOf(endNodeResponse),
									HttpStatus.INTERNAL_SERVER_ERROR);
						} else {
							syncResponse = new ResponseEntity<>(String.valueOf(endNodeResponse),
									HttpStatus.OK);
						}

						break;
					}

				} else {
					auditLogInsert(String.valueOf(inputParameterMap.get(TRACKING_MESSAGE_HEADER)),
							String.valueOf(map.get(API_GROUP_ID)), String.valueOf(map.get(API_ID)),
							Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))),
							"Sync API Calling", String.valueOf(environment.getProperty(LOG_FAILURE_RESPONSE_BIT)),
							"End Node API Response ", String.valueOf(responseFromEndNode).replaceAll(",", "|:")
									.replaceAll("\'", DATABASE_DELIMETER),
							request);
					Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("3");

					Map<String, Object> errorMessage = new HashMap<>((Map) errorMessageMap);
					errorMessage.remove(PRIORITY);
					errorMessage.put("code", errorMessage.get("code"));
					errorMessage.put(DESCRIPTION, String.valueOf(responseFromEndNode).substring(16));

					List<Map<String, Object>> errorList = new LinkedList<>();
					errorList.add(errorMessage);
					Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
					finalErrorMessageMap.put(ERROR, errorList);
					syncResponse = new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);
				}

			}

			/**
			 * Performing Required Actions on mergeResponseMap
			 * 
			 */

			/**
			 * API response of all sync API being called
			 */
			return syncResponse;

		} else {
			/**
			 * API response
			 */

			auditLogInsert(String.valueOf(inputParameterMap.get(TRACKING_MESSAGE_HEADER)), groupLogID, null,
					Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))), "Invalid Parameter ",
					String.valueOf(environment.getProperty(LOG_FAILURE_RESPONSE_BIT)),
					"Invalid Parameter API Response ",
					String.valueOf(validatedParameterList).replaceAll(",", "|:").replaceAll("\'", DATABASE_DELIMETER),
					request);
			if (validatedParameterList.containsKey(MANDATORY_VALIDATION_PARAM)) {
				Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("1");

				LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
				errorMessage.remove(PRIORITY);
				errorMessage.put("code", errorMessage.get("code"));
				errorMessage.put(DESCRIPTION, String.valueOf(validatedParameterList.get(MANDATORY_VALIDATION_PARAM))
						.replaceAll("=", "").replaceAll("\\{", "").replaceAll("\\}", ""));
				List<Map<String, Object>> errorList = new LinkedList<>();
				errorList.add(errorMessage);
				Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
				finalErrorMessageMap.put(ERROR, errorList);
				return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.BAD_REQUEST);
			} else {
				Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("2");

				LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
				errorMessage.remove(PRIORITY);
				errorMessage.put("code", errorMessage.get("code"));
				errorMessage.put(DESCRIPTION, String.valueOf(validatedParameterList.get("notSupportParameterMap"))
						.replaceAll("=", "").replaceAll("\\{", "").replaceAll("\\}", ""));
				List<Map<String, Object>> errorList = new LinkedList<>();
				errorList.add(errorMessage);
				Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
				finalErrorMessageMap.put(ERROR, errorList);
				return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.BAD_REQUEST);
			}
		}
	}

	/*
	 * private ResponseEntity<?> genericApiResponseMethod(String endNodeResponse,
	 * Map<String, String> map, HttpServletResponse response) { Map<String, String>
	 * httpStatusCodesMap = httpStatusParameter.getHttpCodeMap(); List<Map<String,
	 * Object>> finalResponseList = new LinkedList<>(); try {
	 * 
	 * if (String.valueOf(map.get(NOTIFICATION_TEMPLATE)).equalsIgnoreCase("null"))
	 * { StringBuffer sb = new StringBuffer(); if
	 * (String.valueOf(map.get("set_response_key")).equalsIgnoreCase("null") &&
	 * String.valueOf(map.get("get_response_key")).equalsIgnoreCase("null")) {
	 * Pattern pattern = Pattern.compile(TEMPLATE_PARAM_PATTERN); Matcher matcher =
	 * pattern.matcher(String.valueOf(map.get(NOTIFICATION_TEMPLATE))); Map<String,
	 * String> requesParameterMap = new LinkedHashMap<>();
	 * JsonOlcoreModification.parseOL(endNodeResponse, requesParameterMap); while
	 * (matcher.find()) { String matchCase = matcher.group(0);
	 * 
	 * if (matchCase.replaceAll(REPLACE_PARAM_PATTERN, "").contains("#&#")) { String
	 * matchCaseValue = String
	 * .valueOf(requesParameterMap.get(matchCase.replaceAll(REPLACE_PARAM_PATTERN,
	 * ""))); matcher.appendReplacement(sb, matchCaseValue); } }
	 * matcher.appendTail(sb);
	 * 
	 * } return new ResponseEntity<>(sb, HttpStatus.ACCEPTED); } else {
	 * 
	 * Type listType = new TypeToken<Map<String, Object>>() { }.getType();
	 * 
	 * if (String.valueOf(map.get(NOTIFICATION_TEMPLATE)).equals("{}")) { return new
	 * ResponseEntity<>("{}",
	 * HttpStatus.valueOf(httpStatusCodesMap.get(map.get("response_http_code")))); }
	 * 
	 * Map<String, Object> templateMapParam = new Gson()
	 * .fromJson(String.valueOf(map.get(NOTIFICATION_TEMPLATE)), listType);
	 * 
	 * List<Map<String, Object>> responseManipulatedList = null;
	 * if(templateMapParam.get(String.valueOf(map.get("set_response_key"))) != null)
	 * { responseManipulatedList = (List<Map<String, Object>>) templateMapParam
	 * .get(String.valueOf(map.get("set_response_key"))); } else {
	 * responseManipulatedList = new ArrayList<Map<String, Object>>();
	 * responseManipulatedList.add(templateMapParam); }
	 * 
	 * List<Map<String, Object>> responseManipulatedList = (List<Map<String,
	 * Object>>) templateMapParam .get(String.valueOf(map.get("set_response_key")));
	 * 
	 * // Map<String, Object> apiResponse = new
	 * Gson().fromJson(String.valueOf(endNodeResponse), listType); Map<String,
	 * Object> apiResponse = new ObjectMapper().readValue(endNodeResponse,
	 * Map.class);
	 *//**
		 * Casting externalExtraPasses field from apiResponse in List<Map<String,
		 * Object>>
		 */
	/*
	 * 
	 * logger.info("Response From End Node " + endNodeResponse); //
	 * logger.info("apiResponse After JSON to JAVA Object Conversion :"+apiResponse)
	 * ;
	 * 
	 * 
	 * List<Map<String, Object>> responseListToManipulate = (List<Map<String,
	 * Object>>) apiResponse .get(String.valueOf(map.get("get_response_key")));
	 * 
	 * List<Map<String, Object>> responseListToManipulate = null; if
	 * (apiResponse.get(String.valueOf(map.get("get_response_key"))) != null) {
	 * responseListToManipulate = (List<Map<String, Object>>) apiResponse
	 * .get(String.valueOf(map.get("get_response_key"))); } else {
	 * responseListToManipulate = new ArrayList<Map<String, Object>>();
	 * responseListToManipulate.add(apiResponse); }
	 * 
	 * if (responseListToManipulate != null && !responseListToManipulate.isEmpty())
	 * {
	 * 
	 * for (Map<String, Object> map2 : responseListToManipulate) {
	 * 
	 *//**
		 * Map to store response Parameter
		 */
	/*
	 * Map<String, Object> responseParameterMap = new LinkedHashMap<>();
	 *//**
		 * Too add all template parameter
		 */
	/*
	 * responseParameterMap.putAll(responseManipulatedList.get(0));
	 *//**
		 * responseManipulatedList Parameter to get expected Response
		 */
	/*
	 * for (String mapw : responseManipulatedList.get(0).keySet()) {
	 * 
	 * Pattern pattern = Pattern.compile(TEMPLATE_PARAM_PATTERN); Matcher matcherUrl
	 * = pattern .matcher(String.valueOf(responseManipulatedList.get(0).get(mapw)));
	 * if (matcherUrl.find()) { String value =
	 * responseManipulatedList.get(0).get(mapw).toString().replaceAll("[<>]", "");
	 * 
	 * StringBuilder finalValue = new StringBuilder(); if (value.contains(",")) {
	 * 
	 * String[] newValue = value.split(","); for (String string : newValue) {
	 * 
	 * if (map2.containsKey(string.trim())) {
	 * finalValue.append(map2.get(string.trim()) + " "); } else {
	 * finalValue.append(""); } responseParameterMap.put(mapw,
	 * String.valueOf(finalValue).trim()); }
	 * 
	 * } else if (map2.containsKey(value)) { responseParameterMap.put(mapw,
	 * map2.get(value)); } else { // responseParameterMap.put(mapw, ""); //
	 * previously only single statement in this else block
	 * 
	 * String valueData = responseManipulatedList.get(0).get(mapw).toString();
	 * List<String> matchList = new ArrayList<String>(); Pattern regex =
	 * Pattern.compile("<(.*?)>"); // pattern to metch content between <> Matcher
	 * regexMatcher = regex.matcher(valueData);
	 * 
	 * while (regexMatcher.find()) {//Finds Matching Pattern in String
	 * matchList.add(regexMatcher.group(1));//Fetching Group from String }
	 * 
	 * if (matchList.size() > 0) { for (String keyName : matchList) { if
	 * (map2.get(keyName) != null) { valueData =
	 * valueData.replaceAll("<"+keyName+">", map2.get(keyName).toString()); } else {
	 * valueData = valueData.replaceAll("<"+keyName+">", ""); } }
	 * responseParameterMap.put(mapw, valueData); } else {
	 * responseParameterMap.put(mapw, ""); }
	 * 
	 * } } else { responseParameterMap.put(mapw,
	 * responseManipulatedList.get(0).get(mapw)); } }
	 *//**
		 * Adding responseParameterMap in finalResponseList
		 */

	/*
	 * finalResponseList.add(responseParameterMap);
	 * 
	 * }
	 * 
	 * } else { return new ResponseEntity<>(endNodeResponse,
	 * HttpStatus.valueOf(httpStatusCodesMap.get(map.get("response_http_code")))); }
	 *//**
		 * Returning Response after adding finalResponseList in apiResponse
		 *//*
			 * apiResponse = new LinkedHashMap<>();
			 * 
			 * if (String.valueOf(map.get("set_response_key")).equals("null")) { apiResponse
			 * = finalResponseList.get(0); } else {
			 * apiResponse.put(String.valueOf(map.get("set_response_key")),
			 * finalResponseList); }
			 * 
			 * 
			 * logger.info("Response after manipulation and modification...... " +
			 * apiResponse);
			 * 
			 * return new ResponseEntity<>(new Gson().toJson(apiResponse),
			 * HttpStatus.valueOf(httpStatusCodesMap.get(map.get("response_http_code")))); }
			 * } catch (Exception e) { e.printStackTrace(); logger.info(e); // need to
			 * change return new ResponseEntity<>(new LinkedHashMap<>(),
			 * HttpStatus.ACCEPTED); }
			 * 
			 * }
			 */
	private ResponseEntity<?> getAsyncRequestData(List<Map<String, Object>> urlResponse,
			Map<String, String> inputParameterMap, String groupID, Map<String, Object> errorMAp,
			Map<String, String> responseMap, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		OrchestrationMessage responseMessage = new OrchestrationMessage();
		/**
		 * Declaration of List to store inputParameterMap
		 */
		List<Map<String, String>> requestParameterMap = new LinkedList<>();
		/*
		 * Adding inputParameterMap in requestParameterMap
		 */
		requestParameterMap.add(inputParameterMap);
		/**
		 * Calling Method to Validate the Parameter
		 */
		Map<String, Map<String, String>> validatedParameterList = urlParameterValidator(urlResponse, inputParameterMap,
				request);
		/**
		 * If parameters are valid than the validatedParameterList size will be 0.
		 */
		if (validatedParameterList.size() == 0) {
			/**
			 * To get the Token from end nodes in API's where token is required
			 */
			List<Map<String, Object>> tokenResponseList = tokenValid(urlResponse, inputParameterMap, request, response);
			if (tokenResponseList == null) {

				Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("3");

				LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
				errorMessage.remove(PRIORITY);
				errorMessage.put("code", errorMessage.get("code"));
				errorMessage.put(DESCRIPTION, errorMessage.get(DESCRIPTION).toString()
						.concat(":" + "Process Fail Issue in token Management " + responseMap.toString()));
				List<Map<String, Object>> errorList = new LinkedList<>();
				errorList.add(errorMessage);
				Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
				finalErrorMessageMap.put(ERROR, errorList);
				return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);
			}

			/**
			 * To Transformed the data for API's to be called in the format of their
			 * respective calling format
			 */
			List<Map<String, Object>> transformedDataList = dataTransformater(tokenResponseList, inputParameterMap,
					request);
			/**
			 * To check the size of transformedDataList.If data transformed successfully
			 * than the size will be more than 0 else list will be null
			 */
			if (!transformedDataList.isEmpty()) {
				/**
				 * Calling Method to Push Data in Kafka queue
				 */
				Boolean kafkaStatus = executeNotificationtoKafka(transformedDataList, "publisher", request, response);
				/**
				 * To Check kafkaStatus.It will be true if data pushed successfully in kafka
				 * else it will be false
				 */
				if (kafkaStatus) {
					/**
					 * Inserting Audit Log for data pushed successfully in kafka
					 */
					auditLogInsert(inputParameterMap.get(TRACKING_MESSAGE_HEADER),
							String.valueOf(transformedDataList.get(0).get(API_GROUP_ID)), null,
							Integer.parseInt(String.valueOf(environment.getProperty(LOG_SUCCESS_BIT))),
							"Pushed to kafka",
							String.valueOf(environment.getProperty("log.status.kafka.execution.success")), TEMPLATE,
							String.valueOf(transformedDataList).replaceAll(",", "|:")
									.replaceAll("\'", DATABASE_DELIMETER).replaceAll("\'", DATABASE_DELIMETER),
							request);
					/**
					 * Response Of API
					 */
					responseMessage
							.setDescription("Your Request has succesfully Registered and forwarded for Processing");
					responseMessage.setObject(responseMap);
					responseMessage.setValid(true);
					return new ResponseEntity<>(HttpStatus.ACCEPTED);
				} else {
					/**
					 * Inserting Audit Log for data not pushed successfully in kafka
					 */
					auditLogInsert(inputParameterMap.get(TRACKING_MESSAGE_HEADER), groupLogID, null,
							Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))),
							"Kafka issue encoutered  ",
							String.valueOf(environment.getProperty(LOG_KAFKA_FAILURE_STATUS)), TEMPLATE,
							String.valueOf(transformedDataList).replaceAll(",", "|:")
									.replaceAll("\'", DATABASE_DELIMETER).replaceAll("\'", DATABASE_DELIMETER),
							request);
					/**
					 * Response Of API
					 */
					Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("3");

					Map<String, Object> errorMessage = new HashMap<>((Map) errorMessageMap);
					errorMessage.remove(PRIORITY);
					errorMessage.put("code", errorMessage.get("code"));
					errorMessage.put(DESCRIPTION, errorMessage.get(DESCRIPTION).toString().concat(":"
							+ "Kafka issue Encountered... Unable to process the request. " + responseMap.toString()));

					List<Map<String, Object>> errorList = new LinkedList<>();
					errorList.add(errorMessage);
					Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
					finalErrorMessageMap.put(ERROR, errorList);
					return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);

				}
			} else {
				auditLogInsert(inputParameterMap.get(TRACKING_MESSAGE_HEADER), groupLogID, null,
						Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))),
						"Transformation issue encoutered  ",
						String.valueOf(environment.getProperty("log.status.transformation.failure")), TEMPLATE,
						String.valueOf(transformedDataList).replaceAll(",", "|:").replaceAll("\'", DATABASE_DELIMETER),
						request);
				/**
				 * Response Of API
				 */
				Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("4");

				LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
				errorMessage.remove(PRIORITY);
				errorMessage.put("code", errorMessage.get("code"));
				errorMessage.put(DESCRIPTION, errorMessage.get(DESCRIPTION).toString()
						.concat(":" + "Data Transformation issue encoutered " + responseMap.toString()));
				List<Map<String, Object>> errorList = new LinkedList<>();
				errorList.add(errorMessage);
				Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
				finalErrorMessageMap.put(ERROR, errorList);
				return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.BAD_REQUEST);

			}

		} else {
			/**
			 * Response Of API
			 */
			auditLogInsert(inputParameterMap.get(TRACKING_MESSAGE_HEADER), groupLogID, null,
					Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))), "Invalid Parameter",
					String.valueOf(environment.getProperty("log.status.transformation.failure")),
					"Invalid Parameter List",
					String.valueOf(validatedParameterList).replaceAll(",", "|:").replaceAll("\'", DATABASE_DELIMETER),
					request);
			if (validatedParameterList.containsKey(MANDATORY_VALIDATION_PARAM)) {
				Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("1");

				LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
				errorMessage.remove(PRIORITY);
				errorMessage.put("code", errorMessage.get("code"));
				// "Invalid Parameter :"+
				errorMessage.put(DESCRIPTION, String.valueOf(validatedParameterList.get(MANDATORY_VALIDATION_PARAM))
						.replaceAll("=", "").replaceAll("\\{", "").replaceAll("\\}", ""));
				List<Map<String, Object>> errorList = new LinkedList<>();
				errorList.add(errorMessage);
				Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
				finalErrorMessageMap.put(ERROR, errorList);
				return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.BAD_REQUEST);
			} else {
				Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("2");

				LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
				errorMessage.remove(PRIORITY);
				errorMessage.put("code", errorMessage.get("code"));
				errorMessage.put(DESCRIPTION, String.valueOf(validatedParameterList.get("notSupportParameterMap"))
						.replaceAll("=", "").replaceAll("\\{", "").replaceAll("\\}", ""));
				List<Map<String, Object>> errorList = new LinkedList<>();
				errorList.add(errorMessage);
				Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
				finalErrorMessageMap.put(ERROR, errorList);
				return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.BAD_REQUEST);
			}

		}
	}

	/**
	 * Inserting Audit Log with its required Parameter
	 * 
	 * @param tracking_message_header
	 * @param api_group_id
	 * @param api_id
	 * @param is_successful
	 * @param message
	 * @param state_id
	 * @param name
	 * @param value
	 * @param request:::To            get HTTP Basic authentication,where consumer
	 *                                sends the ‘user_name’ and ‘password’ separated
	 *                                by ‘:’, within a base64 and requestId and
	 *                                returnURL from request header
	 * 
	 * @param response:::To           send response
	 * 
	 * @return Return the response message
	 * @throws Exception
	 */
	public void auditLogInsert(String trackingMessageHeader, String apiGroupId, String apiId, int isSuccessful,
			String message, String stateId, String name, String value, HttpServletRequest request) throws Exception {
		try {
			/**
			 * To get list of Procedures from mvc-dispatcher-servlet.xml
			 */
			Map<String, Object> errorLogMap = processParameter.getMaps();
			/**
			 * Calling Procedure to insert Audit Log
			 */

			System.out.println("logData:-" + value.replaceAll(",", "|#@|"));
			genericService.executeOrchestrationProcesure(null, errorLogMap.get("6").toString(), trackingMessageHeader,
					apiGroupId, apiId, isSuccessful, message, stateId, name.replaceAll(",", "|#@|"),
					value.replaceAll(",", "|#@|"), request.getAttribute("user_name"));

		} catch (Exception e) {
			/**
			 * To Print Exception if it comes in console and throw exception
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

			throw e;
		}

	}

	/**
	 * Method To call end Node APi to get Authorization parameter defined in token
	 * response
	 * 
	 * @param hostUrl             End Node API URL
	 * @param urlPassingParameter Passing Parameter of API
	 * @param request             -HttpServletRequest request to get requested
	 *                            machines urlPassingParameter like host ,url
	 *                            ,headers
	 * @param response
	 * @param passingMap          parameter Map to add response parameter with its
	 *                            value
	 * @param headerParameterMap
	 * @param request:::To        get HTTP Basic authentication,where consumer sends
	 *                            the ‘user_name’ and ‘password’ separated by ‘:’,
	 *                            within a base64 and requestId and returnURL from
	 *                            request header
	 * 
	 * @param response:::To       send response
	 * 
	 * @return Return the response message
	 * @throws Exception
	 */
	private Map<String, String> getToken(String hostUrl, String urlPassingParameter, HttpServletRequest request,
			HttpServletResponse response, Map<String, String> passingMap, String methodType, String contentType,
			Map<String, String> headerParameterMap) throws ConnectException, Exception {
		/**
		 * Calling End Node API
		 */
		Object newToken = urlCalling.getOrchestrationData(hostUrl, urlPassingParameter, headerParameterMap, methodType,
				contentType);
		if (newToken.toString().contains(ERROR_RESPONSE_STRING) || newToken.toString().contains("responseCode:301")) {
			throw new ConnectException();
		}

		try {

			/**
			 * Initializing authMap which will store Authorization Parameter with its value
			 */

			Map<String, String> authMap = new LinkedHashMap<>();
			/**
			 * To check if response is empty from API called.
			 */

			/**
			 * To get all token_response in array of String
			 */
			String[] responseParameter = passingMap.get(TOKEN_RESPONSE).split(",");
			/**
			 * To get each Authorization parameter with its value from response
			 */

			/**
			 * Casting response in String format
			 */

			if (newToken.toString().contains(SUCCESS_RESPONSE_STRING)) {

				Map<String, String> authResponseMap = new LinkedHashMap<>();
				JsonOlcoreModification.parseOL(String.valueOf(newToken).substring(16), authResponseMap);

				/**
				 * Storing Authorization parameter with its value
				 */

				for (String string : responseParameter) {
					logger.info("Token Response Key ------------------ " + string);
					if (authResponseMap.containsKey(string)) {
						authMap.put(string, authResponseMap.get(string));
					}
				}

			} else {
				auditLogInsert(String.valueOf(passingMap.get(TRACKING_MESSAGE_HEADER)),
						String.valueOf(passingMap.get(API_GROUP_ID)), String.valueOf(passingMap.get(API_ID)),
						Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))), "Token",
						String.valueOf(environment.getProperty("log.status.token.failure")), "Token Response",
						String.valueOf(newToken), request);
				return null;
			}

			passingMap.put(TOKEN, String.valueOf(authMap.get(responseParameter[0])));

			/**
			 * Inserting Token in Database
			 */

			orchestrationGenericProcess.GenericOrchestrationProcedureCalling("3", passingMap, null, request, response);
			/**
			 * Returning authMap Authorization parameter with its value
			 */
			auditLogInsert(String.valueOf(passingMap.get(TRACKING_MESSAGE_HEADER)),
					String.valueOf(passingMap.get(API_GROUP_ID)), String.valueOf(passingMap.get(API_ID)),
					Integer.parseInt(String.valueOf(environment.getProperty(LOG_SUCCESS_BIT))), "Token ",
					String.valueOf(environment.getProperty("log.status.token.success")), "Token Response",
					String.valueOf(newToken), request);
			return authMap;

		} catch (Exception e) {
			/**
			 * To Print Exception if it comes in console and throw exception
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

			auditLogInsert(String.valueOf(passingMap.get(TRACKING_MESSAGE_HEADER)),
					String.valueOf(passingMap.get(API_GROUP_ID)), String.valueOf(passingMap.get(API_ID)),
					Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))), "Token ",
					String.valueOf(environment.getProperty("log.status.token.failure")), "Token Error Response",
					String.valueOf(newToken), request);
			return null;
		}

	}

	/**
	 * To Validate the Parameter
	 * 
	 * @param tokenResponse
	 * @param requestParameterMap
	 * @param request:::To        get HTTP Basic authentication,where consumer sends
	 *                            the ‘user_name’ and ‘password’ separated by ‘:’,
	 *                            within a base64 and requestId and returnURL from
	 *                            request header
	 * 
	 * @param response:::To       send response
	 * 
	 * @return Return the response message
	 * @throws Exception
	 */
	public Map<String, Map<String, String>> urlParameterValidator(List<Map<String, Object>> validRequestData,
			Map<String, String> requestParameterMap, HttpServletRequest request) throws Exception {
		/**
		 * Initializing response List
		 */
		Map<String, Map<String, String>> responseParameter = new LinkedHashMap<>();
		try {
			/*
			 * To get each API object from Group
			 */
			Map<String, String> notSupportParameterMap = new LinkedHashMap<>();
			Map<String, String> mandatoryParameterMap = new LinkedHashMap<>();
			for (Map<String, Object> apiMap : validRequestData) {

				StringBuilder validatedParameterValue = new StringBuilder();
				StringBuilder validatedParameterName = new StringBuilder();
				/**
				 * Splitting Parameter to get all input_parameters_client_parameter with its
				 * validations to validate parameter
				 */

				String[] parameterDatalength = String.valueOf(apiMap.get("input_parameters_length")).split("\\#\\$\\#");
				String[] parameterDataRegex = String.valueOf(apiMap.get("input_parameters_regex")).split("\\#\\$\\#");
				String[] parameterDataGMParameter = String.valueOf(apiMap.get("input_parameters_client_parameter"))
						.split(",");
				String[] parameterDataGMParameterValue = String
						.valueOf(apiMap.get("input_parameters_client_parameter_value")).split(",");
				String[] inputParameterValue = String.valueOf(apiMap.get("input_parameters_name")).split(",");
				String[] inputParameterRequiredValue = String.valueOf(apiMap.get("input_parameters_is_required"))
						.split(",");
				String[] inputParameterRegexDescription = String
						.valueOf(apiMap.get("input_parameters_regex_description")).split("\\#\\$\\#");
				/**
				 * Getting Each Parameter with its validation constraints
				 */
				for (int i = 0; i < parameterDataGMParameter.length; i++) {
					/**
					 * To get parameter mapping with user's Parameter on the basis of mandatory
					 * parameter and value not supported
					 */
					if (!parameterDataGMParameter[i].trim().equalsIgnoreCase("null")
							&& !String.valueOf(apiMap.get(TOKEN_RESPONSE)).contains(parameterDataGMParameter[i])) {
						String parameterValue = String.valueOf(requestParameterMap.get(parameterDataGMParameter[i]));
						if (inputParameterRequiredValue[i].trim().equalsIgnoreCase("1")) {
							if (!parameterValue.trim().equalsIgnoreCase("")
									&& !parameterValue.trim().equalsIgnoreCase("null")) {
								Map<String, String> parameterVAlidationMap = validate(parameterValue,
										parameterDatalength[i], parameterDataRegex[i], parameterDataGMParameter[i],
										inputParameterRegexDescription[i]);
								/**
								 * If Parameter is Valid
								 */
								if (parameterVAlidationMap != null) {

									notSupportParameterMap.putAll(parameterVAlidationMap);

									validatedParameterName.append(parameterDataGMParameter[i] + ",");
									validatedParameterValue
											.append(String
													.valueOf(parameterVAlidationMap.get(parameterDataGMParameter[i])
															.concat(String.valueOf(parameterVAlidationMap
																	.get(parameterDataGMParameter[i]))))
													.replaceAll(",", "") + ",");
								} else {
									validatedParameterName.append(parameterDataGMParameter[i] + ",");
									validatedParameterValue.append(
											String.valueOf(requestParameterMap.get(parameterDataGMParameter[i])) + ",");
								}
							} else {
								mandatoryParameterMap.put(parameterDataGMParameter[i], " is Mandatory Parameter");
							}

						} else {
							/**
							 * If parameter is optional than its value will be 0 .Here we will validate
							 * parameter only in case is some value comes.
							 */
							if ((inputParameterRequiredValue[i].trim().equalsIgnoreCase("0")
									&& !parameterValue.equalsIgnoreCase("null"))
									&& !parameterValue.equalsIgnoreCase("")) {

								Map<String, String> parameterVAlidationMap = validate(parameterValue,
										parameterDatalength[i], parameterDataRegex[i], parameterDataGMParameter[i],
										inputParameterRegexDescription[i]);
								/**
								 * If Parameter is Valid
								 */
								if (parameterVAlidationMap != null) {
									notSupportParameterMap.putAll(parameterVAlidationMap);
									validatedParameterName.append(parameterDataGMParameter[i] + ",");
									validatedParameterValue
											.append(String
													.valueOf(parameterVAlidationMap.get(parameterDataGMParameter[i])
															.concat(String.valueOf(parameterVAlidationMap
																	.get(parameterDataGMParameter[i]))))
													.replaceAll(",", "") + ",");
								} else {
									validatedParameterName.append(parameterDataGMParameter[i] + ",");
									validatedParameterValue.append(
											String.valueOf(requestParameterMap.get(parameterDataGMParameter[i])) + ",");
								}
							}
						}

					} else {
						/**
						 * Adding Parameter for Logs
						 */
						validatedParameterName.append(inputParameterValue[i] + ",");
						validatedParameterValue.append(String.valueOf(parameterDataGMParameterValue[i]) + ",");
					}

				}
				if (notSupportParameterMap.size() > 0) {
					responseParameter.put("notSupportParameterMap", notSupportParameterMap);
				}
				if (mandatoryParameterMap.size() > 0) {
					responseParameter.put(MANDATORY_VALIDATION_PARAM, mandatoryParameterMap);
				}
				if (responseParameter.size() > 0) {
					/**
					 * To Insert Audit Of API Parameter if parameter is valid
					 */
					auditLogInsert(String.valueOf(requestParameterMap.get(TRACKING_MESSAGE_HEADER)),
							String.valueOf(apiMap.get(API_GROUP_ID)), String.valueOf(apiMap.get(API_ID)),
							Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))),
							"Parameter is not  valid",
							String.valueOf(environment.getProperty("log.status.validation.failure")),
							String.valueOf(validatedParameterName), String.valueOf(validatedParameterValue), request);
				} else {
					/**
					 * To Insert Audit Of API Parameter if its not valid
					 */

					auditLogInsert(String.valueOf(requestParameterMap.get(TRACKING_MESSAGE_HEADER)),
							String.valueOf(apiMap.get(API_GROUP_ID)), String.valueOf(apiMap.get(API_ID)),
							Integer.parseInt(String.valueOf(environment.getProperty(LOG_SUCCESS_BIT))),
							"Parameter is valid",
							String.valueOf(environment.getProperty("log.status.validation.success")),
							String.valueOf(validatedParameterName), String.valueOf(validatedParameterValue), request);
				}

			}
			return responseParameter;
		} catch (Exception e) {
			/**
			 * To Print Exception if it comes in console and throw exception
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

			return null;

		}
	}

	/**
	 * This Method will Validate Parameter on the basis of :
	 * 
	 * 1.Length : It will check the length of the parameter defined in configuration
	 * database that to defined in the following sub checks:
	 * 
	 * i .less than (<): in this parameterDatalength is defined in >x format.
	 * 
	 * ii. greater than(>):in this parameterDatalength is defined in <x format.
	 * 
	 * iii equals(=):in this parameterDatalength is defined in =x format.
	 * 
	 * iv between(x <value <y):in this parameterDatalength is defined in xandy
	 * format.
	 * 
	 * 
	 * 2.Regex : It will check the regex of the parameter defined in configuration
	 * database .It will compile the regex using Pattern and Matcher class.
	 * 
	 * @param parameterValue                 It contains client value
	 * 
	 * @param parameterDatalength            It contains length defined in database
	 *                                       for respective parameter
	 * 
	 * @param parameterDataRegex             It contains the Regex defined for the
	 *                                       given parameter in database
	 * 
	 * @param parameterDataGMParameter       It contains the mapping of the client
	 *                                       parameter with api parameters
	 * @param inputParameterRegexDescription
	 * 
	 * @return If parameter is valid returns null else return invalid parameter
	 */
	private Map<String, String> validate(String parameterValue, String parameterDatalength, String parameterDataRegex,
			String parameterDataGMParameter, String inputParameterRegexDescription) {
		/**
		 * validationMap return parameter with their parameterVAlidationMap
		 */

		Map<String, String> validationMap = new LinkedHashMap<>();
		try {

			/**
			 * If no Constraint defined than parameter parameterVAlidationMap will be true
			 */
			if (parameterDatalength.trim().equalsIgnoreCase("null")
					&& parameterDataRegex.trim().equalsIgnoreCase("null")) {
				return null;
			}

			/**
			 * To check if length is > than given length
			 */
			if (!parameterDatalength.trim().equalsIgnoreCase("null")) {

				if (parameterDatalength.trim().contains(">")) {

					int length = Integer.parseInt(parameterDatalength.substring(parameterDatalength.indexOf('>') + 1));

					if (parameterValue.trim().length() <= length) {
						validationMap.put(parameterDataGMParameter, inputParameterRegexDescription);

					}
				}
				/**
				 * To check if length is < than given length
				 */
				if (parameterDatalength.contains("<")) {

					int length = Integer.parseInt(parameterDatalength.substring(parameterDatalength.indexOf('<') + 1));

					if (parameterValue.length() >= length) {

						validationMap.put(parameterDataGMParameter, inputParameterRegexDescription);

					}
				}
				/**
				 * To check if length is between the given length
				 */
				if (parameterDatalength.contains("and")) {

					String[] lengths = parameterDatalength.split("and");

					if ((parameterValue.length() >= Integer.parseInt(lengths[0]))
							&& (parameterValue.length() <= Integer.parseInt(lengths[1]))) {

						validationMap.put(parameterDataGMParameter, inputParameterRegexDescription);

					}

				}
				/**
				 * To check if length is equals to given length
				 */
				if (parameterDatalength.contains("=")) {

					int length = Integer.parseInt(parameterDatalength.substring(parameterDatalength.indexOf('=') + 1));

					if (parameterValue.length() != length) {

						validationMap.put(parameterDataGMParameter, inputParameterRegexDescription);

					}
				}

				/**
				 * To Check if Parameter has same length
				 */

			}
			/**
			 * If Regex Constraint is there
			 */
			if (!parameterDataRegex.equalsIgnoreCase("null")) {

				Pattern pattern = Pattern.compile(parameterDataRegex);
				Matcher matcher = pattern.matcher(parameterValue.trim());

				if (!matcher.matches()) {
					validationMap.put(parameterDataGMParameter, inputParameterRegexDescription);

				}

			}
			/***
			 * Returns validationMap
			 */
			if (validationMap.size() > 0) {
				return validationMap;
			} else {
				return null;
			}

		} catch (Exception e) {
			/**
			 * If exception comes
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

			/**
			 * To bypass the internal error
			 */
			return null;

		}

	}

	/**
	 * This Method is used to push the transformed data to the kafka queue by
	 * fetching kafka config from database by passing kafka type.i.e weather it is
	 * producer or subscriber of the kafka.This Method will take the data Which
	 * needs to send in List<Map<String, Object>> and transform it in json format
	 * and than fetch data from DB of Kafka and than push it in defined Kafka Queue
	 * 
	 * @param tokenResponse
	 * @param request:::To  get HTTP Basic authentication,where consumer sends the
	 *                      ‘user_name’ and ‘password’ separated by ‘:’, within a
	 *                      base64 and requestId and returnURL from request header
	 * 
	 * @param response:::To send response
	 * 
	 * @return Return the Boolean bit of success or failure
	 * @throws Exception
	 */
	public Boolean executeNotificationtoKafka(List<Map<String, Object>> tokenResponse, String kafkaType,
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		try {
			/**
			 * Transforming coming data in json string format
			 */

			String commandNotificationJson = new Gson().toJson(tokenResponse);
			/**
			 * To check if string is not null
			 */
			if (commandNotificationJson != null) {
				/**
				 * To get kafka config from database
				 */
				Map<String, String> map = new LinkedHashMap<>();
				map.put("kafka_type", kafkaType);
				/**
				 * Calling generic Procedure to get kafka config
				 */

				OrchestrationMessage message = orchestrationGenericProcess.GenericOrchestrationProcedureCalling("5",
						map, null, request, response);

				/*
				 * To transform response coming from database
				 */
				List<Map<String, Object>> kafkaConfigData = (List<Map<String, Object>>) message.getObject();
				if (message.isValid() && !kafkaConfigData.isEmpty()) {
					Properties kafkaProperties = new Properties();
					/**
					 * Defining kakfa config as per coming from database
					 */
					kafkaProperties.put("zookeeper.connect", kafkaConfigData.get(0).get("zookeeper_ip") + ":"
							+ kafkaConfigData.get(0).get("zookeeper_port"));

					kafkaProperties.put("metadata.broker.list",
							kafkaConfigData.get(0).get("kafka_ip") + ":" + kafkaConfigData.get(0).get("kafka_port"));
					kafkaProperties.put("serializer.class", "kafka.serializer.StringEncoder");

					kafkaProperties.put("request.required.acks", "1");
					/**
					 * Defining Kafka Producer Config
					 */
					ProducerConfig producerConfig = new ProducerConfig(kafkaProperties);
					/**
					 * KeyedMessage message with data to publish in kafka
					 */
					KeyedMessage<String, String> messageToPublish = new KeyedMessage<>(
							String.valueOf(kafkaConfigData.get(0).get("topic_name")), "ip", commandNotificationJson);
					logger.setLevel(org.apache.log4j.Level.INFO);
					logger.info("data after: \n" + messageToPublish);

					/*
					 * Send the data on kafka.
					 */
					Producer<String, String> kafkaProducer = new Producer<>(producerConfig);
					/**
					 * To Send Data to Kafka
					 */

					kafkaProducer.send(messageToPublish);

					/*
					 * Close the kafka producer connection.
					 */
					kafkaProducer.close();
					/*
					 * If data published successfully it will send true in return
					 */
					return true;
				} else {
					/*
					 * If data not published successfully it will send false in return
					 */
					return false;
				}

			} else {
				/*
				 * If data is null , it will send false in return
				 */
				return false;
			}

		} catch (Exception e) {
			/**
			 * To get exception if it comes
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

			return false;
		}
	}

	/**
	 * Method is used to transform the each API call which is defined in the group
	 * API and will be called by Streaming layer.The process this method follow are
	 * as follow
	 * 
	 * 1. Split the following parameters in String Array:
	 * 
	 * 
	 * i.input_parameters_client_parameter
	 * 
	 * ii.input_parameters_name
	 * 
	 * iii.input_parameters_client_parameter_value
	 * 
	 * iv.input_parameter_is_header
	 * 
	 * v.api_response_parameter_value
	 * 
	 * 
	 * 2.Map the each parameter with its mapped parameter with client or static
	 * value or a response parameter of API and store it in new String List name
	 * inputParameterValue.
	 * 
	 * 3.Remove key which will be not used by Streaming and add few new key for the
	 * streaming use.
	 * 
	 * 4.Will insert Audit log for each API being transformed
	 * 
	 * 
	 * 
	 * @param tokenResponse
	 * @param requestParameterMap
	 * @param request:::To        get HTTP Basic authentication,where consumer sends
	 *                            the ‘user_name’ and ‘password’ separated by ‘:’,
	 *                            within a base64 and requestId and returnURL from
	 *                            request header
	 * 
	 * @param response:::To       send response
	 * 
	 * @return Return the response message
	 * @throws Exception
	 */
	public List<Map<String, Object>> dataTransformater(List<Map<String, Object>> tokenResponse,
			Map<String, String> requestParameter, HttpServletRequest request) throws Exception {
		/**
		 * To get each API set from group
		 */
		for (Map<String, Object> apiMap : tokenResponse) {
			try {
				/**
				 * Splitting Parameter To Transform
				 */

				String[] parameterGMParameter = String.valueOf(apiMap.get("input_parameters_client_parameter"))
						.split(",");
				String[] inputParameterName = String.valueOf(apiMap.get("input_parameters_name")).split(",");
				String[] parameterDataGMParameterValue = String
						.valueOf(apiMap.get("input_parameters_client_parameter_value")).split(",");

				String[] responseParameterValue = String.valueOf(apiMap.get("api_response_parameter_value")).split(",");

				String[] isPrefixValue = String.valueOf(apiMap.get("input_parameters_prefix_value")).split(",");
				String[] isPrefixBit = String.valueOf(apiMap.get("input_parameters_is_prefix")).split(",");

				String[] isSuffixValue = String.valueOf(apiMap.get("input_parameters_suffix_value")).split(",");
				String[] isSuffix = String.valueOf(apiMap.get("input_parameters_is_suffix")).split(",");

				List<String> inputParameterValue = new LinkedList<>();
				/**
				 * To get each Parameter to transform it accordingly
				 */

				for (int i = 0; i < inputParameterName.length; i++) {
					/**
					 * to check mapping client parameter
					 */
					if (!parameterGMParameter[i].trim().equalsIgnoreCase("null")) {

						if (apiMap.get(parameterGMParameter[i]) != null) {
							if (String.valueOf((isPrefixBit[i])).trim().equals("1")) {
								inputParameterValue.add(String.valueOf(isPrefixValue[i])
										+ String.valueOf(apiMap.get(parameterGMParameter[i])));
							}

							else {
								inputParameterValue.add(String.valueOf(apiMap.get(parameterGMParameter[i])));

							}

							// inputParameterValue.add(String.valueOf(apiMap.get(parameterGMParameter[i])));
						} else {
							inputParameterValue.add(requestParameter.get(parameterGMParameter[i]));
						}
						/**
						 * To check the mapping with static value
						 */
					} else if (!parameterDataGMParameterValue[i].trim().equalsIgnoreCase("null")) {

						inputParameterValue.add(parameterDataGMParameterValue[i].trim());
						/**
						 * to check the mapping with response parameter
						 */
					} else if (!responseParameterValue[i].trim().equalsIgnoreCase("null")) {

						inputParameterValue.add(responseParameterValue[i].trim());
					} else {
						/**
						 * To check the mapping with input parameter value with input parameter map
						 */
						inputParameterValue.add(String.valueOf(apiMap.get(inputParameterName[i])).trim());
					}
					/**
					 * To add parameter in URL if its isHeaderParameterValue value of i is 1
					 */

				}
				/**
				 * To remove Unused Parameter and Add Parameter which will be used by Streaming
				 */

				apiMap.remove("input_parameters_client_parameter");
				apiMap.remove("input_parameters_client_parameter_value");
				apiMap.remove("input_parameters_regex");
				apiMap.remove("input_parameters_regex_description");
				apiMap.remove("input_parameters_is_required");
				apiMap.remove("input_parameters_length");
				apiMap.remove("input_parameters_data_type");
				apiMap.remove(IS_TOKEN_REQUIRED);
				apiMap.remove("api_globetouch_endnode");
				apiMap.remove("token_parameter");
				apiMap.remove(TOKEN_RESPONSE);
				apiMap.remove("input_parameters_api_group_id");
				apiMap.remove("response_api_url");
				apiMap.remove("response_parameter_name");
				apiMap.remove("response_parameter_api_id");
				apiMap.remove("response_parameter_api_group_id");
				apiMap.remove("respose_api_name");
				apiMap.remove("api_response_parameter_id");
				apiMap.remove("input_parameters_response_parameter_id");
				apiMap.remove("api_response_parameter_value");
				apiMap.remove(API_TOKEN_API);
				apiMap.put(TRACKING_MESSAGE_HEADER, requestParameter.get(TRACKING_MESSAGE_HEADER));
				apiMap.put("tmp_variable", "");
				apiMap.put("retry_no", 0);
				apiMap.put("user_name", request.getHeader("user_name"));
				apiMap.put("returnUrl", String.valueOf(requestParameter.get("returnUrl")));
				apiMap.put(REQUEST_ID, String.valueOf(requestParameter.get(REQUEST_ID)));

				/**
				 * To Check if inputParameterValue is not null
				 */

				if (!inputParameterValue.isEmpty()) {

					/**
					 * To add input_parameters_value in current API set
					 */
					apiMap.put("input_parameters_value",
							String.valueOf(inputParameterValue).replaceAll("\\[", "").replaceAll("\\]", ""));

					/**
					 * Audit Log Insert for Transformation done successfully
					 */
					auditLogInsert(String.valueOf(apiMap.get(TRACKING_MESSAGE_HEADER)),
							String.valueOf(apiMap.get(API_GROUP_ID)), String.valueOf(apiMap.get(API_ID)),
							Integer.parseInt(String.valueOf(environment.getProperty(LOG_SUCCESS_BIT))),
							"API urlPassingParameter is transformed",
							String.valueOf(environment.getProperty("log.status.transformation.success")),
							String.valueOf(apiMap.get("input_parameters_name")),
							String.valueOf(apiMap.get("input_parameters_value")), request);
				}

			} catch (Exception e) {
				/**
				 * To Print exception if it comes
				 */
				logger.setLevel(org.apache.log4j.Level.ERROR);

				logger.error(e);

				/**
				 * Audit Log Insert for Transformation not done.
				 */
				auditLogInsert(String.valueOf(tokenResponse.get(0).get(TRACKING_MESSAGE_HEADER)),
						String.valueOf(tokenResponse.get(0).get(API_GROUP_ID)), String.valueOf(apiMap.get(API_ID)),
						Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))),
						"Transformation Failure",
						String.valueOf(environment.getProperty("log.status.transformation.failure")),
						String.valueOf(ERROR_STRING), String.valueOf(e.getMessage()), request);
				return null;

			}
		}
		/**
		 * Return Statement
		 */
		return tokenResponse;
	}

	/**
	 * This Method is used to get the URL parameter of end nodes API to call it .The
	 * process this method do is as follow:
	 * 
	 * 1. Split the following parameters in String Array: *
	 * 
	 * i.input_parameters_client_parameter
	 * 
	 * ii.input_parameters_name
	 * 
	 * iii.input_parameters_client_parameter_value
	 * 
	 * iv.input_parameter_is_header
	 * 
	 * v.api_response_parameter_value
	 * 
	 * 
	 * 2.Add Header parameters in headerMAp
	 * 
	 * 3.For compiling body parameters it follow the following steps:
	 * 
	 * i.Compile the api_template using Pattern and Matcher class. ii.replace
	 * <parameterName> with its value on the basis of mapping defined in DB from one
	 * of this 2 MAPS:
	 * 
	 * a.requesParameterMap
	 * 
	 * b.staticParamMAp
	 * 
	 * ii.Storing whole transformed body parameters in StringBuider sb.
	 * 
	 * 
	 * 4.Add both headerMap and Body parameters in parameterMAp.
	 * 
	 * 5.returns parameterMAp.
	 * 
	 * 
	 * @param urlResponse        List of Group API
	 * @param requesParameterMap Map of input parameter
	 * @param request:::To       get HTTP Basic authentication,where consumer sends
	 *                           the ‘user_name’ and ‘password’ separated by ‘:’,
	 *                           within a base64 and requestId and returnURL from
	 *                           request header
	 * 
	 * @param response:::To      send response
	 * 
	 * @return Return the response message
	 * @throws Exception
	 */
	public Map<String, Object> getUrlParameters(Map<String, Object> apiMap, Map<String, String> requesParameterMap,
			HttpServletRequest request) throws Exception {
		Map<String, Object> parameterMAp = new LinkedHashMap<>();
		/**
		 * To get Each API set from Group
		 */

		try {
			StringBuffer sb = new StringBuffer();

			String[] inputParameterName = String.valueOf(apiMap.get("input_parameters_name")).split(",");
			String[] inputParameterValue = String.valueOf(apiMap.get("input_parameters_value")).split(",");
			String[] isHeaderParameterValue = String.valueOf(apiMap.get("input_parameter_is_header")).split(",");
			String[] isQueryParameterValue = String.valueOf(apiMap.get("input_parameters_is_query")).split(",");

			/**
			 * Initializing header Map to store the header parameter
			 */
			Map<String, String> headerMap = new LinkedHashMap<>();
			Map<String, String> bodyMap = new LinkedHashMap<>();

			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < inputParameterName.length; i++) {

				if (isHeaderParameterValue[i].trim().equalsIgnoreCase("1")) {
					headerMap.put(inputParameterName[i].trim(), inputParameterValue[i].trim());
				} else {
					bodyMap.put(inputParameterName[i].trim(), inputParameterValue[i].trim());
				}

			}
			for (int i = 0; i < isQueryParameterValue.length; i++) {
				if (isQueryParameterValue[i].trim().equalsIgnoreCase("1")) {
					if (!inputParameterValue[i].trim().equalsIgnoreCase("null")) {

						builder.append(String.valueOf(inputParameterName[i]).trim() + "="
								+ inputParameterValue[i].trim() + "&");
					} else if (!inputParameterName[i].trim().equalsIgnoreCase("null")) {

						builder.append(String.valueOf(inputParameterName[i]).trim()
								.concat("=" + String.valueOf(bodyMap.get(inputParameterName[i])).trim() + "&"));
					} else {
						if (bodyMap.get(inputParameterName[i]).trim().equalsIgnoreCase("null")) {
							builder.append(String.valueOf(inputParameterName[i]).trim() + "="
									+ String.valueOf(bodyMap.get(inputParameterName[i])).trim() + "&");
						}

					}
				}

			}

			if (builder.length() > 0) {
				String newUrl = String.valueOf(apiMap.get(API_URL));
				apiMap.remove(API_URL);
				builder.deleteCharAt(builder.lastIndexOf("&"));
				apiMap.put(API_URL, newUrl + "?" + builder.toString());
			}

			StringBuffer url = new StringBuffer();
			// if api have path parameters, then replace url with their values
			// Changes made as previously only one path parameter was getting replaced.
			if (String.valueOf(apiMap.get("input_parameters_is_path")).contains("1")) {
				Pattern pattern = Pattern.compile(TEMPLATE_PARAM_PATTERN);
				Matcher matcherUrl = pattern.matcher(String.valueOf(apiMap.get(API_URL)));
				while (matcherUrl.find()) {
					String matchCaseUrl = matcherUrl.group(0);
					String matchCaseValue = "";
					if (matchCaseUrl.replaceAll(REPLACE_PARAM_PATTERN, "").contains("#&#")) {
						matchCaseValue = String.valueOf(bodyMap.get(matchCaseUrl.replaceAll(REPLACE_PARAM_PATTERN, "")))
								.trim();

					}
					if (bodyMap.containsKey(matchCaseUrl.replaceAll(REPLACE_PARAM_PATTERN, ""))) {
						matchCaseValue = String.valueOf(bodyMap.get(matchCaseUrl.replaceAll(REPLACE_PARAM_PATTERN, "")))
								.trim();

					} else {
						matchCaseValue = "";

					}

					matcherUrl.appendReplacement(url, matchCaseValue.trim());

				}

				matcherUrl.appendTail(url);
				apiMap.put(API_URL, String.valueOf(url).trim());
				System.out.println("URL AFTER PATH PARAMETER CHANGES:" + String.valueOf(url).trim());
				parameterMAp.put(API_URL, apiMap.get(API_URL));
			}
			if (!String.valueOf(apiMap.get(API_TEMPLATE)).equalsIgnoreCase("null")) {

				if (String.valueOf(apiMap.get(API_TYPE)).equalsIgnoreCase("REST")) {
					Pattern pattern = Pattern.compile(TEMPLATE_PARAM_PATTERN);
					Matcher matcher = pattern.matcher(String.valueOf(apiMap.get(API_TEMPLATE)));

//					parameterMAp.put(API_URL, apiMap.get(API_URL));

					if (sb.length() == 0) {
						while (matcher.find()) {
							String matchCase = matcher.group(0);

							if (requesParameterMap.containsKey(matchCase.replaceAll(REPLACE_PARAM_PATTERN, ""))) {
								String matchCaseValue = String
										.valueOf(bodyMap.get(matchCase.replaceAll(REPLACE_PARAM_PATTERN, ""))).trim();
								matcher.appendReplacement(sb, matchCaseValue.trim());
							}

							/** STC SUSPEND SIM CASE RESOLVED BY EXTRA ELSE IF LOOP ***/

							else if (bodyMap.containsKey(matchCase.replaceAll(REPLACE_PARAM_PATTERN, ""))) {
								String matchCaseValue = String
										.valueOf(bodyMap.get(matchCase.replaceAll(REPLACE_PARAM_PATTERN, ""))).trim();
								matcher.appendReplacement(sb, matchCaseValue.trim());
							} else {
								String matchCaseValue = "";
								matcher.appendReplacement(sb, matchCaseValue.trim());
							}

						}
						matcher.appendTail(sb);

					}
				} else if (String.valueOf(apiMap.get(API_TYPE)).equalsIgnoreCase("SOAP")) {
					Pattern pattern = Pattern.compile("@@.+?@@");
					Matcher matcher = pattern.matcher(String.valueOf(apiMap.get(API_TEMPLATE)));

					if (sb.length() == 0) {
						while (matcher.find()) {
							String matchCase = matcher.group(0);

							if (requesParameterMap.containsKey(matchCase.replaceAll("[@@]", ""))) {
								String matchCaseValue = String.valueOf(bodyMap.get(matchCase.replaceAll("[@@]", "")))
										.trim();
								matcher.appendReplacement(sb, matchCaseValue.trim());
							} else {
								String matchCaseValue = "";
								matcher.appendReplacement(sb, matchCaseValue.trim());
							}
						}
						matcher.appendTail(sb);
					}
				}
				parameterMAp.put(BODY_PARAM, sb);
				parameterMAp.put(HEADER_PARAM, headerMap);
			} else {
				parameterMAp.put(BODY_PARAM, "");
				parameterMAp.put(HEADER_PARAM, headerMap);
			}

			/**
			 * Adding body_parameter and header_parameter in parameterMAp
			 */

			/**
			 * Insert Audit Log for
			 */
			auditLogInsert(String.valueOf(requesParameterMap.get(TRACKING_MESSAGE_HEADER)),
					String.valueOf(apiMap.get(API_GROUP_ID)), String.valueOf(apiMap.get(API_ID)),
					Integer.parseInt(String.valueOf(environment.getProperty(LOG_SUCCESS_BIT))), "SYNC API",
					String.valueOf(environment.getProperty("log.status.response.success")), "API Parameter",
					String.valueOf(parameterMAp), request);

		} catch (Exception e) {
			/**
			 * To Print Exception if it comes
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

			auditLogInsert(String.valueOf(requesParameterMap.get(TRACKING_MESSAGE_HEADER)),
					String.valueOf(apiMap.get(API_GROUP_ID)), String.valueOf(apiMap.get(API_ID)),
					Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))), "SYNC API",
					String.valueOf(environment.getProperty(LOG_FAILURE_RESPONSE_BIT)), "ERROR ",
					String.valueOf(e.getMessage()), request);
			return null;

		}
		logger.info(parameterMAp + "********************** parameterMAp");
		return parameterMAp;
	}

	/**
	 * it is recursive method which executed till all json object convert to
	 * hashmap.
	 * 
	 * @param json:Json String containing Json Object and Json Array
	 * @param map:It    hold all parameter and value which is extract from json
	 *                  variable
	 * @return:HashMap<String, String> map which havin all parameter and value
	 * 
	 * 
	 */
	public Map<String, String> responseParameterParse(String json, Map<String, String> map, String groupID,
			String apiId) {
		try {
			/**
			 * Initializing Json Factory
			 */
			JsonFactory factory = new JsonFactory();
			/**
			 * Initializing ObjectMapper
			 */
			ObjectMapper mapper = new ObjectMapper(factory);
			/**
			 * Initializing JsonNode
			 */
			JsonNode rootNode = mapper.readTree(json);
			/**
			 * Initializing Iterator
			 */
			Iterator<Map.Entry<String, JsonNode>> fieldsIterator = rootNode.getFields();
			/**
			 * Iterating Json till its last value
			 */
			while (fieldsIterator.hasNext()) {
				/**
				 * To get Response parameter value
				 */
				Map.Entry<String, JsonNode> field = fieldsIterator.next();

				/**
				 * To put response key and value in map
				 */
				map.put("#$#" + field.getKey() + "#&#" + apiId + "#&#" + groupID, String.valueOf(field.getValue()));
				/**
				 * Parsing Response as per their format
				 */
				if ((String.valueOf(field.getValue()).startsWith("{")
						&& String.valueOf(field.getValue()).endsWith("}"))) {

					responseParameterParse(String.valueOf(field.getValue()), map, groupID, apiId);

				}
				if ((String.valueOf(field.getValue()).startsWith("\"")
						&& String.valueOf(field.getValue()).endsWith("\""))) {

					responseParameterParse(String.valueOf(field.getValue()), map, groupID, apiId);

				}
				if (String.valueOf(field.getValue()).startsWith("[{")
						&& String.valueOf(field.getValue()).endsWith("}]")) {

					parseJsonArray(String.valueOf(field.getValue()), map, groupID, apiId);

				}
			}
		} catch (Exception e) {
			/**
			 * To Print Exception if it comes
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

		}
		/**
		 * Return Response Map
		 */
		return map;
	}

	/**
	 * parseJsonArray(String jsonArray,HashMap<String, String> map) :This Method is
	 * use to convert json object to map of parameter and value
	 * 
	 * @param jsonArray:String    of json Array
	 * @param map:HashMap<String, String> map havin all parameter and value extract
	 *                            form json
	 */
	public void parseJsonArray(String jsonArray, Map<String, String> map, String groupID, String apiId) {
		try {

			JSONArray jsonArray1 = new JSONArray(jsonArray);
			for (int i = 0; i < jsonArray1.length(); i++) {
				JSONObject json = jsonArray1.getJSONObject(i);
				Iterator<String> keys = json.keys();

				while (keys.hasNext()) {
					String key = keys.next();
					map.put("#$#" + key + "#&#" + apiId + "#&#" + groupID, String.valueOf(json.get(key)));

				}

			}
		} catch (Exception e) {
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

		}
	}

	/**
	 * This method is use to validate user this method will be called by interface
	 * API THis method will call the procedure of Database by passing user_name and
	 * password and if it gets success response than it will send success response
	 * else false response
	 * 
	 * 
	 * @param user_name     user name of the client which need to be validated
	 * @param password      password of the user defined in the Database
	 * @param ip            IP of the user from where the API being called
	 * @param request:::To  get HTTP Basic authentication,where consumer sends the
	 *                      ‘user_name’ and ‘password’ separated by ‘:’, within a
	 *                      base64 and requestId and returnURL from request header
	 * 
	 * @param response:::To send response
	 * 
	 * @return Return the response message
	 * @throws Exception
	 */

	public ResponseEntity<?> validateUser(String userName, String password, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		/**
		 * Initializing the response message
		 */
		OrchestrationMessage responseMessage = new OrchestrationMessage();
		try {
			/**
			 * Parameter Map with the required parameter to call the API
			 */
			Map<String, String> map = new LinkedHashMap<>();
			map.put("user_name", userName);
			map.put("password", password);
			map.put("ip", request.getRemoteHost());
			/**
			 * Calling GenericOrchestrationProcedureCalling Method to call the procedure to
			 * validate the user.
			 */
			OrchestrationMessage message = orchestrationGenericProcess.GenericOrchestrationProcedureCalling("4", map,
					null, request, response);
			/**
			 * To response is Valid
			 */
			if (message.isValid()) {
				/**
				 * Casting response in List<Map<String, Object>> format.
				 */
				List<Map<String, Object>> formattedList = (List<Map<String, Object>>) message.getObject();

				/**
				 * TO check if valid bit of user is 1
				 */
				if (formattedList.get(0).get("is_valid").toString().equalsIgnoreCase("1")) {
					/**
					 * To send response of Method
					 */
					responseMessage.setObject(formattedList.get(0));

					return new ResponseEntity<>(responseMessage.getObject(), HttpStatus.ACCEPTED);
				} else {
					/**
					 * To send response of Method
					 */

					return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
				}
			} else {
				/**
				 * To send response of Method
				 */

				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			}
		} catch (Exception e) {
			/**
			 * To send response of Method
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}

	}

	/**
	 * 
	 * This method is used to store all the custom error code of OL when the
	 * APllication of interface is being deployed.This method will call only when
	 * custom error codes in the Constant class is null and store all the error code
	 * by calling the DB.
	 * 
	 * @param request:::To  get HTTP Basic authentication,where consumer sends the
	 *                      ‘user_name’ and ‘password’ separated by ‘:’, within a
	 *                      base64 and requestId and returnURL from request header
	 * 
	 * @param response:::To send response
	 * 
	 * @return Return the response message
	 * @throws Exception
	 */
	public Map<String, Object> getErrorCodes(String groupID, HttpServletRequest request, HttpServletResponse response) {

		OrchestrationMessage message;
		Map<String, Object> errorMap = new LinkedHashMap<>();
		try {

			Map<String, String> passingMap = new LinkedHashMap<>();

			if (groupID.contains(",")) {
				String newGroupId = groupID.substring(0, groupID.indexOf("|#@|"));
				passingMap.put(API_GROUP_ID, String.valueOf(newGroupId));
			} else {
				passingMap.put(API_GROUP_ID, String.valueOf(groupID));
			}

			// Calling GenericOrchestrationProcedureCalling Method to call the procedure to
			// get error codes by API_GROUP_ID
			message = orchestrationGenericProcess.GenericOrchestrationProcedureCalling("9", passingMap, null, request,
					response);

			// checking storedProcedure response data is valid or not
			if (message.isValid()) {

				// Casting response in List<Map<String, Object>> format.
				List<Map<String, Object>> formattedList = (List<Map<String, Object>>) message.getObject();
				if (!formattedList.isEmpty()) {
					// storing error codes in map, here key is priority
					for (Map<String, Object> map : formattedList) {
						errorMap.put(map.get(PRIORITY).toString(), map);
					}

				}
			}
		} catch (Exception e) {
			/**
			 * To Print if Exception Occurs
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

		}
		return errorMap;

	}

	/**
	 * API to get Logs application wise on the basis of their user_name between the
	 * given dates
	 * 
	 * @param start_time    Contains start_time
	 * @param end_time      Contains end_time
	 * 
	 * @param request:::To  get HTTP Basic authentication,where consumer sends the
	 *                      ‘user_name’ and ‘password’ separated by ‘:’, within a
	 *                      base64 and requestId and returnURL from request header
	 * 
	 * @param response:::To send response
	 * 
	 * @return Return the response message
	 * @throws Exception
	 */

	public ResponseEntity<?> auditLogByUser(String userName, String startTime, String endTime,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		/**
		 * Initializing the response message
		 */
		Map<String, Object> errorMap = getErrorCodes("0", request, response);
		OrchestrationMessage responseMessage = new OrchestrationMessage();
		try {
			/**
			 * Parameter Map with the required parameter to call the API
			 */
			Map<String, String> map = new LinkedHashMap<>();
			map.put("user_name", userName);
			map.put("start_time", startTime);
			map.put("end_time", endTime);
			/**
			 * Calling GenericOrchestrationProcedureCalling Method to call the procedure to
			 * validate the user.
			 */
			OrchestrationMessage message = orchestrationGenericProcess.GenericOrchestrationProcedureCalling("7", map,
					null, request, response);
			/**
			 * To check response is Valid
			 */
			if (message.isValid()) {
				/**
				 * Casting response in List<Map<String, Object>> format.
				 */
				List<Map<String, Object>> formattedList = (List<Map<String, Object>>) message.getObject();
				/**
				 * To send response of Method
				 */

				return new ResponseEntity<>(formattedList, HttpStatus.ACCEPTED);

			} else {
				/**
				 * To send response of Method
				 */
				Map<String, Object> errorMessageMap = (Map<String, Object>) errorMap.get("3");

				LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
				errorMessage.remove(PRIORITY);
				errorMessage.put("code", errorMessage.get("code"));
				errorMessage.put(DESCRIPTION, errorMessage.get(DESCRIPTION).toString().concat(":" + PROCESS_FAIL));
				List<Map<String, Object>> errorList = new LinkedList<>();
				errorList.add(errorMessage);
				Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
				finalErrorMessageMap.put(ERROR, errorList);
				return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (Exception e) {
			/**
			 * To send response of Method
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

			responseMessage.setDescription(PROCESS_FAIL + e.getMessage());

			responseMessage.setValid(false);
			Map<String, Object> errorMessageMap = (Map<String, Object>) errorMap.get("3");

			LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
			errorMessage.put("code", errorMessage.get("code"));
			errorMessage.remove(PRIORITY);
			errorMessage.put(DESCRIPTION,
					errorMessage.get(DESCRIPTION).toString().concat(":" + PROCESS_FAIL + e.getMessage()));
			List<Map<String, Object>> errorList = new LinkedList<>();
			errorList.add(errorMessage);
			Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
			finalErrorMessageMap.put(ERROR, errorList);
			return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * API to get Logs log_id wise
	 * 
	 * @param log_id        Contains log_id to gets its all logs
	 * @param request:::To  get HTTP Basic authentication,where consumer sends the
	 *                      ‘user_name’ and ‘password’ separated by ‘:’, within a
	 *                      base64 and requestId and returnURL from request header
	 * 
	 * @param response:::To send response
	 * 
	 * @return Return the response message
	 * @throws Exception
	 */

	public ResponseEntity<?> auditLogById(String trackingMessageHeader, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		/**
		 * Initializing the response message
		 */
		Map<String, Object> errorMap = getErrorCodes("0", request, response);
		OrchestrationMessage responseMessage = new OrchestrationMessage();
		try {
			/**
			 * Parameter Map with the required parameter to call the API
			 */
			Map<String, String> map = new LinkedHashMap<>();
			map.put(TRACKING_MESSAGE_HEADER, trackingMessageHeader);
			/**
			 * Calling GenericOrchestrationProcedureCalling Method to call the procedure to
			 * validate the user.
			 */
			OrchestrationMessage message = orchestrationGenericProcess.GenericOrchestrationProcedureCalling("8", map,
					null, request, response);
			/**
			 * To response is Valid
			 */
			if (message.isValid()) {
				/**
				 * Casting response in List<Map<String, Object>> format.
				 */
				List<Map<String, Object>> formattedList = (List<Map<String, Object>>) message.getObject();

				/**
				 * To send response of Method
				 */
				return new ResponseEntity<>(formattedList, HttpStatus.ACCEPTED);

			} else {
				/**
				 * To send response of Method
				 */
				Map<String, Object> errorMessageMap = (Map<String, Object>) errorMap.get("3");

				LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
				errorMessage.put("code", errorMessage.get("code"));
				errorMessage.remove(PRIORITY);
				errorMessage.put(DESCRIPTION, errorMessage.get(DESCRIPTION).toString().concat(":" + PROCESS_FAIL));
				List<Map<String, Object>> errorList = new LinkedList<>();
				errorList.add(errorMessage);
				Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
				finalErrorMessageMap.put(ERROR, errorList);
				return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);

			}
		} catch (Exception e) {
			/**
			 * To send response of Method
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

			responseMessage.setDescription(PROCESS_FAIL + e.getMessage());

			Map<String, Object> errorMessageMap = (Map<String, Object>) errorMap.get("3");

			LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
			errorMessage.put("code", errorMessage.get("code"));
			errorMessage.remove(PRIORITY);
			errorMessage.put(DESCRIPTION,
					errorMessage.get(DESCRIPTION).toString().concat(":" + PROCESS_FAIL + e.getMessage()));
			List<Map<String, Object>> errorList = new LinkedList<>();
			errorList.add(errorMessage);
			Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
			finalErrorMessageMap.put(ERROR, errorList);
			return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);

		}

	}

	/**
	 * 
	 * This method is used to store all the custom error code of OL when the
	 * APllication of interface is being deployed.This method will call only when
	 * custom error codes in the Constant class is null and store all the error code
	 * by calling the DB.
	 * 
	 * @param request:::To  get HTTP Basic authentication,where consumer sends the
	 *                      ‘user_name’ and ‘password’ separated by ‘:’, within a
	 *                      base64 and requestId and returnURL from request header
	 * 
	 * @param response:::To send response
	 * 
	 * @return Return the response message
	 * @throws Exception
	 */

	public List<Map<String, Object>> getNotificationTemplate(String groupID, HttpServletRequest request,
			HttpServletResponse response) {
		/**
		 * To check if Error Map in Constant class is empty
		 */

		/**
		 * initialize response message
		 */
		OrchestrationMessage message;
		List<Map<String, Object>> responseTemplateMap = new LinkedList<>();
		try {
			/**
			 * Calling GenericOrchestrationProcedureCalling Method to call the procedure to
			 * validate the user.
			 */
			Map<String, String> passinMap = new LinkedHashMap<>();

			passinMap.put(API_GROUP_ID, String.valueOf(groupID));
			message = orchestrationGenericProcess.GenericOrchestrationProcedureCalling("10", passinMap, null, request,
					response);
			/**
			 * To check response is Valid
			 */

			if (message.isValid()) {

				/**
				 * Casting response in List<Map<String, Object>> format.
				 */
				responseTemplateMap = (List<Map<String, Object>>) message.getObject();

			}
		} catch (Exception e) {
			/**
			 * To Print if Exception Occurs
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

		}
		return responseTemplateMap;

	}

	public ResponseEntity<?> checkRequestId(String groupID, Map<String, String> parameterMap,
			HttpServletRequest request, HttpServletResponse response) {

		Map<String, Object> errorMAp = getErrorCodes(groupID, request, response);
		try {
			OrchestrationMessage requestIdValidation = orchestrationGenericProcess
					.GenericOrchestrationProcedureCalling("12", parameterMap, null, request, response);

			List<Map<String, Object>> requestIdValidationResponse = (List<Map<String, Object>>) requestIdValidation
					.getObject();

			if (requestIdValidationResponse.get(0).get("is_exists").toString().equalsIgnoreCase("1")) {
				Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("3");

				LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
				errorMessage.remove(PRIORITY);
				errorMessage.put("code", errorMessage.get("code"));
				errorMessage.put(DESCRIPTION,
						errorMessage.get(DESCRIPTION).toString().concat(":" + "Request Already Exists"));
				List<Map<String, Object>> errorList = new LinkedList<>();
				errorList.add(errorMessage);
				Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
				finalErrorMessageMap.put(ERROR, errorList);
				return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);
			} else {
				return new ResponseEntity<>(HttpStatus.ACCEPTED);
			}
		} catch (Exception e) {
			/**
			 * To Print if Exception Occurs
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	public ResponseEntity<?> getOperatorConfig(Map<String, String> parameterMap, HttpServletRequest request,
			HttpServletResponse response) {
		/*
		 * Initialization of response message
		 */

		Map<String, Object> responseMap = new LinkedHashMap<>();

		Map<String, String> requestRouterResponseMap = new LinkedHashMap<>();
		Map<String, Object> errorMAp = getErrorCodes(environment.getProperty(ROUTER_CONFIG_NAME), request, response);
		try {

			parameterMap.put(API_GROUP_ID, String.valueOf(environment.getProperty(ROUTER_CONFIG_NAME)));

			OrchestrationMessage getOperatorGroup = orchestrationGenericProcess
					.GenericOrchestrationProcedureCalling("13", parameterMap, null, request, response);
			responseMap.put(TRACKING_MESSAGE_HEADER, String.valueOf(parameterMap.get(TRACKING_MESSAGE_HEADER)));

			List<Map<String, Object>> getOperatorResponse = (List<Map<String, Object>>) getOperatorGroup.getObject();
			if (!getOperatorResponse.isEmpty()) {

				ResponseEntity<?> requestRouterResponse = getSyncRequestData(getOperatorResponse, parameterMap,
						environment.getProperty(ROUTER_CONFIG_NAME), errorMAp, request, response);
				if (requestRouterResponse.getStatusCode().is2xxSuccessful()) {

					JsonOlcoreModification.parseOL(requestRouterResponse.getBody().toString(),
							requestRouterResponseMap);

					OrchestrationMessage getPlatform = orchestrationGenericProcess.GenericOrchestrationProcedureCalling(
							"14", requestRouterResponseMap, null, request, response);

					if (getPlatform.isValid()) {
						List<Map<String, String>> getPlatformResponse = (List<Map<String, String>>) getPlatform
								.getObject();
						requestRouterResponseMap.putAll(getPlatformResponse.get(0));

						logger.info("Response from core :" + requestRouterResponseMap);
						return new ResponseEntity<>(new Gson().toJson(requestRouterResponseMap), HttpStatus.ACCEPTED);
					} else {
						Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("1");

						LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
						errorMessage.remove(PRIORITY);
						errorMessage.put("code", errorMessage.get("code"));

						List<Map<String, Object>> errorList = new LinkedList<>();
						errorList.add(errorMessage);
						Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
						finalErrorMessageMap.put(ERROR, errorList);
						return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.BAD_REQUEST);
					}

				} else {
					return requestRouterResponse;
				}

			} else {
				logger.info("No Data for Request Data from Database Configuration");
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}

		} catch (Exception e) {
			/**
			 * To Print if Exception Occurs
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	public ResponseEntity<?> genericApiFailureNotifactionMethod(Map<String, String> map,
			Map<String, String> parameterMap, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		/**
		 * Initializing the response message
		 */
		Map<String, Object> errorMap = getErrorCodes(groupName, request, response);
		OrchestrationMessage responseMessage = new OrchestrationMessage();
		try {

			/**
			 * Casting response in List<Map<String, Object>> format.
			 */
			StringBuffer sb = new StringBuffer();

			Pattern pattern = Pattern.compile(TEMPLATE_PARAM_PATTERN);
			Matcher matcher = pattern.matcher(String.valueOf(map.get(NOTIFICATION_TEMPLATE)));

			if (sb.length() == 0) {
				while (matcher.find()) {
					String matchCase = matcher.group(0);

					if (parameterMap.containsKey(matchCase.replaceAll(REPLACE_PARAM_PATTERN, ""))) {
						String matchCaseValue = String
								.valueOf(parameterMap.get(matchCase.replaceAll(REPLACE_PARAM_PATTERN, "")));
						matcher.appendReplacement(sb, matchCaseValue);
					}

				}
				matcher.appendTail(sb);
			}
			Type type = new TypeToken<List<Map<String, Object>>>() {
			}.getType();

			List<Map<String, Object>> transformedDataList = new Gson().fromJson(sb.toString(), type);
			/**
			 * Calling Method to Push Data in Kafka queue
			 */
			Boolean kafkaStatus = executeNotificationtoKafka(transformedDataList, "notification_publisher", request,
					response);
			/**
			 * To Check kafkaStatus.It will be true if data pushed successfully in kafka
			 * else it will be false
			 */
			if (kafkaStatus) {

				/**
				 * Response Of API
				 */
				responseMessage.setDescription("Your Request has succesfully Registered and forwarded for Processing");
				responseMessage.setObject(kafkaStatus);
				responseMessage.setValid(true);
				return new ResponseEntity<>(HttpStatus.ACCEPTED);
			} else {
				/**
				 * Response Of API
				 */
				Map<String, Object> errorMessageMap = (Map<String, Object>) errorMap.get("3");

				LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
				errorMessage.remove(PRIORITY);
				errorMessage.put("code", errorMessage.get("code"));
				errorMessage.put(DESCRIPTION, errorMessage.get(DESCRIPTION).toString()
						.concat(":" + "Kafka issue Encountered... Unable to process the request. " + kafkaStatus));

				List<Map<String, Object>> errorList = new LinkedList<>();
				errorList.add(errorMessage);
				Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
				finalErrorMessageMap.put(ERROR, errorList);
				return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);

			}

		} catch (Exception e) {
			/**
			 * To send response of Method
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

			responseMessage.setDescription(PROCESS_FAIL + e.getMessage());

			Map<String, Object> errorMessageMap = (Map<String, Object>) errorMap.get("3");

			LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
			errorMessage.put("code", errorMessage.get("code"));
			errorMessage.remove(PRIORITY);
			errorMessage.put(DESCRIPTION,
					errorMessage.get(DESCRIPTION).toString().concat(":" + PROCESS_FAIL + e.getMessage()));
			List<Map<String, Object>> errorList = new LinkedList<>();
			errorList.add(errorMessage);
			Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
			finalErrorMessageMap.put(ERROR, errorList);
			return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);

		}

	}

	public ResponseEntity<?> genericNotificationMethod(String groupID, String platformName, String responseCode,
			Map<String, String> parameterMap, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		/*
		 * Initialization of response message
		 */
		OrchestrationMessage responseMessage = new OrchestrationMessage();
		Map<String, Object> responseMap = new LinkedHashMap<>();
		/**
		 * To get Custom error code as per API group.
		 */
		Map<String, Object> errorMap = getErrorCodes(groupID, request, response);
		Map<String, String> inputParameterMap = new LinkedHashMap<>();
		try {

			/**
			 * To Check if tracking_message_header is not Empty
			 */

			inputParameterMap.putAll(parameterMap);
			responseMap.put(TRACKING_MESSAGE_HEADER, String.valueOf(inputParameterMap.get(TRACKING_MESSAGE_HEADER)));

			/**
			 * Adding group id to parameter from configuration defined in
			 * ApiConfiguration.properties
			 */
			inputParameterMap.put("api_group_name", String.valueOf(groupID));
			inputParameterMap.put("platform_name", String.valueOf(platformName));
			inputParameterMap.put(RESPONSE_CODE, String.valueOf(responseCode));

			/**
			 * Calling Procedure to get set of API which need to be validated ,transformed
			 * and forwarded to kafka queue
			 */

			OrchestrationMessage getUrlsMessage = orchestrationGenericProcess.GenericOrchestrationProcedureCalling("15",
					inputParameterMap, null, request, response);

			/**
			 * To check if response is valid
			 */
			if (getUrlsMessage.isValid()) {
				/**
				 * Casting response in List<Map<String, Object>> format
				 */
				List<Map<String, Object>> urlResponse = (List<Map<String, Object>>) getUrlsMessage.getObject();

				if (!urlResponse.isEmpty()) {
					if (urlResponse.get(0).containsKey(NOTIFICATION_TEMPLATE)) {
						/**
						 * Casting response in List<Map<String, Object>> format.
						 */
						StringBuffer sb = new StringBuffer();

						Pattern pattern = Pattern.compile(TEMPLATE_PARAM_PATTERN);
						Matcher matcher = pattern
								.matcher(String.valueOf(urlResponse.get(0).get(NOTIFICATION_TEMPLATE)));

						if (sb.length() == 0) {
							while (matcher.find()) {
								String matchCase = matcher.group(0);

								if (parameterMap.containsKey(matchCase.replaceAll(REPLACE_PARAM_PATTERN, ""))) {
									String matchCaseValue = String
											.valueOf(parameterMap.get(matchCase.replaceAll(REPLACE_PARAM_PATTERN, "")));
									matcher.appendReplacement(sb, matchCaseValue);
								}

							}
							matcher.appendTail(sb);
						}
						Type type = new TypeToken<List<Map<String, Object>>>() {
						}.getType();

						List<Map<String, Object>> transformedDataList = new Gson().fromJson(sb.toString(), type);
						/**
						 * Calling Method to Push Data in Kafka queue
						 */
						Boolean kafkaStatus = executeNotificationtoKafka(transformedDataList, "notification_publisher",
								request, response);
						/**
						 * To Check kafkaStatus.It will be true if data pushed successfully in kafka
						 * else it will be false
						 */
						if (kafkaStatus) {

							/**
							 * Response Of API
							 */
							responseMessage.setDescription(
									"Your Request has succesfully Registered and forwarded for Processing");
							responseMessage.setObject(kafkaStatus);
							responseMessage.setValid(true);
							return new ResponseEntity<>(HttpStatus.ACCEPTED);
						} else {
							/**
							 * Response Of API
							 */
							Map<String, Object> errorMessageMap = (Map<String, Object>) errorMap.get("3");

							LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
							errorMessage.remove(PRIORITY);
							errorMessage.put("code", errorMessage.get("code"));
							errorMessage.put(DESCRIPTION, errorMessage.get(DESCRIPTION).toString().concat(
									":" + "Kafka issue Encountered... Unable to process the request. " + kafkaStatus));

							List<Map<String, Object>> errorList = new LinkedList<>();
							errorList.add(errorMessage);
							Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
							finalErrorMessageMap.put(ERROR, errorList);
							return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);

						}

					} else {
						/**
						 * To send response of Method
						 */
						Map<String, Object> errorMessageMap = (Map<String, Object>) errorMap.get("3");

						LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
						errorMessage.put("code", errorMessage.get("code"));
						errorMessage.remove(PRIORITY);
						errorMessage.put(DESCRIPTION, errorMessage.get(DESCRIPTION).toString());
						List<Map<String, Object>> errorList = new LinkedList<>();
						errorList.add(errorMessage);
						Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
						finalErrorMessageMap.put(ERROR, errorList);
						return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);

					}

				} else {
					/**
					 * Response Of API
					 */
					auditLogInsert(String.valueOf(inputParameterMap.get(TRACKING_MESSAGE_HEADER)), groupLogID, null,
							Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))),
							"Invalid Notification", String.valueOf(environment.getProperty(LOG_FAILURE_RESPONSE_BIT)),
							"Invalid Notification",
							String.valueOf(responseMap).replaceAll(",", "|:").replaceAll("\'", DATABASE_DELIMETER),
							request);

					Map<String, Object> errorMessageMap = (Map<String, Object>) errorMap.get("1");

					LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
					errorMessage.remove(PRIORITY);
					errorMessage.put("code", errorMessage.get("code"));
					errorMessage.put(DESCRIPTION, errorMessage.get(DESCRIPTION));

					List<Map<String, Object>> errorList = new LinkedList<>();
					errorList.add(errorMessage);
					Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
					finalErrorMessageMap.put(ERROR, errorList);
					return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.BAD_REQUEST);
				}

			} else {
				/**
				 * Response Of API
				 */
				auditLogInsert(String.valueOf(inputParameterMap.get(TRACKING_MESSAGE_HEADER)), groupLogID, null,
						Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))),
						"Internal Issue Occured From Database ",
						String.valueOf(environment.getProperty(LOG_FAILURE_RESPONSE_BIT)), ERROR_STRING,
						String.valueOf(responseMap).replaceAll(",", "|:").replaceAll("\'", DATABASE_DELIMETER),
						request);

				Map<String, Object> errorMessageMap = (Map<String, Object>) errorMap.get("3");

				LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
				errorMessage.remove(PRIORITY);
				errorMessage.put("code", errorMessage.get("code"));
				errorMessage.put(DESCRIPTION,
						errorMessage.get(DESCRIPTION).toString().concat(":" + PROCESS_FAIL + responseMap.toString()));
				List<Map<String, Object>> errorList = new LinkedList<>();
				errorList.add(errorMessage);
				Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
				finalErrorMessageMap.put(ERROR, errorList);
				return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);

			}

		} catch (

		Exception e) {
			/**
			 * To print the exception if it comes and return exception
			 */
			logger.setLevel(org.apache.log4j.Level.ERROR);

			logger.error(e);

			auditLogInsert(inputParameterMap.get(TRACKING_MESSAGE_HEADER),
					String.valueOf(environment.getProperty("group.id.auth")), null,
					Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))),
					"Internal Issue encoutered  ", String.valueOf(environment.getProperty(LOG_KAFKA_FAILURE_STATUS)),
					TEMPLATE, String.valueOf(e.getMessage()).replaceAll(",", "|:").replaceAll("\'", DATABASE_DELIMETER),
					request);
			Map<String, Object> errorMessageMap = (Map<String, Object>) errorMap.get("3");

			LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
			errorMessage.remove(PRIORITY);
			errorMessage.put("code", errorMessage.get("code"));
			errorMessage.put(DESCRIPTION,
					errorMessage.get(DESCRIPTION).toString().concat(":" + PROCESS_FAIL + responseMap.toString()));
			List<Map<String, Object>> errorList = new LinkedList<>();
			errorList.add(errorMessage);
			Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
			finalErrorMessageMap.put(ERROR, errorList);
			return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);

		}
	}

	private ResponseEntity<?> genericApiResponseMethodNew(String endNodeResponse, Map<String, Object> apiResponse,
			Map<String, String> map, HttpServletResponse response) {

		Map<String, String> httpStatusCodesMap = httpStatusParameter.getHttpCodeMap();
		List<Map<String, Object>> finalResponseList = new LinkedList<>();
		try {

			if (String.valueOf(map.get(NOTIFICATION_TEMPLATE)).equalsIgnoreCase("null")) {
				StringBuffer sb = new StringBuffer();
				if (String.valueOf(map.get("set_response_key")).equalsIgnoreCase("null")
						&& String.valueOf(map.get("get_response_key")).equalsIgnoreCase("null")) {
					Pattern pattern = Pattern.compile(TEMPLATE_PARAM_PATTERN);
					Matcher matcher = pattern.matcher(String.valueOf(map.get(NOTIFICATION_TEMPLATE)));
					Map<String, String> requesParameterMap = new LinkedHashMap<>();
					JsonOlcoreModification.parseOL(endNodeResponse, requesParameterMap);
					while (matcher.find()) {
						String matchCase = matcher.group(0);

						if (matchCase.replaceAll(REPLACE_PARAM_PATTERN, "").contains("#&#")) {
							String matchCaseValue = String
									.valueOf(requesParameterMap.get(matchCase.replaceAll(REPLACE_PARAM_PATTERN, "")));
							matcher.appendReplacement(sb, matchCaseValue);
						}
					}
					matcher.appendTail(sb);

				}
				return new ResponseEntity<>(sb, HttpStatus.ACCEPTED);
			} else {

				Type listType = new TypeToken<Map<String, Object>>() {
				}.getType();

				if (String.valueOf(map.get(NOTIFICATION_TEMPLATE)).equals("{}")) {
					return new ResponseEntity<>("{}",
							HttpStatus.valueOf(httpStatusCodesMap.get(map.get("response_http_code"))));
				}

				Map<String, Object> templateMapParam = new Gson()
						.fromJson(String.valueOf(map.get(NOTIFICATION_TEMPLATE)), listType);

				List<Map<String, Object>> responseManipulatedList = null;
				if (templateMapParam.get(String.valueOf(map.get("set_response_key"))) != null) {
					responseManipulatedList = (List<Map<String, Object>>) templateMapParam
							.get(String.valueOf(map.get("set_response_key")));
				} else {
					responseManipulatedList = new ArrayList<Map<String, Object>>();
					responseManipulatedList.add(templateMapParam);
				}

				logger.info("Response From End Node " + endNodeResponse);

				List<Map<String, Object>> responseListToManipulate = null;
				if (apiResponse.get(String.valueOf(map.get("get_response_key"))) != null) {
					responseListToManipulate = (List<Map<String, Object>>) apiResponse
							.get(String.valueOf(map.get("get_response_key")));

					apiResponse.get(String.valueOf(map.get("get_response_key")));

				}

				/*
				 * if (apiResponse.get(String.valueOf(map.get("get_response_key"))) != null) {
				 * 
				 * //Code Commented as get_response_key will not be used to get data of a
				 * particular key beacuse we have Iteration over response now. //Response at any
				 * key can be get using notification template.
				 * 
				 * 
				 *//**
					 * responseListToManipulate = (List<Map<String, Object>>) apiResponse
					 * .get(String.valueOf(map.get("get_response_key")));
					 */
				/*
				
				*//**
					 * we will have use get_response_key as a map now. It will keep key value pair
					 * in map where key will be the original key and value will be the key at which
					 * response is coming.
					 * 
					 *//*
						 * getResponseKeyMap= (LinkedHashMap<String, String>) new
						 * ObjectMapper().readValue(String.valueOf(map.get("get_response_key")),
						 * Map.class);
						 * 
						 * 
						 * }
						 */

				else {
					responseListToManipulate = new ArrayList<Map<String, Object>>();
					responseListToManipulate.add(apiResponse);

				}

				if (responseListToManipulate != null && !responseListToManipulate.isEmpty()) {
					finalResponseList = setIntoTemplate(responseManipulatedList, responseListToManipulate);
				}

				/**
				 * Returning Response after adding finalResponseList in apiResponse
				 */
				apiResponse = new LinkedHashMap<>();

				if (String.valueOf(map.get("set_response_key")).equals("null")) {
					apiResponse = finalResponseList.get(0);
				} else {
					apiResponse.put(String.valueOf(map.get("set_response_key")), finalResponseList);
				}

				logger.info("Response after manipulation and modification from ol core" + apiResponse);

				return new ResponseEntity<>(apiResponse,
						HttpStatus.valueOf(httpStatusCodesMap.get(map.get("response_http_code"))));
				// return new ResponseEntity<>(new
				// ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(apiResponse),
				// HttpStatus.valueOf(httpStatusCodesMap.get(map.get("response_http_code"))));
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.info(e);
			// need to change
			return new ResponseEntity<>(new LinkedHashMap<>(), HttpStatus.ACCEPTED);
		}

	}

	public static List<Map<String, Object>> setIntoTemplate(List<Map<String, Object>> responseManipulatedList,
			List<Map<String, Object>> responseListToManipulate) {

		List<Map<String, Object>> finalResponseList = new LinkedList<>();
		if (responseListToManipulate != null && !responseListToManipulate.isEmpty()) {

			for (Map<String, Object> map2 : responseListToManipulate) {

				/**
				 * Map to store response Parameter
				 */
				Map<String, Object> responseParameterMap = new LinkedHashMap<>();
				/**
				 * Too add all template parameter
				 */
				responseParameterMap.putAll(responseManipulatedList.get(0));
				/**
				 * responseManipulatedList Parameter to get expected Response
				 */

				responseParameterMap = fitIntoTemplate(responseManipulatedList, responseListToManipulate, map2,
						responseParameterMap);

				finalResponseList.add(responseParameterMap);

			}

		}
		return finalResponseList;

	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> fitIntoTemplate(List<Map<String, Object>> responseManipulatedList,
			List<Map<String, Object>> responseListToManipulate, Map<String, Object> map2,
			Map<String, Object> responseParameterMap) {
		System.out.println("Template List::" + responseManipulatedList);

		for (String mapw : responseManipulatedList.get(0).keySet()) {

			Pattern pattern = Pattern.compile(TEMPLATE_PARAM_PATTERN);
			Matcher matcherUrl = pattern.matcher(String.valueOf(responseManipulatedList.get(0).get(mapw)));
			if (matcherUrl.find()) {
				String value = responseManipulatedList.get(0).get(mapw).toString().replaceAll("[<>]", "");

				if (isArray(value)) {
					String originalKey = "";
					if (!map2.containsKey(mapw)) {
						originalKey = getResponseKeyMap.get(mapw);
					}

					else {
						originalKey = mapw;
					}

					Object itemValue = map2.get(originalKey);

					Type type = new TypeToken<List<Map<String, Object>>>() {
					}.getType();

					List<Map<String, Object>> al = new Gson().fromJson(itemValue.toString(), type);
					ArrayList<Map<String, Object>> responseManipulatedListNew = new ArrayList<>();
					responseManipulatedListNew = (ArrayList<Map<String, Object>>) responseManipulatedList.get(0)
							.get(mapw);
					// Object
					// responseManipulatedListObject=responseManipulatedList.get(0).get(mapw);
					// ArrayList<Map<String, Object>> responseManipulatedListNew =new
					// Gson().fromJson(responseManipulatedListObject.toString(), type);

					List<Map<String, Object>> modifiedListOld = new ArrayList<>();

					// This for loop will set data node into template and add result in list

					for (Map<String, Object> dataNode : al) {

						List<Map<String, Object>> modifiedListNew = new ArrayList<>();
						Map<String, Object> responseObjectMap = new LinkedHashMap<>();
						Map<String, Object> responseObjectMapNew = new LinkedHashMap<>();
						modifiedListNew = generateModifiedList(responseManipulatedListNew, dataNode, responseObjectMap,
								responseObjectMapNew, modifiedListNew, modifiedListOld);
						modifiedListOld.add(modifiedListNew.get(0));

					}

					responseParameterMap.put(mapw, modifiedListOld);

				} else {

					/**
					 * if (value.contains(",") ) {
					 * 
					 * String[] newValue = value.split(","); for (String string : newValue) {
					 * 
					 * if (map2.containsKey(string.trim())) {
					 * finalValue.append(map2.get(string.trim()) + " "); } else {
					 * finalValue.append(""); } responseParameterMap.put(mapw,
					 * String.valueOf(finalValue).trim()); }
					 * 
					 * }
					 * 
					 */
					if (value.contains(",") && value.substring(0, 1).contains("{")) {

						List<Map<String, Object>> templateManipultedList = null;
						Object obj = responseManipulatedList.get(0).get(mapw);
						ArrayList<Map<String, Object>> al = new ArrayList<>();
						al.add((Map<String, Object>) obj);
						templateManipultedList = al;
						String mapwValue = "";
						if (!map2.containsKey(mapw)) {
							String keyValue = getResponseKeyMap.get(mapw);
							mapwValue = map2.get(keyValue).toString();
							HashMap<String, String> mapNew = new HashMap<>();
							JsonOlcoreModification.parseOL(mapwValue, mapNew);
							map2.put(keyValue, mapNew);
						}

						Map<String, Object> responseParameterMap1 = (Map<String, Object>) responseManipulatedList.get(0)
								.get(mapw);
						responseParameterMap1 = fitIntoTemplate(templateManipultedList, responseListToManipulate, map2,
								responseParameterMap1);
						responseParameterMap.put(mapw, responseParameterMap1);

					} else if (map2.containsKey(value)) {

						responseParameterMap.put(mapw, map2.get(value));
					} else {
						// responseParameterMap.put(mapw, ""); // previously only single statement in
						// this else block

						String valueData = responseManipulatedList.get(0).get(mapw).toString();
						List<String> matchList = new ArrayList<>();
						Pattern regex = Pattern.compile("<(.*?)>"); // pattern to metch content between <>
						Matcher regexMatcher = regex.matcher(valueData);

						while (regexMatcher.find()) {// Finds Matching Pattern in String
							matchList.add(regexMatcher.group(1));// Fetching Group from String
						}

						if (!matchList.isEmpty()) {
							for (String keyName : matchList) {
								if (map2.get(keyName) != null) {
									valueData = valueData.replaceAll("<" + keyName + ">", map2.get(keyName).toString());
								} else {
									valueData = valueData.replaceAll("<" + keyName + ">", "");
								}
							}
							responseParameterMap.put(mapw, valueData);
						} else {
							responseParameterMap.put(mapw, "");
						}

					}

				}

			}

			else {
				responseParameterMap.put(mapw, responseManipulatedList.get(0).get(mapw));
			}
		}
		/**
		 * Adding responseParameterMap in finalResponseList
		 */

		return responseParameterMap;
	}

	public static List<Map<String, Object>> generateModifiedList(List<Map<String, Object>> responseManipulatedList,
			Map<String, Object> map2, Map<String, Object> responseObjectMap, Map<String, Object> responseObjectMapNew,
			List<Map<String, Object>> modifiedListNew, List<Map<String, Object>> modifiedListOld) {
		System.out.println("Template List At starting of method::" + responseManipulatedList);

		/** Map<String, Object> responseObjectMap= new LinkedHashMap<>(); */
		for (String mapw : responseManipulatedList.get(0).keySet()) {

			Pattern pattern = Pattern.compile(TEMPLATE_PARAM_PATTERN);
			Matcher matcherUrl = pattern.matcher(String.valueOf(responseManipulatedList.get(0).get(mapw)));
			if (matcherUrl.find()) {
				String value = responseManipulatedList.get(0).get(mapw).toString().replaceAll("[<>]", "");
				if (isArray(value)) {
					String originalKey = "";
					if (!map2.containsKey(mapw)) {
						originalKey = getResponseKeyMap.get(mapw);
						;
					}

					else {
						originalKey = mapw;
					}

					Object itemValue = map2.get(originalKey);

					Type type = new TypeToken<List<Map<String, Object>>>() {
					}.getType();

					List<Map<String, Object>> al = new Gson().fromJson(itemValue.toString(), type);

					ArrayList<Map<String, Object>> responseManipulatedListNew = new ArrayList<>();
					responseManipulatedListNew = (ArrayList<Map<String, Object>>) responseManipulatedList.get(0)
							.get(mapw);
					// Object
					// responseManipulatedListObject=responseManipulatedList.get(0).get(mapw);
					// ArrayList<Map<String, Object>> responseManipulatedListNew =new
					// Gson().fromJson(responseManipulatedListObject.toString(), type);

					modifiedListNew = new ArrayList<>();
					for (Map<String, Object> dataNode : al) {
						Map<String, Object> responseObjectMapNew1 = new LinkedHashMap<>();
						generateModifiedList(responseManipulatedListNew, dataNode, responseObjectMapNew1,
								responseObjectMapNew, modifiedListNew, modifiedListOld);
						System.out.println("responseObjectMap::" + responseObjectMapNew1 + "\n");

					}

					List<Map<String, Object>> l = new ArrayList<>();
					l.addAll(modifiedListNew);
					responseObjectMap.put(mapw, l);
					modifiedListNew.clear();
					System.out.println("responseObjectMap::" + responseObjectMap + "\n");
					System.out.println("modified list::" + modifiedListNew);

				} else {

					StringBuilder finalValue = new StringBuilder();
					/**
					 * if (value.contains(",") ) {
					 * 
					 * String[] newValue = value.split(","); for (String string : newValue) {
					 * 
					 * if (map2.containsKey(string.trim())) {
					 * finalValue.append(map2System.out.println("Modified List::"
					 * +modifiedList);.get(string.trim()) + " "); } else { finalValue.append(""); }
					 * responseParameterMap.put(mapw, String.valueOf(finalValue).trim()); }
					 * 
					 * }
					 * 
					 */
					if (value.contains(",") && value.substring(0, 1).contains("{")) {

						System.out.println(value);

					} else if (map2.containsKey(value)) {
						responseObjectMap.put(mapw, map2.get(value));
					} else {
						// responseParameterMap.put(mapw, ""); // previously only single statement in
						// this else block

						String valueData = responseManipulatedList.get(0).get(mapw).toString();
						List<String> matchList = new ArrayList<String>();
						Pattern regex = Pattern.compile("<(.*?)>"); // pattern to metch content between <>
						Matcher regexMatcher = regex.matcher(valueData);

						while (regexMatcher.find()) {// Finds Matching Pattern in String
							matchList.add(regexMatcher.group(1));// Fetching Group from String
						}

						if (matchList.size() > 0) {
							for (String keyName : matchList) {
								if (map2.get(keyName) != null) {
									valueData = valueData.replaceAll("<" + keyName + ">", map2.get(keyName).toString());
								} else {
									valueData = valueData.replaceAll("<" + keyName + ">", "");
								}
							}
							responseObjectMap.put(mapw, valueData);
						} else {
							responseObjectMap.put(mapw, "");
						}

					}

				}

			} else {
				responseObjectMap.put(mapw, responseManipulatedList.get(0).get(mapw));
			}

		}
		modifiedListNew.add(responseObjectMap);

		return modifiedListNew;
		/**
		 * Adding responseParameterMap in finalResponseList
		 */

	}

	public static boolean isArray(String s) {

		return s.substring(0, 1).contains("[");

	}

	/**
	 * This Method is used to check whether all the input parameters required for
	 * group are valid or not. Steps will be followed in this Method:
	 * 
	 * 1.It will get the set of APIs to be called as per the group id passed with
	 * respect to country_code and ICCID passed to get their respective host
	 * addresses of End Node APIs.
	 * 
	 * 
	 * a.Call urlParameterValidator method to validate the client parameters with
	 * its mapping defined in the database.
	 * 
	 * @param groupID
	 * @param inputParameterMap
	 * @param request
	 * @param response
	 * @return ResponseEntity
	 * @throws Exception
	 */

	public ResponseEntity<?> parameterValidatorBasisOnGroupName(String groupID, Map<String, String> inputParameterMap,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		// Fetching all the error codes basis on group id and store in variable
		// errorMap.
		Map<String, Object> errorMAp = getErrorCodes(groupID, request, response);
		// This message will get all the api's associated with this group.
		OrchestrationMessage getUrlsMessage = orchestrationGenericProcess.GenericOrchestrationProcedureCalling("1",
				inputParameterMap, null, request, response);
		/**
		 * If message is valid then it will check for parameter list and check for its
		 * validation
		 **/
		if (getUrlsMessage.isValid()) {

			List<Map<String, Object>> urlResponse = (List<Map<String, Object>>) getUrlsMessage.getObject();
            //Condition to check if url response is null or not because of country code is invalid.
		    if(!urlResponse.isEmpty())
		    {
		    	// urlParameterValidator will return the list . if size of list is zero then all
				// the parameters has been validated if size is not zero it will return the
				// required and mandatory fields.
				Map<String, Map<String, String>> validatedParameterList = urlParameterValidator(urlResponse,
						inputParameterMap, request);

				/// validatedParameterList.size()==0 represents that all the parameters have
				/// been validated.
				if (validatedParameterList.size() == 0) {
					return new ResponseEntity<>(HttpStatus.OK);
				}

				// Else block states that parameters are not getting validated.
				else {
					auditLogInsert(String.valueOf(inputParameterMap.get(TRACKING_MESSAGE_HEADER)), groupLogID, null,
							Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))),
							"Invalid Parameter ", String.valueOf(environment.getProperty(LOG_FAILURE_RESPONSE_BIT)),
							"Invalid Parameter API Response ", String.valueOf(validatedParameterList).replaceAll(",", "|:")
									.replaceAll("\'", DATABASE_DELIMETER),
							request);

					/**
					 * If list contain key of mandatory parameters will send the error message
					 * mandatory fields are required
					 */
					if (validatedParameterList.containsKey(MANDATORY_VALIDATION_PARAM)) {
						Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("1");

						LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
						errorMessage.remove(PRIORITY);
						errorMessage.put("code", errorMessage.get("code"));
						errorMessage.put(DESCRIPTION, String.valueOf(validatedParameterList.get(MANDATORY_VALIDATION_PARAM))
								.replaceAll("=", "").replaceAll("\\{", "").replaceAll("\\}", ""));
						List<Map<String, Object>> errorList = new LinkedList<>();
						errorList.add(errorMessage);
						Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
						finalErrorMessageMap.put(ERROR, errorList);
						return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.BAD_REQUEST);

					}
					/** Otherwise it will set the error message parameter value not supported */
					else {
						Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("2");

						LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
						errorMessage.remove(PRIORITY);
						errorMessage.put("code", errorMessage.get("code"));
						errorMessage.put(DESCRIPTION, String.valueOf(validatedParameterList.get("notSupportParameterMap"))
								.replaceAll("=", "").replaceAll("\\{", "").replaceAll("\\}", ""));
						List<Map<String, Object>> errorList = new LinkedList<>();
						errorList.add(errorMessage);
						Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
						finalErrorMessageMap.put(ERROR, errorList);
						return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.BAD_REQUEST);
					}

				}
		    	
		    }
		    
		    else
		    {
		    	auditLogInsert(String.valueOf(inputParameterMap.get(TRACKING_MESSAGE_HEADER)).trim(), groupLogID,
						null, Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))),
						"Invalid COUNTRY Code ", String.valueOf(environment.getProperty(LOG_FAILURE_RESPONSE_BIT)),
						"Invalid COUNTRY Code ",
						String.valueOf(inputParameterMap).replaceAll(",", "|:").replaceAll("\'", DATABASE_DELIMETER),
						request);

				Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("1");

				LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
				errorMessage.remove(PRIORITY);
				errorMessage.put("code", errorMessage.get("code"));
				errorMessage.put(DESCRIPTION, "Country Code " + errorMessage.get(DESCRIPTION).toString()
						.concat(":" + "Invalid Country Code "));

				List<Map<String, Object>> errorList = new LinkedList<>();
				errorList.add(errorMessage);
				Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
				finalErrorMessageMap.put(ERROR, errorList);
				return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.BAD_REQUEST);
		    	
		    }
  	
			

		}

		
		
		
		/**
		 * If Orchestration url message in not valid means Internal issue from database
		 **/
		
		else {
			/**
			 * Response Of API
			 */
			auditLogInsert(String.valueOf(inputParameterMap.get(TRACKING_MESSAGE_HEADER)), groupLogID, null,
					Integer.parseInt(String.valueOf(environment.getProperty(LOG_FAILURE_BIT))),
					"Internal Issue Occured From Database",
					String.valueOf(environment.getProperty(LOG_FAILURE_RESPONSE_BIT)), ERROR_STRING,
					String.valueOf(inputParameterMap).replaceAll(",", "|:").replaceAll("\'", DATABASE_DELIMETER),
					request);

			Map<String, Object> errorMessageMap = (Map<String, Object>) errorMAp.get("3");

			LinkedHashMap<String, Object> errorMessage = new LinkedHashMap<>((Map) errorMessageMap);
			errorMessage.remove(PRIORITY);
			errorMessage.put("code", errorMessage.get("code"));
			errorMessage.put(DESCRIPTION,
					errorMessage.get(DESCRIPTION).toString().concat(":" + PROCESS_FAIL + inputParameterMap.toString()));
			List<Map<String, Object>> errorList = new LinkedList<>();
			errorList.add(errorMessage);
			Map<String, Object> finalErrorMessageMap = new LinkedHashMap<>();
			finalErrorMessageMap.put(ERROR, errorList);
			return new ResponseEntity<>(finalErrorMessageMap, HttpStatus.INTERNAL_SERVER_ERROR);

		}

	}

	private ResponseEntity<?> genericApiResponseMethod(String endNodeResponse, Map<String, String> map,
			HttpServletResponse response) {

		Map<String, String> httpStatusCodesMap = httpStatusParameter.getHttpCodeMap();
		List<Map<String, Object>> finalResponseList = new LinkedList<>();
		try {

			if (String.valueOf(map.get(NOTIFICATION_TEMPLATE)).equalsIgnoreCase("null")) {
				StringBuffer sb = new StringBuffer();
				if (String.valueOf(map.get("set_response_key")).equalsIgnoreCase("null")
						&& String.valueOf(map.get("get_response_key")).equalsIgnoreCase("null")) {
					Pattern pattern = Pattern.compile(TEMPLATE_PARAM_PATTERN);
					Matcher matcher = pattern.matcher(String.valueOf(map.get(NOTIFICATION_TEMPLATE)));
					Map<String, String> requesParameterMap = new LinkedHashMap<>();
					JsonOlcoreModification.parseOL(endNodeResponse, requesParameterMap);
					while (matcher.find()) {
						String matchCase = matcher.group(0);

						if (matchCase.replaceAll(REPLACE_PARAM_PATTERN, "").contains("#&#")) {
							String matchCaseValue = String
									.valueOf(requesParameterMap.get(matchCase.replaceAll(REPLACE_PARAM_PATTERN, "")));
							matcher.appendReplacement(sb, matchCaseValue);
						}
					}
					matcher.appendTail(sb);

				}
				return new ResponseEntity<>(sb, HttpStatus.ACCEPTED);
			} else {

				Type listType = new TypeToken<Map<String, Object>>() {
				}.getType();

				if (String.valueOf(map.get(NOTIFICATION_TEMPLATE)).equals("{}")) {
					return new ResponseEntity<>("{}",
							HttpStatus.valueOf(httpStatusCodesMap.get(map.get("response_http_code"))));
				}

				Map<String, Object> templateMapParam = new Gson()
						.fromJson(String.valueOf(map.get(NOTIFICATION_TEMPLATE)), listType);

				List<Map<String, Object>> responseManipulatedList = null;
				if (templateMapParam.get(String.valueOf(map.get("set_response_key"))) != null) {
					responseManipulatedList = (List<Map<String, Object>>) templateMapParam
							.get(String.valueOf(map.get("set_response_key")));
				} else {
					responseManipulatedList = new ArrayList<Map<String, Object>>();
					responseManipulatedList.add(templateMapParam);
				}

				Map<String, Object> apiResponse = new ObjectMapper().readValue(endNodeResponse, Map.class);
				/**
				 * Casting externalExtraPasses field from apiResponse in List<Map<String,
				 * Object>>
				 */

				logger.info("Response From End Node " + endNodeResponse);

				List<Map<String, Object>> responseListToManipulate = null;
				if (apiResponse.get(String.valueOf(map.get("get_response_key"))) != null) {
					responseListToManipulate = (List<Map<String, Object>>) apiResponse
							.get(String.valueOf(map.get("get_response_key")));

					apiResponse.get(String.valueOf(map.get("get_response_key")));

				}

				/*
				 * if (apiResponse.get(String.valueOf(map.get("get_response_key"))) != null) {
				 * 
				 * //Code Commented as get_response_key will not be used to get data of a
				 * particular key beacuse we have Iteration over response now. //Response at any
				 * key can be get using notification template.
				 * 
				 * 
				 *//**
					 * responseListToManipulate = (List<Map<String, Object>>) apiResponse
					 * .get(String.valueOf(map.get("get_response_key")));
					 */
				/*
				
				*//**
					 * we will have use get_response_key as a map now. It will keep key value pair
					 * in map where key will be the original key and value will be the key at which
					 * response is coming.
					 * 
					 *//*
						 * getResponseKeyMap= (LinkedHashMap<String, String>) new
						 * ObjectMapper().readValue(String.valueOf(map.get("get_response_key")),
						 * Map.class);
						 * 
						 * 
						 * }
						 */

				else {
					responseListToManipulate = new ArrayList<Map<String, Object>>();
					responseListToManipulate.add(apiResponse);

				}

				if (responseListToManipulate != null && !responseListToManipulate.isEmpty()) {
					finalResponseList = setIntoTemplate(responseManipulatedList, responseListToManipulate);
				}

				/**
				 * Returning Response after adding finalResponseList in apiResponse
				 */
				apiResponse = new LinkedHashMap<>();

				if (String.valueOf(map.get("set_response_key")).equals("null")) {
					apiResponse = finalResponseList.get(0);
				} else {
					apiResponse.put(String.valueOf(map.get("set_response_key")), finalResponseList);
				}

				logger.info("Response after manipulation and modification from ol core" + apiResponse);

				return new ResponseEntity<>(apiResponse,
						HttpStatus.valueOf(httpStatusCodesMap.get(map.get("response_http_code"))));
/**				return new ResponseEntity<>(new Gson().toJson(apiResponse), HttpStatus.valueOf(httpStatusCodesMap.get(map.get("response_http_code"))));*/
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.info(e);
			// need to change
			return new ResponseEntity<>(new LinkedHashMap<>(), HttpStatus.ACCEPTED);
		}

	}

}
