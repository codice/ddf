/*jslint node: true */
/* global describe, it, require */
'use strict'

var shared = require('./shared')

describe('external authentication Login', function() {
  shared.setup(this)
  it("should have 'Logout button", function() {
    return this.browser
      .waitForElementById('signin', shared.timeout)
      .click()

      .waitForElementByClassName('btn-logout', shared.timeout)
      .takeScreenshot()
      .saveScreenshot(
        shared.getPathForScreenshot('sign-in-with-credentials.png')
      )
  })
})
