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

define(['backbone'], function(Backbone) {
  let Organization = {}

  Organization = Backbone.Model.extend({
    // Defaults set to 'Not Provided' to indicate the software correctly pulled the Organizational Info
    // but the user did not enter any. N/A was determined to be too vague
    defaults: {
      name: 'Not Provided',
      address: 'Not Provided',
      phoneNumber: 'Not Provided',
      emailAddress: 'Not Provided',
    },

    initializeFromOrganization: function(org) {
      this.name = org.name
      this.address = org.address
      this.phoneNumber = org.phoneNumber
      this.emailAddress = org.emailAddress
    },
  })

  return Organization
})
