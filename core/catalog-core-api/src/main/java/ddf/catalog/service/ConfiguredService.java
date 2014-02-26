/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
 package ddf.catalog.service;
 
 /**
  * Describes a service that is created via ConfigurationAdmin.
  * 
  * @author Scott Tustison
  */
 public interface ConfiguredService {
     /**
      * Returns the PID of the configuration that corresponds to this service
      * 
      * @return The unique PID of the configuration associated with this service
      */
     public String getConfigurationPid();
 
     /**
      * Sets the PID of this service's corresponding configuration
      * 
      * @param configurationPid
      *            The unique PID of the configuration associated with this service
      */
     public void setConfigurationPid(String configurationPid);
 }