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
// NOTE: to enable debug uncomment the following 2 lines.
//casper.options.verbose = true;
//casper.options.logLevel = 'debug';
casper.test.begin('Login test', 1, function(test) {
  var url = casper.cli.get('url')
  casper.start(url)

  casper.waitFor(
    function() {
      // Make sure that we got the loginForm
      return this.evaluate(function() {
        return document.querySelectorAll('form[id="loginForm"]').length === 1
      })
    },
    function() {
      test.pass('Found login form')
    },
    function() {
      test.fail('Failed finding login form')
    }
  )

  casper.run(function() {
    test.done()
  })
})
