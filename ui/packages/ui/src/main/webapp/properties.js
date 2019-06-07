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

define(['jquery'], function($) {
  'use strict'

  var properties = {
    /* these are empty until loaded from ajax */
    ui: {},
    admin: {},

    init: function() {
      // use this function to initialize variables that rely on others

      var props = this

      $.ajax({
        async: false, // must be synchronous to guarantee that no tests are run before fixture is loaded
        cache: false,
        dataType: 'json',
        url: '../services/platform/config/ui',
      })
        .done(function(uiConfig) {
          props.ui = uiConfig
          return props
        })
        .fail(function(jqXHR, status, errorThrown) {
          if (console) {
            console.log(
              'Platform UI Configuration could not be loaded: (status: ' +
                status +
                ', message: ' +
                errorThrown.message +
                ')'
            )
          }
        })

      $.ajax({
        async: false, // must be synchronous to guarantee that no tests are run before fixture is loaded
        cache: false,
        dataType: 'json',
        url: '../services/admin/config',
      })
        .done(function(adminConfig) {
          props.admin = adminConfig
          return props
        })
        .fail(function(jqXHR, status, errorThrown) {
          if (console) {
            console.log(
              'Admin UI Configuration could not be loaded: (status: ' +
                status +
                ', message: ' +
                errorThrown.message +
                ')'
            )
          }
        })

      return props
    },
  }

  return properties.init()
})
