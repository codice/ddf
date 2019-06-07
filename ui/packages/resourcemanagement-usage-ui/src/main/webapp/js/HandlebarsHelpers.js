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

define(['icanhaz', 'jquery'], function(ich, $) {
  'use strict'
  var helper,
    helpers = {
      select: function(value, options) {
        var $el = $('<select />').html(options.fn(this))
        $el.find('[value="' + value + '"]').attr({ selected: 'selected' })
        return $el.html()
      },
      gt: function(value, test, options) {
        if (value > test) {
          return options.fn(this)
        } else {
          return options.inverse(this)
        }
      },
    }

  for (helper in helpers) {
    if (helpers.hasOwnProperty(helper)) {
      ich.addHelper(helper, helpers[helper])
    }
  }
})
