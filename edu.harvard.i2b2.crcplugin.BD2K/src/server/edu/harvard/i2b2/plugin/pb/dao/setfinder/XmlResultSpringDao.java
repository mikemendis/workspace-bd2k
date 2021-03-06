/*
 * Copyright (c) 2006-2007 Massachusetts General Hospital 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the i2b2 Software License v1.0 
 * which accompanies this distribution. 
 * 
 * Contributors: 
 *     Rajesh Kuttan
 */
package edu.harvard.i2b2.plugin.pb.dao.setfinder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import edu.harvard.i2b2.common.exception.I2B2DAOException;
import edu.harvard.i2b2.plugin.pb.dao.CRCDAO;
import edu.harvard.i2b2.plugin.pb.dao.DAOFactoryHelper;
import edu.harvard.i2b2.plugin.pb.datavo.DataSourceLookup;
import edu.harvard.i2b2.plugin.pb.datavo.QtQueryInstance;
import edu.harvard.i2b2.plugin.pb.datavo.QtQueryResultInstance;
import edu.harvard.i2b2.plugin.pb.datavo.QtXmlResult;

/**
 * Class to handle persistance operation of Query instance i.e. each run of
 * query is called query instance $Id: QueryInstanceSpringDao.java,v 1.4
 * 2008/04/08 19:38:24 rk903 Exp $
 * 
 * @author rkuttan
 * @see QtQueryInstance
 */
public class XmlResultSpringDao extends CRCDAO implements IXmlResultDao {

	JdbcTemplate jdbcTemplate = null;

	QtXmlResultRowMapper xmlResultMapper = new QtXmlResultRowMapper();
	private DataSourceLookup dataSourceLookup = null;

	public XmlResultSpringDao(DataSource dataSource,
			DataSourceLookup dataSourceLookup) {
		setDataSource(dataSource);
		setDbSchemaName(dataSourceLookup.getFullSchema());
		jdbcTemplate = new JdbcTemplate(dataSource);
		this.dataSourceLookup = dataSourceLookup;

	}

	/**
	 * Function to create query instance
	 * 
	 * @param queryMasterId
	 * @param userId
	 * @param groupId
	 * @param batchMode
	 * @param statusId
	 * @return query instance id
	 */
	public String createQueryXmlResult(String resultInstanceId, String xmlValue) {
		String ORACLE_SQL = "INSERT INTO "
				+ getDbSchemaName()
				+ "QT_XML_RESULT(xml_result_id,result_instance_id,xml_value) VALUES(?,?,?)";
		String SQLSERVER_SQL = "INSERT INTO " + getDbSchemaName()
				+ "QT_XML_RESULT(result_instance_id,xml_value) VALUES(?,?)";
		String SEQUENCE_ORACLE = "SELECT " + dbSchemaName
				+ "QT_SQ_QXR_XRID.nextval from dual";
		int xmlResultId = 0;
		if (dataSourceLookup.getServerType().equalsIgnoreCase(
				DAOFactoryHelper.ORACLE)) {
			xmlResultId = jdbcTemplate.queryForInt(SEQUENCE_ORACLE);
			jdbcTemplate.update(ORACLE_SQL, new Object[] { xmlResultId,
					resultInstanceId, xmlValue });
		} else if (dataSourceLookup.getServerType().equalsIgnoreCase(
				DAOFactoryHelper.POSTGRESQL)) {
			jdbcTemplate.update(SQLSERVER_SQL, new Object[] { Integer.parseInt(resultInstanceId),
					xmlValue });
		} else if (dataSourceLookup.getServerType().equalsIgnoreCase(
				DAOFactoryHelper.SQLSERVER)) {
			jdbcTemplate.update(SQLSERVER_SQL, new Object[] { resultInstanceId,
					xmlValue });
			xmlResultId = jdbcTemplate.queryForInt("SELECT @@IDENTITY");
		}

		return String.valueOf(xmlResultId);
	}

	/**
	 * Returns list of query instance for the given master id
	 * 
	 * @param queryMasterId
	 * @return List<QtQueryInstance>
	 */
	@SuppressWarnings("unchecked")
	public QtXmlResult getXmlResultByResultInstanceId(String resultInstanceId)
			throws I2B2DAOException {
		String sql = "select *  from " + getDbSchemaName()
				+ "qt_xml_result where result_instance_id = ?";
		List<QtXmlResult> queryXmlResult = jdbcTemplate.query(sql,
				new Object[] { resultInstanceId }, xmlResultMapper);
		if (queryXmlResult != null && queryXmlResult.size() > 0) {
			return queryXmlResult.get(0);
		} else {
			throw new I2B2DAOException("Query result instance  id "
					+ resultInstanceId + " not found");
		}

	}

	private static class QtXmlResultRowMapper implements RowMapper {
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			QtXmlResult xmlResult = new QtXmlResult();
			xmlResult.setXmlResultId(rs.getString("XML_RESULT_ID"));
			QtQueryResultInstance queryResultInstance = new QtQueryResultInstance();
			queryResultInstance.setResultInstanceId(rs
					.getString("RESULT_INSTANCE_ID"));
			xmlResult.setQtQueryResultInstance(queryResultInstance);
			xmlResult.setXmlValue(rs.getString("XML_VALUE"));
			return xmlResult;
		}
	}

}
