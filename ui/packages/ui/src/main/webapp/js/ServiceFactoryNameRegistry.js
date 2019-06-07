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

define(['js/MetatypeRegistry'], function(MetatypeRegistry) {
  return {
    getName: function(properties) {
      switch (properties.get('service.factoryPid')) {
        case 'org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor':
          var processingMechanism = MetatypeRegistry.getRelevantOptionLabel({
            metatypeId: properties.get('service.factoryPid'),
            property: 'processingMechanism',
            value: properties.get('processingMechanism'),
          })
          return (
            processingMechanism +
            ' | ' +
            properties.get('monitoredDirectoryPath')
          )
        default:
          return null
      }
    },
  }
})
