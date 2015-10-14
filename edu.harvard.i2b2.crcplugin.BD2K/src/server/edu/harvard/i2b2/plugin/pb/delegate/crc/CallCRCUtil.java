package edu.harvard.i2b2.plugin.pb.delegate.crc;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.harvard.i2b2.common.exception.I2B2Exception;
import edu.harvard.i2b2.common.exception.StackTraceUtil;
import edu.harvard.i2b2.common.util.jaxb.JAXBUnWrapHelper;
import edu.harvard.i2b2.common.util.jaxb.JAXBUtilException;
import edu.harvard.i2b2.crc.datavo.i2b2message.BodyType;
import edu.harvard.i2b2.crc.datavo.i2b2message.FacilityType;
import edu.harvard.i2b2.crc.datavo.i2b2message.MessageHeaderType;
import edu.harvard.i2b2.crc.datavo.i2b2message.RequestHeaderType;
import edu.harvard.i2b2.crc.datavo.i2b2message.RequestMessageType;
import edu.harvard.i2b2.crc.datavo.i2b2message.ResponseHeaderType;
import edu.harvard.i2b2.crc.datavo.i2b2message.ResponseMessageType;
import edu.harvard.i2b2.crc.datavo.i2b2message.SecurityType;
import edu.harvard.i2b2.crc.datavo.i2b2message.StatusType;
import edu.harvard.i2b2.crc.datavo.pdo.PidSet;
import edu.harvard.i2b2.crc.datavo.pdo.query.FilterListType;
import edu.harvard.i2b2.crc.datavo.pdo.query.GetPDOFromInputListRequestType;
import edu.harvard.i2b2.crc.datavo.pdo.query.InputOptionListType;
import edu.harvard.i2b2.crc.datavo.pdo.query.OutputOptionListType;
import edu.harvard.i2b2.crc.datavo.pdo.query.OutputOptionSelectType;
import edu.harvard.i2b2.crc.datavo.pdo.query.OutputOptionType;
import edu.harvard.i2b2.crc.datavo.pdo.query.PatientDataResponseType;
import edu.harvard.i2b2.crc.datavo.pdo.query.PatientListType;
import edu.harvard.i2b2.crc.datavo.pdo.query.PdoQryHeaderType;
import edu.harvard.i2b2.crc.datavo.pdo.query.PdoRequestTypeType;
import edu.harvard.i2b2.crc.datavo.pm.CellDataType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.MasterInstanceResultResponseType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.MasterResponseType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.ObjectFactory;
import edu.harvard.i2b2.crc.datavo.setfinder.query.PsmQryHeaderType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.PsmRequestTypeType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.QueryDefinitionRequestType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.QueryDefinitionType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.QueryModeType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.QueryResultInstanceType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.ResultOutputOptionListType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.ResultOutputOptionType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.UserType;
/*
import edu.harvard.i2b2.crc.datavo.pm.ConfigureType;
import edu.harvard.i2b2.crc.datavo.pm.GetUserConfigurationType;
import edu.harvard.i2b2.crc.datavo.pm.ObjectFactory;
import edu.harvard.i2b2.crc.datavo.pm.ProjectType;
import edu.harvard.i2b2.crc.datavo.pm.UserType;
 */
import edu.harvard.i2b2.plugin.pb.datavo.CRCJAXBUtil;
import edu.harvard.i2b2.plugin.pb.util.QueryProcessorUtil;

public class CallCRCUtil {

	private SecurityType securityType = null;
	private String projectId = null;
	private String crcUrl = null;

	private static Log log = LogFactory.getLog(CallCRCUtil.class);

	
	public CallCRCUtil()
	{
		
	}
	public CallCRCUtil(String requestXml) throws JAXBUtilException,
	I2B2Exception {
		this(QueryProcessorUtil.getInstance().getOntologyUrl(), requestXml);
	}

