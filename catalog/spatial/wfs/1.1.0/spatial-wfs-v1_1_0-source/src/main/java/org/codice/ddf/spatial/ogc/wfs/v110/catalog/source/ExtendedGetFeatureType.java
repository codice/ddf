/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.source;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import net.opengis.wfs.v_1_1_0.GetFeatureType;
import org.jvnet.jaxb2_commons.lang.CopyStrategy2;
import org.jvnet.jaxb2_commons.lang.CopyTo2;
import org.jvnet.jaxb2_commons.lang.Equals2;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy2;
import org.jvnet.jaxb2_commons.lang.HashCode2;
import org.jvnet.jaxb2_commons.lang.HashCodeStrategy2;
import org.jvnet.jaxb2_commons.lang.MergeFrom2;
import org.jvnet.jaxb2_commons.lang.MergeStrategy2;
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.util.LocatorUtils;

@XmlRootElement(namespace = "http://www.opengis.net/wfs", name = "GetFeature")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetFeatureType")
public class ExtendedGetFeatureType extends GetFeatureType
    implements Cloneable, CopyTo2, Equals2, HashCode2, MergeFrom2, ToString2 {

  @XmlAttribute(name = "startIndex")
  @XmlSchemaType(name = "positiveInteger")
  protected BigInteger startIndex;

  /**
   * Gets the value of the startIndex property.
   *
   * @return possible object is {@link BigInteger }
   */
  public BigInteger getStartIndex() {
    return startIndex;
  }

  /**
   * Sets the value of the startIndex property.
   *
   * @param value allowed object is {@link BigInteger }
   */
  public void setStartIndex(BigInteger value) {
    this.startIndex = value;
  }

  public boolean isSetStartIndex() {
    return (this.startIndex != null);
  }

  public StringBuilder appendFields(
      ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
    super.appendFields(locator, buffer, strategy);
    {
      BigInteger theStartIndex;
      theStartIndex = this.getStartIndex();
      strategy.appendField(
          locator, this, "startIndex", buffer, theStartIndex, this.isSetStartIndex());
    }
    return buffer;
  }

  public boolean equals(
      ObjectLocator thisLocator,
      ObjectLocator thatLocator,
      Object object,
      EqualsStrategy2 strategy) {
    if ((object == null) || (this.getClass() != object.getClass())) {
      return false;
    }
    if (this == object) {
      return true;
    }
    if (!super.equals(thisLocator, thatLocator, object, strategy)) {
      return false;
    }
    final ExtendedGetFeatureType that = ((ExtendedGetFeatureType) object);
    {
      BigInteger lhsStartIndex;
      lhsStartIndex = this.getStartIndex();
      BigInteger rhsStartIndex;
      rhsStartIndex = that.getStartIndex();
      if (!strategy.equals(
          LocatorUtils.property(thisLocator, "startIndex", lhsStartIndex),
          LocatorUtils.property(thatLocator, "startIndex", rhsStartIndex),
          lhsStartIndex,
          rhsStartIndex,
          this.isSetStartIndex(),
          that.isSetStartIndex())) {
        return false;
      }
    }
    return true;
  }

  public int hashCode(ObjectLocator locator, HashCodeStrategy2 strategy) {
    int currentHashCode = super.hashCode(locator, strategy);
    {
      BigInteger theStartIndex;
      theStartIndex = this.getStartIndex();
      currentHashCode =
          strategy.hashCode(
              LocatorUtils.property(locator, "startIndex", theStartIndex),
              currentHashCode,
              theStartIndex,
              this.isSetStartIndex());
    }
    return currentHashCode;
  }

  public Object copyTo(ObjectLocator locator, Object target, CopyStrategy2 strategy) {
    final Object draftCopy = ((target == null) ? createNewInstance() : target);
    super.copyTo(locator, draftCopy, strategy);
    if (draftCopy instanceof ExtendedGetFeatureType) {
      final ExtendedGetFeatureType copy = ((ExtendedGetFeatureType) draftCopy);
      {
        Boolean startIndexShouldBeCopiedAndSet =
            strategy.shouldBeCopiedAndSet(locator, this.isSetStartIndex());
        if (startIndexShouldBeCopiedAndSet == Boolean.TRUE) {
          BigInteger sourceStartIndex;
          sourceStartIndex = this.getStartIndex();
          BigInteger copyStartIndex =
              ((BigInteger)
                  strategy.copy(
                      LocatorUtils.property(locator, "startIndex", sourceStartIndex),
                      sourceStartIndex,
                      this.isSetStartIndex()));
          copy.setStartIndex(copyStartIndex);
        } else {
          if (startIndexShouldBeCopiedAndSet == Boolean.FALSE) {
            copy.startIndex = null;
          }
        }
      }
    }
    return draftCopy;
  }

  public void mergeFrom(
      ObjectLocator leftLocator,
      ObjectLocator rightLocator,
      Object left,
      Object right,
      MergeStrategy2 strategy) {
    super.mergeFrom(leftLocator, rightLocator, left, right, strategy);
    if (right instanceof ExtendedGetFeatureType) {
      final ExtendedGetFeatureType target = this;
      final ExtendedGetFeatureType leftObject = ((ExtendedGetFeatureType) left);
      final ExtendedGetFeatureType rightObject = ((ExtendedGetFeatureType) right);
      {
        Boolean startIndexShouldBeMergedAndSet =
            strategy.shouldBeMergedAndSet(
                leftLocator,
                rightLocator,
                leftObject.isSetStartIndex(),
                rightObject.isSetStartIndex());
        if (startIndexShouldBeMergedAndSet == Boolean.TRUE) {
          BigInteger lhsStartIndex;
          lhsStartIndex = leftObject.getStartIndex();
          BigInteger rhsStartIndex;
          rhsStartIndex = rightObject.getStartIndex();
          BigInteger mergedStartIndex =
              ((BigInteger)
                  strategy.merge(
                      LocatorUtils.property(leftLocator, "startIndex", lhsStartIndex),
                      LocatorUtils.property(rightLocator, "startIndex", rhsStartIndex),
                      lhsStartIndex,
                      rhsStartIndex,
                      leftObject.isSetStartIndex(),
                      rightObject.isSetStartIndex()));
          target.setStartIndex(mergedStartIndex);
        } else {
          if (startIndexShouldBeMergedAndSet == Boolean.FALSE) {
            target.startIndex = null;
          }
        }
      }
    }
  }

  public GetFeatureType withStartIndex(BigInteger value) {
    setStartIndex(value);
    return this;
  }
}
