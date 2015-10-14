package edu.harvard.i2b2.plugin.pb.main;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.apache.axis2.AxisFault;

import edu.harvard.i2b2.common.exception.I2B2DAOException;
import edu.harvard.i2b2.common.exception.I2B2Exception;
import edu.harvard.i2b2.common.util.jaxb.JAXBUtil;
import edu.harvard.i2b2.common.util.jaxb.JAXBUtilException;
import edu.harvard.i2b2.crc.datavo.i2b2message.SecurityType;
import edu.harvard.i2b2.crc.datavo.i2b2result.BodyType;
import edu.harvard.i2b2.crc.datavo.i2b2result.DataType;
import edu.harvard.i2b2.crc.datavo.i2b2result.ResultEnvelopeType;
import edu.harvard.i2b2.crc.datavo.i2b2result.ResultType;
import edu.harvard.i2b2.crc.datavo.ontology.ConceptType;
import edu.harvard.i2b2.crc.datavo.ontology.ConceptsType;
import edu.harvard.i2b2.crc.datavo.pdo.PidSet;
import edu.harvard.i2b2.crc.datavo.pdo.PidType;
import edu.harvard.i2b2.crc.datavo.pdo.PidType.PatientMapId;
import edu.harvard.i2b2.crc.datavo.pdo.query.PidListType.Pid;
import edu.harvard.i2b2.crc.datavo.pm.CellDataType;
import edu.harvard.i2b2.crc.datavo.pm.ConfigureType;
import edu.harvard.i2b2.crc.datavo.pm.ConfiguresType;
import edu.harvard.i2b2.crc.datavo.pm.HiveDataType;
import edu.harvard.i2b2.crc.datavo.pm.ProjectType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.AnalysisDefinitionType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.AnalysisParamType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.AnalysisResultOptionListType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.AnalysisResultOptionType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.ItemType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.MasterInstanceResultResponseType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.PanelType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.QueryResultInstanceType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.ResultOutputOptionListType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.PanelType.TotalItemOccurrences;
import edu.harvard.i2b2.crc.datavo.setfinder.query.PatientSetType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.QueryDefinitionType;
import edu.harvard.i2b2.crc.datavo.setfinder.query.ResultOutputOptionType;
import edu.harvard.i2b2.plugin.pb.delegate.pm.CallPMUtil;
import edu.harvard.i2b2.plugin.pb.dao.DAOFactoryHelper;
import edu.harvard.i2b2.plugin.pb.dao.DataSourceLookupHelper;
import edu.harvard.i2b2.plugin.pb.dao.SetFinderDAOFactory;
import edu.harvard.i2b2.plugin.pb.dao.setfinder.IQueryInstanceDao;
import edu.harvard.i2b2.plugin.pb.dao.setfinder.IQueryMasterDao;
import edu.harvard.i2b2.plugin.pb.dao.setfinder.IQueryResultInstanceDao;
import edu.harvard.i2b2.plugin.pb.dao.setfinder.IXmlResultDao;
import edu.harvard.i2b2.plugin.pb.dao.setfinder.QueryStatusTypeId;
import edu.harvard.i2b2.plugin.pb.datavo.CRCJAXBUtil;
import edu.harvard.i2b2.plugin.pb.datavo.DataSourceLookup;
import edu.harvard.i2b2.plugin.pb.datavo.QtQueryInstance;
import edu.harvard.i2b2.plugin.pb.datavo.QtQueryMaster;
import edu.harvard.i2b2.plugin.pb.datavo.QtQueryResultInstance;
import edu.harvard.i2b2.plugin.pb.delegate.crc.CallCRCUtil;
import edu.harvard.i2b2.plugin.pb.delegate.ontology.CallOntologyUtil;
import edu.harvard.i2b2.plugin.pb.util.I2B2RequestMessageHelper;
import edu.harvard.i2b2.plugin.pb.util.PMServiceAccountUtil;
import edu.harvard.i2b2.plugin.pb.util.QueryProcessorUtil;