	public CallCRCUtil(String crcUrl, String requestXml)
			throws JAXBUtilException, I2B2Exception {
		JAXBElement responseJaxb = CRCJAXBUtil.getJAXBUtil()
				.unMashallFromString(requestXml);
		RequestMessageType request = (RequestMessageType) responseJaxb
				.getValue();
		this.securityType = request.getMessageHeader().getSecurity();
		this.projectId = request.getMessageHeader().getProjectId();
		this.crcUrl = crcUrl;
	}

	public CallCRCUtil(SecurityType securityType, String projectId, CellDataType crcUrl)
			throws I2B2Exception {
		this.securityType = securityType;
		this.projectId = projectId;
		this.crcUrl = crcUrl.getUrl();
		log.debug("CRC PM call url" + crcUrl);
	}

	public CallCRCUtil(String crcUrl, SecurityType securityType,
			String projectId) throws I2B2Exception {
		this.securityType = securityType;
		this.projectId = projectId;
		this.crcUrl = crcUrl;
	}


	/*
	public ProjectType callUserProject() throws AxisFault, I2B2Exception {
		RequestMessageType requestMessageType = getI2B2RequestMessage();
		OMElement requestElement = null;
		ProjectType projectType = null;
		try {
			requestElement = buildOMElement(requestMessageType);
			log.debug("CRC PM call's request xml " + requestElement);
			OMElement response = getServiceClient().sendReceive(requestElement);
			projectType = getUserProjectFromResponse(response.toString());
		} catch (XMLStreamException e) {
			e.printStackTrace();
			throw new I2B2Exception("" + StackTraceUtil.getStackTrace(e));
		} catch (JAXBUtilException e) {
			e.printStackTrace();
			throw new I2B2Exception("" + StackTraceUtil.getStackTrace(e));
		}
		return projectType;
	}
	 */


	public PidSet callQueryDefinionType(QueryDefinitionType queryDef, ResultOutputOptionListType resultOutputOptionListType) throws AxisFault, I2B2Exception {
		;
		RequestMessageType requestMessageType = getI2B2RequestMessage(queryDef, resultOutputOptionListType);
		OMElement requestElement = null;
		OMElement requestElement2 = null;
		MasterInstanceResultResponseType responseType = null;
		try {
			requestElement = buildOMElement(requestMessageType);
			log.debug("CRC PM call's request xml " + requestElement);
			OMElement response = getServiceClient("request").sendReceive(requestElement);
			System.out.println(response.toString());
			responseType = getMasterResponseTypeFromResponse(response.toString());

			log.debug("Got response and unmarhslled it ");
			// try to get the patient set
			String resultInstanceId = null;
			for (QueryResultInstanceType qrit: responseType.getQueryResultInstance())
			{
				if (qrit.getQueryResultType().getName().equals("PATIENTSET"))
				{
					resultInstanceId = qrit.getResultInstanceId();

					GetPDOFromInputListRequestType	getPDO = new GetPDOFromInputListRequestType();
					InputOptionListType inputlist = new InputOptionListType();
					PatientListType patientList = new PatientListType();
					patientList.setMax(1000000);
					patientList.setMin(0);
					patientList.setPatientSetCollId(resultInstanceId);
					inputlist.setPatientList(patientList);
					getPDO.setInputList(inputlist );

					OutputOptionType patientOutputOptionType = new OutputOptionType();
					patientOutputOptionType
					.setSelect(OutputOptionSelectType.USING_INPUT_LIST);

					patientOutputOptionType.setOnlykeys(false);


					FilterListType filterListType = new FilterListType();
					OutputOptionListType outputList = new OutputOptionListType();
					//outputList.setPatientSet(patientOutputOptionType);
					outputList.setPidSet(patientOutputOptionType);
					getPDO.setOutputOption(outputList);
					getPDO.setFilterList(filterListType);
					RequestMessageType requestMessageType2 = getI2B2RequestMessage(getPDO);

					requestElement2 = buildOMElement(requestMessageType2);
					log.debug("CRC PM call's request xml " + requestElement2);
					OMElement response2 = getServiceClient("pdorequest").sendReceive(requestElement2);
					System.out.println(response2.toString());
					PatientDataResponseType responseType2 = getPatientDataTypeFromResponse(response2.toString());
					return responseType2.getPatientData().getPidSet();
				}
			}


			//mm			projectType = getUserConfigureFromResponse(response.toString());
		} catch (XMLStreamException e) {
			e.printStackTrace();
			throw new I2B2Exception("" + StackTraceUtil.getStackTrace(e));
		} catch (JAXBUtilException e) {
			e.printStackTrace();
			throw new I2B2Exception("" + StackTraceUtil.getStackTrace(e));
		}
		return null;
	}


