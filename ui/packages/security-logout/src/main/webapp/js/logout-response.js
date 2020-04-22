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
/* global $ */
// eslint-disable-next-line no-extra-semi
;(function() {
  var redirectUrl = window.location.href.replace(/\/logout\/.*/, '')
  var searchString = (window.location.search + '').split('?')
  var mustCloseBrowser = false

  if (searchString[1] !== undefined) {
    var searchParams = searchString[1].split('&')

    searchParams.forEach(function(paramString) {
      var param = paramString.split('=')

      if (param[0] === 'mustCloseBrowser') {
        // eslint-disable-next-line no-undef
        mustCloseBrowser = true
      } else if (param[0] === 'prevurl') {
        // eslint-disable-next-line no-undef
        redirectUrl = param[1]
      }
      //add additional params here if needed
    })
  }

  if (mustCloseBrowser) {
    $('#close-browser-msg').show()
    return
  }

  $('#link').attr('href', decodeUrl(redirectUrl))
  $('#standard-msg').show()
})()

function decodeUrl(url) {
  if (url.indexOf('%') !== -1) {
    return decodeURIComponent(url)
  }

  return url
}
