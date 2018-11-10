/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global require, window*/

/*
    This file exists to address inabilities of current media queries.  Specifically, it's impossible to 
    have a media query of screen size based upon rem.  The browser will only do an initial conversion of 
    rem to px upon page load (using the browser's font size setting).  This means that media queries will
    not function completely as intended.
*/
var _ = require('underscore')
var $ = require('jquery')
var user = require('../component/singletons/user-instance.js')

//in rem based on 16px base font size => 420 / 16
var mobileScreenSize = 26.25 //420 px
var smallScreenSize = 58.75 //940 px
var mediumScreenSize = 90 //1440 px

var updateMediaQueries = _.throttle(function() {
  var $html = $('html')
  var fontSize = parseInt(
    user
      .get('user')
      .get('preferences')
      .get('fontSize')
  )
  var screenSize = window.innerWidth / fontSize
  var mobile = screenSize < mobileScreenSize
  var small = screenSize < smallScreenSize && !mobile
  var medium = screenSize < mediumScreenSize && !small && !mobile
  $html.toggleClass('is-mobile-screen', mobile)
  $html.toggleClass('is-small-screen', small)
  $html.toggleClass('is-medium-screen', medium)
}, 30)

$(window).resize(updateMediaQueries)
user
  .get('user')
  .get('preferences')
  .on('change:fontSize', updateMediaQueries)
updateMediaQueries()