	public String callQueryDefinionTypeFull(QueryDefinitionType queryDef, ResultOutputOptionListType resultOutputOptionListType) throws AxisFault, I2B2Exception {
		;
		RequestMessageType requestMessageType = getI2B2RequestMessage(queryDef, resultOutputOptionListType);
		OMElement requestElement = null;
		OMElement requestElement2 = null;
		MasterInstanceResultResponseType responseType = null;
		try {
			requestElement = buildOMElement(requestMessageType);
			log.debug("CRC PM call's request xml " + requestElement);
			OMElement response = getServiceClient("request").sendReceive(requestElement);
			return response.toString();
			//responseType = getMasterResponseTypeFromResponse(response.toString());

			//log.debug("Got response and unmarhslled it ");
		
		} catch (XMLStreamException e) {
			e.printStackTrace();
			throw new I2B2Exception("" + StackTraceUtil.getStackTrace(e));
		} catch (JAXBUtilException e) {
			e.printStackTrace();
			throw new I2B2Exception("" + StackTraceUtil.getStackTrace(e));
		}
		//return null;
	}

	
	public MasterInstanceResultResponseType getMasterResponseTypeFromResponse(String responseXml)
			throws JAXBUtilException, I2B2Exception {
		JAXBElement responseJaxb = CRCJAXBUtil.getJAXBUtil()
				.unMashallFromString(responseXml);
		ResponseMessageType pmRespMessageType = (ResponseMessageType) responseJaxb
				.getValue();
		log.debug("CRC's PM call response xml" + responseXml);

		ResponseHeaderType responseHeader = pmRespMessageType
				.getResponseHeader();
		StatusType status = responseHeader.getResultStatus().getStatus();
		String procStatus = status.getType();
		String procMessage = status.getValue();

		if (procStatus.equals("ERROR")) {
			log.info("PM Error reported by CRC web Service " + procMessage);
			throw new I2B2Exception("PM Error reported by CRC web Service "
					+ procMessage);
		} else if (procStatus.equals("WARNING")) {
			log.info("PM Warning reported by CRC web Service" + procMessage);
			throw new I2B2Exception("PM Warning reported by CRC web Service"
					+ procMessage);
		}

		JAXBUnWrapHelper helper = new JAXBUnWrapHelper();
		MasterInstanceResultResponseType configureType = (MasterInstanceResultResponseType) helper.getObjectByClass(
				pmRespMessageType.getMessageBody().getAny(),
				MasterInstanceResultResponseType.class);
		return configureType;
	}