/**
 * 
 * Sample analysis plugin.
 * 
 * Shows how to fetch datasource and uses existing DAOs for following
 * operations.
 * 
 * 
 * a)using instance id to fetch analysis requests xml from the master table
 * 
 * b)write generated result xml to the QT_XML_RESULT table
 * 
 */
public class DistributedQueryMain extends edu.harvard.i2b2.plugin.pb.dao.CRCDAO {

	private static final String RESULT_NAME = "DISTRIBUTED_QUERY";

	public static void main(String args[]) throws Exception {
		DistributedQueryMain main1 = new DistributedQueryMain();

		// read command line params[domain, project, user and analysis
		// instance id
		String arg = "", domainId = "", projectId = "", userId = "", patientSetId = "", instanceId = "", conceptPath = "";
		int i = 0;
		while (i < args.length) {
			arg = args[i++];
			if (arg.startsWith("-domain_id")) {
				domainId = arg.substring(arg.indexOf('=') + 1);
			} else if (arg.startsWith("-project_id")) {
				projectId = arg.substring(arg.indexOf('=') + 1);
			} else if (arg.startsWith("-user_id")) {
				userId = arg.substring(arg.indexOf('=') + 1);
			} else if (arg.startsWith("-instance_id")) {
				instanceId = arg.substring(arg.indexOf('=') + 1);
			}
		}
		System.out.println("domainId = " + domainId + " project " + projectId
				+ " userid" + userId + " instanceId " + instanceId);

		// call the calculation function
		main1.calculateAndWriteResultXml(projectId, userId, domainId,
				patientSetId, instanceId, conceptPath);
	}

