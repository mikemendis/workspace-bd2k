//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-558 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.09.28 at 01:40:19 PM EDT 
//


package edu.harvard.i2b2.crc.datavo.pdo.query;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * requestType is abstract so a concrete type must be declared in the
 *                 instance document using the xsi:type attribute.
 * 
 * <p>Java class for requestType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="requestType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "requestType")
@XmlSeeAlso({
    GetConceptByPrimaryKeyRequestType.class,
    GetObserverByPrimaryKeyRequestType.class,
    GetObservationFactByPrimaryKeyRequestType.class,
    GetPDOFromInputListRequestType.class,
    GetPatientByPrimaryKeyRequestType.class,
    GetModifierByPrimaryKeyRequestType.class,
    GetPDOTemplateRequestType.class,
    GetEventByPrimaryKeyRequestType.class
})
public abstract class RequestType {


}