	private PatientDataResponseType getPatientDataTypeFromResponse(String responseXml)
			throws JAXBUtilException, I2B2Exception {
		JAXBElement responseJaxb = CRCJAXBUtil.getJAXBUtil()
				.unMashallFromString(responseXml);
		ResponseMessageType pmRespMessageType = (ResponseMessageType) responseJaxb
				.getValue();
		log.debug("CRC's PM call response xml" + responseXml);

		ResponseHeaderType responseHeader = pmRespMessageType
				.getResponseHeader();
		StatusType status = responseHeader.getResultStatus().getStatus();
		String procStatus = status.getType();
		String procMessage = status.getValue();

		if (procStatus.equals("ERROR")) {
			log.info("PM Error reported by CRC web Service " + procMessage);
			throw new I2B2Exception("PM Error reported by CRC web Service "
					+ procMessage);
		} else if (procStatus.equals("WARNING")) {
			log.info("PM Warning reported by CRC web Service" + procMessage);
			throw new I2B2Exception("PM Warning reported by CRC web Service"
					+ procMessage);
		}

		JAXBUnWrapHelper helper = new JAXBUnWrapHelper();
		PatientDataResponseType configureType = (PatientDataResponseType) helper.getObjectByClass(
				pmRespMessageType.getMessageBody().getAny(),
				PatientDataResponseType.class);
		return configureType;
	}

	private OMElement buildOMElement(RequestMessageType requestMessageType)
			throws XMLStreamException, JAXBUtilException {
		StringWriter strWriter = new StringWriter();
		edu.harvard.i2b2.crc.datavo.i2b2message.ObjectFactory hiveof = new edu.harvard.i2b2.crc.datavo.i2b2message.ObjectFactory();
		CRCJAXBUtil.getJAXBUtil().marshaller(
				hiveof.createRequest(requestMessageType), strWriter);
		// getOMElement from message
		OMFactory fac = OMAbstractFactory.getOMFactory();

		StringReader strReader = new StringReader(strWriter.toString());
		XMLInputFactory xif = XMLInputFactory.newInstance();
		XMLStreamReader reader = xif.createXMLStreamReader(strReader);
		StAXOMBuilder builder = new StAXOMBuilder(reader);
		OMElement request = builder.getDocumentElement();
		return request;
	}

	private RequestMessageType getI2B2RequestMessage(QueryDefinitionType queryDef, ResultOutputOptionListType resultOutputOptionListType ) {
		QueryProcessorUtil queryUtil = QueryProcessorUtil.getInstance();
		MessageHeaderType messageHeaderType = (MessageHeaderType) queryUtil
				.getSpringBeanFactory().getBean("message_header");
		messageHeaderType.setSecurity(securityType);
		messageHeaderType.setProjectId(projectId);

		messageHeaderType.setReceivingApplication(messageHeaderType
				.getSendingApplication());
		FacilityType facilityType = new FacilityType();
		facilityType.setFacilityName("sample");
		messageHeaderType.setSendingFacility(facilityType);
		messageHeaderType.setReceivingFacility(facilityType);
		// build message body
		// GetUserInfoType getUserInfoType = null;

		//GetUserConfigurationType userConfig = new GetUserConfigurationType();
		//userConfig.getProject().add(projectId);
		//QueryDefinitionType queryDef = new QueryDefinitionType();
		//queryDef.g

		RequestMessageType requestMessageType = new RequestMessageType();
		ObjectFactory of = new ObjectFactory();
		BodyType bodyType = new BodyType();
		PsmQryHeaderType psmHeader = new PsmQryHeaderType();
		psmHeader.setPatientSetLimit(0);
		psmHeader.setEstimatedTime(0);


		UserType userType = new UserType();
		userType.setLogin(securityType.getUsername());
		userType.setGroup(projectId);
		userType.setValue(securityType.getUsername());

		psmHeader.setUser(userType);
		psmHeader
		.setRequestType(PsmRequestTypeType.CRC_QRY_RUN_QUERY_INSTANCE_FROM_QUERY_DEFINITION);

		psmHeader.setQueryMode(QueryModeType.OPTIMIZE_WITHOUT_TEMP_TABLE);

		QueryDefinitionRequestType queryDefinitionRequestType = new QueryDefinitionRequestType();

		if (resultOutputOptionListType == null) {
			resultOutputOptionListType = new ResultOutputOptionListType();
			ResultOutputOptionType resultOutputOptionType = new ResultOutputOptionType();
			resultOutputOptionType.setName("patient_count_xml");
			resultOutputOptionType.setPriorityIndex(1);

			ResultOutputOptionType resultOutputOptionType2 = new ResultOutputOptionType();
			resultOutputOptionType2.setName("patientset");
			resultOutputOptionType2.setPriorityIndex(2);
		

			resultOutputOptionListType.getResultOutput().add(
					resultOutputOptionType);
			resultOutputOptionListType.getResultOutput().add(
					resultOutputOptionType2);
		}
		
		queryDefinitionRequestType.setQueryDefinition(queryDef);
		queryDefinitionRequestType.setResultOutputList(resultOutputOptionListType);

		bodyType.getAny().add(of.createPsmheader(psmHeader)); //.createGetUserConfiguration(userConfig));
		bodyType.getAny().add(of.createRequest(queryDefinitionRequestType)); //.createGetUserConfiguration(userConfig));
		requestMessageType.setMessageBody(bodyType);

		requestMessageType.setMessageHeader(messageHeaderType);

		RequestHeaderType requestHeader = new RequestHeaderType();
		requestHeader.setResultWaittimeMs(180000);
		requestMessageType.setRequestHeader(requestHeader);

		return requestMessageType;

	}