	public void calculateAndWriteResultXml(String projectId, String userId,
			String domainId, String patientSetId, String instanceId,
			String queryDefinition) throws Exception {
		boolean errorFlag = false;
		String resultInstanceId = "";
		SetFinderDAOFactory setfinderDaoFactory = null;
		Throwable throwable = null;
		try {

			// find out datasource for the matching domain,project and user id
			DataSourceLookupHelper dataSourceLookupHelper = new DataSourceLookupHelper();
			DataSourceLookup dataSourceLookup = dataSourceLookupHelper
					.matchDataSource(domainId, projectId, userId);

			// inside analysis plugin always instanciate datasource using
			// spring, the
			// jboss container datasource will not work
			QueryProcessorUtil qpUtil = QueryProcessorUtil.getInstance();
			DataSource dataSource = qpUtil.getSpringDataSource(dataSourceLookup
					.getDataSource());

			DAOFactoryHelper daoHelper = new DAOFactoryHelper(dataSourceLookup,
					dataSource);

			// from the dao helper, get the setfinder dao factory
			setfinderDaoFactory = daoHelper.getDAOFactory()
					.getSetFinderDAOFactory();

			// read the analysis definition from the master to perform the
			// calculation
			// //step 1:get master id from the instance id
			IQueryInstanceDao queryInstanceDao = setfinderDaoFactory
					.getQueryInstanceDAO();
			QtQueryInstance queryInstance = queryInstanceDao
					.getQueryInstanceByInstanceId(instanceId);
			String masterId = queryInstance.getQtQueryMaster()
					.getQueryMasterId();

			// //step2:get analysis definition from the master id to read the
			// parameters like concept_path,..
			IQueryMasterDao queryMasterDao = setfinderDaoFactory
					.getQueryMasterDAO();
			QtQueryMaster qtQueryMaster = queryMasterDao
					.getQueryDefinition(masterId);
			String requestXml = qtQueryMaster.getRequestXml();
			System.out.println("The request xml " + requestXml);

			String i2b2RequestXml = qtQueryMaster.getI2b2RequestXml();
			I2B2RequestMessageHelper analysisRequestHelper = new I2B2RequestMessageHelper(
					i2b2RequestXml);
			SecurityType securityType = analysisRequestHelper.getSecurityType();

			QueryMaster queryMasterHelper = new QueryMaster(setfinderDaoFactory);
			AnalysisDefinitionType analysisDefinition = queryMasterHelper
					.getAnalysisDefinitionByMasterId(masterId);
			AnalysisParamType queryDefinitionParam = analysisDefinition
					.getCrcAnalysisInputParam().getParam().get(0);
			queryDefinition = queryDefinitionParam.getValue();


			ResultOutputOptionListType resultOutputOptionListType = new ResultOutputOptionListType();
			for (AnalysisResultOptionType results: analysisDefinition.getCrcAnalysisResultList().getResultOutput())
			{

				//	results.get

				ResultOutputOptionType resultOutput = new ResultOutputOptionType();
				resultOutput.setDisplayType(results.getDisplayType());
				resultOutput.setFullName(results.getFullName());
				resultOutput.setName(results.getName());
				resultOutput.setPriorityIndex(results.getPriorityIndex());
				resultOutputOptionListType.getResultOutput().add(resultOutput );
			}
			// Check against the parent PM
			CallPMUtil pmUtil = new CallPMUtil( securityType,   projectId );


			I2B2RequestMessageHelper analysisRequestHelper2 = new I2B2RequestMessageHelper(
					queryDefinition);
			QueryDefinitionType qdtOrig = analysisRequestHelper2.getQueryDefinition();

			QueryDefinitionType qdtOrigRebuilt = new QueryDefinitionType();
			qdtOrigRebuilt.setQueryName("Parent Run: " + analysisRequestHelper2.getQueryDefinition().getQueryName());
			qdtOrigRebuilt.setQueryTiming(analysisRequestHelper2.getQueryDefinition().getQueryTiming());
			qdtOrigRebuilt.setLinkageThreshold(0);
			qdtOrigRebuilt.setSpecificityScale(analysisRequestHelper2.getQueryDefinition().getSpecificityScale());
			for (PanelType panel: analysisRequestHelper2.getQueryDefinition().getPanel())
			{
				PanelType newPanel = new PanelType();
				newPanel.setInvert(panel.getInvert());
				newPanel.setName(panel.getName());
				newPanel.setPanelAccuracyScale(panel.getPanelAccuracyScale());
				newPanel.setPanelDateFrom(panel.getPanelDateFrom());
				newPanel.setPanelDateTo(panel.getPanelDateTo());
				newPanel.setPanelNumber(panel.getPanelNumber());
				newPanel.setPanelTiming(panel.getPanelTiming());
				qdtOrigRebuilt.getPanel().add(newPanel);
			}
			//analysisRequestHelper2.getQueryDefinition();
			//Clear out all items from the rebuiltPanel
			//for (PanelType panel: qdtOrigRebuilt.getPanel())
			//	{
			//		panel.getItem().clear();
			//	}


			ConfigureType ctype =  pmUtil.callUserConfigure();


			for (HiveDataType configuretype: ctype.getHiveDatas().getHiveData())
			{
				if (configuretype.getActive()  != 3)
					continue;

				qdtOrig = analysisRequestHelper2.getQueryDefinition();
				//Clone queryDeftype and than go through each panel

				for (int i=0; i < qdtOrig.getPanel().size(); i++)
				{
					QueryDefinitionType qdtClone = new QueryDefinitionType();
					qdtClone.setQueryName(analysisRequestHelper2.getQueryDefinition().getQueryName());
					qdtClone.setQueryTiming(analysisRequestHelper2.getQueryDefinition().getQueryTiming());
					qdtClone.setLinkageThreshold(analysisRequestHelper2.getQueryDefinition().getLinkageThreshold());
					qdtClone.setSpecificityScale(analysisRequestHelper2.getQueryDefinition().getSpecificityScale());

					//Panel newPanel - 
					qdtClone.getPanel().add(qdtOrig.getPanel().get(i));
					//addAll(analysisRequestHelper2.getQueryDefinition().getPanel());

					//for (int k=0; k < qdtOrig.getPanel().size(); k++){
					//	if (k !=i)
					//		qdtClone.getPanel().remove(k);
					//}
					PidSet pset = getPatientSet(pmUtil, "/" + configuretype.getDomainName(), qdtClone, requestXml, projectId, securityType, ctype);

					// Child
					//PanelType panelType = analysisRequestHelper2.getQueryDefinition().getPanel().get(i);

					if (pset == null)
						continue;

					for (PidType pidType: pset.getPid()) {
						for (PatientMapId pid: pidType.getPatientMapId()) {
							//if (pid.getSource().equals("Hospital-1")) {
							ItemType itemType = new ItemType();
							itemType.setItemKey("patient:" + pid.getSource() + ":" + pid.getValue());
							itemType.setItemName("<font color=\"#darkblue\">[" + configuretype.getDomainName() + "]</font> " + pid.getSource() + ":" + pid.getValue());
							//								+ " - " + pid.getLinkageThreshold()   + " &rarr; "+ pidType.getPatientId().getValue()
							//								);
							itemType.setTooltip("[" + configuretype.getDomainName() + "] " + pid.getSource() + ":" + pid.getValue());
							itemType.setItemIsSynonym(false);
							itemType.setHlevel(0);

							qdtOrigRebuilt.getPanel().get(i).getItem().add(itemType);
							/*
							boolean found = false;
							for (ItemType itemTypeFind: panelType.getItem()) {
								if (itemTypeFind.getItemKey().equals(itemType.getItemKey()))
								{
									found = true;
									break;
								}
							}
							if (!found)
								panelType.getItem().add(itemType);
							 */
						}

						//break;

					}
					//if (i==0) 
					//	qdt.getPanel().add(panelType);
				}	
			}


			for (int k=0; k < qdtOrig.getPanel().size(); k++){
				if (qdtOrigRebuilt.getPanel().get(k).getItem().isEmpty())
					qdtOrigRebuilt.getPanel().remove(k);
			}


			String resultXml  = createPatientSet(pmUtil, "/", qdtOrigRebuilt, requestXml, projectId, securityType, pmUtil.callUserConfigure(), resultOutputOptionListType);

			log.debug("Got pid sets");

			// build result xml
			//String resultXml = buildXmlResult(dataSource, conceptsType,
			//		setfinderDaoFactory);

			// to write the result xml get the result instance id by instance id

			List<QtQueryResultInstance> resultInstanceList = setfinderDaoFactory
					.getPatientSetResultDAO().getResultInstanceList(instanceId);

			resultInstanceId = resultInstanceList.get(0).getResultInstanceId();

			// writing back the result xml in the result table
			IXmlResultDao xmlResultDao = setfinderDaoFactory.getXmlResultDao();

			resultXml =buildXmlResult(resultXml,setfinderDaoFactory,qdtOrigRebuilt.getQueryName());
			/*"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
+ "<ns10:i2b2_result_envelope xmlns:ns2=\"http://www.i2b2.org/xsd/hive/pdo/1.1/\" xmlns:ns4=\"http://www.i2b2.org/xsd/cell/crc/psm/1.1/\" xmlns:ns3=\"http://www.i2b2.org/xsd/cell/crc/pdo/1.1/\" xmlns:ns9=\"http://www.i2b2.org/xsd/cell/ont/1.1/\" xmlns:ns5=\"http://www.i2b2.org/xsd/hive/msg/1.1/\" xmlns:ns6=\"http://www.i2b2.org/xsd/cell/crc/psm/analysisdefinition/1.1/\" xmlns:ns10=\"http://www.i2b2.org/xsd/hive/msg/result/1.1/\" xmlns:ns7=\"http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/\" xmlns:ns8=\"http://www.i2b2.org/xsd/cell/pm/1.1/\">"
+ "    <body>"
+ "        <ns10:result name=\"PATIENT_COUNT_XML\">"
+ "            <data type=\"int\" column=\"patient_count\">28</data>"
+ "        </ns10:result>"
+ "    </body>"
+ "</ns10:i2b2_result_envelope>"; */
			xmlResultDao.createQueryXmlResult(resultInstanceId, resultXml);

		} catch (Exception e) {
			e.printStackTrace();
			errorFlag = true;
			// write exception stack trace to the output file
			throwable = e.getCause();
			throw e;
		} finally {

			if (setfinderDaoFactory != null) {
				IQueryResultInstanceDao resultInstanceDao = setfinderDaoFactory
						.getPatientSetResultDAO();

				if (errorFlag) {
					resultInstanceDao.updatePatientSet(resultInstanceId,
							QueryStatusTypeId.STATUSTYPE_ID_ERROR, throwable
							.getMessage(), 0, 0, "");
				} else {
					resultInstanceDao.updatePatientSet(resultInstanceId,
							QueryStatusTypeId.STATUSTYPE_ID_FINISHED, 0);
				}
			}
		}

	}

