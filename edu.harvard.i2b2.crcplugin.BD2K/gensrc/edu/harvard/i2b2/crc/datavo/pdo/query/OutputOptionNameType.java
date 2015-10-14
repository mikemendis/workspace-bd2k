//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-558 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.09.28 at 01:40:19 PM EDT 
//


package edu.harvard.i2b2.crc.datavo.pdo.query;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for outputOptionNameType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="outputOptionNameType">
 *   &lt;restriction base="{http://www.i2b2.org/xsd/cell/crc/pdo/1.1/}tokenType">
 *     &lt;enumeration value="none"/>
 *     &lt;enumeration value="asattributes"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "outputOptionNameType")
@XmlEnum
public enum OutputOptionNameType {


    /**
     * Intags
     * 
     */
    @XmlEnumValue("none")
    NONE("none"),

    /**
     * As attributes
     * 
     */
    @XmlEnumValue("asattributes")
    ASATTRIBUTES("asattributes");
    private final String value;

    OutputOptionNameType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static OutputOptionNameType fromValue(String v) {
        for (OutputOptionNameType c: OutputOptionNameType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