	private RequestMessageType getI2B2RequestMessage(GetPDOFromInputListRequestType queryDef ) {
		QueryProcessorUtil queryUtil = QueryProcessorUtil.getInstance();
		MessageHeaderType messageHeaderType = (MessageHeaderType) queryUtil
				.getSpringBeanFactory().getBean("message_header");
		messageHeaderType.setSecurity(securityType);
		messageHeaderType.setProjectId(projectId);

		messageHeaderType.setReceivingApplication(messageHeaderType
				.getSendingApplication());
		FacilityType facilityType = new FacilityType();
		facilityType.setFacilityName("sample");
		messageHeaderType.setSendingFacility(facilityType);
		messageHeaderType.setReceivingFacility(facilityType);
		// build message body
		// GetUserInfoType getUserInfoType = null;

		//GetUserConfigurationType userConfig = new GetUserConfigurationType();
		//userConfig.getProject().add(projectId);
		//QueryDefinitionType queryDef = new QueryDefinitionType();
		//queryDef.g

		RequestMessageType requestMessageType = new RequestMessageType();
		edu.harvard.i2b2.crc.datavo.pdo.query.ObjectFactory of = new  edu.harvard.i2b2.crc.datavo.pdo.query.ObjectFactory();
		BodyType bodyType = new BodyType();


		UserType userType = new UserType();
		userType.setLogin(securityType.getUsername());
		userType.setGroup(projectId);
		userType.setValue(securityType.getUsername());


		PdoQryHeaderType pdoHeader = new PdoQryHeaderType();
		pdoHeader.setEstimatedTime(180000);
		pdoHeader.setRequestType(PdoRequestTypeType.GET_PDO_FROM_INPUT_LIST);

		bodyType.getAny().add(of.createPdoheader(pdoHeader)); //.createGetUserConfiguration(userConfig));
		bodyType.getAny().add(of.createRequest(queryDef)); //.createGetUserConfiguration(userConfig));
		requestMessageType.setMessageBody(bodyType);

		requestMessageType.setMessageHeader(messageHeaderType);

		RequestHeaderType requestHeader = new RequestHeaderType();
		requestHeader.setResultWaittimeMs(180000);
		requestMessageType.setRequestHeader(requestHeader);

		return requestMessageType;

	}


	private ServiceClient getServiceClient(String method) {
		// call
		ServiceClient serviceClient = CRCServiceClient.getServiceClient();

		Options options = new Options();
		options.setTo(new EndpointReference(crcUrl + method));
		options.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		options.setProperty(Constants.Configuration.ENABLE_REST,
				Constants.VALUE_TRUE);
		options.setTimeOutInMilliSeconds(50000);
		serviceClient.setOptions(options);
		return serviceClient;

	}

}