	private String createPatientSet(CallPMUtil pmUtil, String client,
			QueryDefinitionType qdt, String requestXml, String projectId,
			SecurityType securityType, 			ConfigureType ptype, ResultOutputOptionListType resultOutputOptionListType) throws I2B2Exception, JAXBUtilException, AxisFault {


		CellDataType cellData = pmUtil.getCellForProject(ptype.getCellDatas(), client, "CRC");
		CallCRCUtil crcUtil = new CallCRCUtil( cellData.getUrl(), securityType,   projectId);

		return crcUtil.callQueryDefinionTypeFull(qdt,  resultOutputOptionListType);
	}



	private PidSet getPatientSet(CallPMUtil pmUtil, String client,
			QueryDefinitionType qdt, String requestXml, String projectId,
			SecurityType securityType, 			ConfigureType ptype) throws I2B2Exception, JAXBUtilException, AxisFault {

		//CellDataType cellData = pmUtil.getCellForProject(ptype.getCellDatas(), client, "ONT");

		//	I2B2RequestMessageHelper analysisRequestHelper2 = new I2B2RequestMessageHelper(
		//			queryDefinition);
		//	QueryDefinitionType qdt = analysisRequestHelper2.getQueryDefinition();
		//for (PanelType panel : qdt.getPanel()) {


		String url = null;
		for (CellDataType cdata: ptype.getCellDatas().getCellData())
		{
			if (cdata.getId().equals("ONT"))
				url = cdata.getUrl() + "getPicsureCategories";

		}


		for (int a = 0; a < qdt.getPanel().size(); a++) {
			PanelType panel = qdt.getPanel().get(a);
			for(Iterator<ItemType> i = panel.getItem().iterator(); i.hasNext(); ) {
				ItemType item = i.next();

				/*
				String url = null;
				for (CellDataType cdata: ptype.getDistributeCellDatas().getCellData())
				{
					if (cdata.getDomainName().equals(client.substring(1)) && cdata.getId().equals("ONT"))
						url = cdata.getUrl();

				}
				 */
				// call ontology to get children
				CallOntologyUtil callOntologyUtil = buildOntologyUtil(requestXml,
						projectId, securityType, url);

				ConceptsType conceptsType = null;

				try {
					conceptsType = callOntologyUtil
							.callGetCategoriesWithHttpClient(url);

					Boolean found = false;
					for (ConceptType concept: conceptsType.getConcept())
					{
						if (concept.getDomainName() != null && concept.getDomainName().equals(client.substring(1)) &&
								item.getItemKey().contains(concept.getKey())
								)
							found = true;
					}

					if (found == false)
						i.remove();
					//conceptsType = callOntologyUtil
					//		.callGetChildrenWithHttpClient(item.getItemKey());
				} catch (Exception e){
					e.printStackTrace();

				}

				//if (conceptsType == null)
				//{
				//	i.remove();
				//	System.out.println("Could not find: " +url + " for " + item.getItemKey());
				//}

			}

			//Remove panel is no items
			if (panel.getItem().size() == 0)
				qdt.getPanel().remove(a);
		}



		//cellData = pmUtil.getCellForProject(ptype.getCellDatas(), client, "CRC");

		url = null;
		for (CellDataType cdata: ptype.getDistributeCellDatas().getCellData())
		{
			if (cdata.getDomainName().equals(client.substring(1)) && cdata.getId().equals("CRC"))
				url = cdata.getUrl();

		}
		CallCRCUtil crcUtil = new CallCRCUtil( url, securityType,   projectId);

		if (qdt.getPanel().size() == 0)
			return null;
		else
			return crcUtil.callQueryDefinionType(qdt, null);
	}



	public String buildXmlResult(String resultXml, SetFinderDAOFactory sfDAOFactory, String queryName)
			throws I2B2DAOException {
		this
		.setDbSchemaName(sfDAOFactory.getDataSourceLookup()
				.getFullSchema());

		String tempTableName = "";
		PreparedStatement stmt = null;
		boolean errorFlag = false;
		String itemKey = "";
		//		Connection conn = null;
		try {
			ResultType resultType = new ResultType();
			resultType.setName("PATIENT_COUNT_XML");
			//for (ConceptType conceptType : conceptsType.getConcept()) {

			// build results



			String demoCount = null;

			CallCRCUtil callCrc = new CallCRCUtil();

			MasterInstanceResultResponseType responseType = callCrc.getMasterResponseTypeFromResponse(resultXml);

			log.debug("Got response and unmarhslled it ");
			// try to get the patient set
			String resultInstanceId = null;
			for (QueryResultInstanceType qrit: responseType.getQueryResultInstance())
			{
				qrit.setDescription("Number of patients for " + queryName);
				demoCount = Integer.toString(qrit.getSetSize());

			}
			DataType mdataType = new DataType();
			mdataType.setValue(demoCount);
			mdataType.setColumn("patient_count");
			mdataType.setType("int");
			resultType.getData().add(mdataType);

			

			edu.harvard.i2b2.crc.datavo.i2b2result.ObjectFactory of = new edu.harvard.i2b2.crc.datavo.i2b2result.ObjectFactory();
			BodyType bodyType = new BodyType();
			bodyType.getAny().add(of.createResult(resultType));
			ResultEnvelopeType resultEnvelop = new ResultEnvelopeType();
			resultEnvelop.setBody(bodyType);

			JAXBUtil jaxbUtil = CRCJAXBUtil.getJAXBUtil();

			StringWriter strWriter = new StringWriter();

			jaxbUtil.marshaller(of.createI2B2ResultEnvelope(resultEnvelop),
					strWriter);

			return strWriter.toString();

		} catch (Exception sqlEx) {
			log.error("QueryResultPatientSetGenerator.generateResult:"
					+ sqlEx.getMessage(), sqlEx);
			throw new I2B2DAOException(
					"QueryResultPatientSetGenerator.generateResult:"
							+ sqlEx.getMessage(), sqlEx);
		} 

	}


	private CallOntologyUtil buildOntologyUtil(String requestXml,
			String projectId, SecurityType securityType, String ontologyUrl) {
		CallOntologyUtil callOntologyUtil = null;
		try {
			QueryProcessorUtil qpUtil = QueryProcessorUtil.getInstance();
			//String ontologyUrl = qpUtil.getOntologyUrl();

			String getChildrenOperationName = qpUtil
					.getCRCPropertyValue("edu.harvard.i2b2.crcplugin.pb.delegate.ontology.operation.getchildren");
			String ontologyGetChildrenUrl = ontologyUrl
					+ getChildrenOperationName;
			log.debug("Ontology getChildren url from property file ["
					+ ontologyGetChildrenUrl + "]");

			//SecurityType serviceSecurityType = PMServiceAccountUtil
			//		.getServiceSecurityType(securityType.getDomain());
			// callOntologyUtil = new CallOntologyUtil(ontologyUrl, requestXml);
			callOntologyUtil = new CallOntologyUtil(ontologyGetChildrenUrl,
					securityType, projectId);
		} catch (I2B2Exception e) {
			e.printStackTrace();
		}
		return callOntologyUtil;
	}
}
