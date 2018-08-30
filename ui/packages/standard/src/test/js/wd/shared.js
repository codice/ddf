/*jslint node: true */
/* global after, before, beforeEach, afterEach, require */
'use strict'

require('colors')
var argv = require('yargs').argv
var chai = require('chai')
var chaiAsPromised = require('chai-as-promised')
var wd = require('wd')
var fs = require('fs')
var path = require('path')
var newline = '\n'
var indentation = ''
var stackTrace = []
var browser
var failureScreenshotPath = path.join(
  __dirname,
  '../../../..',
  'target/webapp/images/failures'
)
var screenshotPath = path.join(__dirname, '../../../..', 'target/webapp/images')
/*
 Called when a test fails.  It constructs a screenshot name by removing problematic characters from
 the full test name.  It then prints out the stack trace of commands and saves a screenshot to the
 failure directory.
 */
function handleError(testName) {
  var screenshotName =
    testName
      .split(' ')
      .join('-')
      .split('/')
      .join('-')
      .split(':')
      .join('-') + '.png'
  console.error(
    indentation +
      screenshotName.red +
      ' saved in target/webapp/images/failures.'.blue
  )
  while (stackTrace.length > 0) {
    console.error(indentation + stackTrace.pop().red)
  }
  return browser.saveScreenshot(
    path.join(failureScreenshotPath, screenshotName)
  )
}
/*
 Called whenever a command is issued to the browser.  Depending on the eventType (and command),
 it constructs a message to put into the stack trace in case the test fails.  It is only
 added if it is not equal to the previous command and the command before that.  This prevents
 duplicate commands (in the case of our polling commands) and results in an easy to read
 stack trace.
 */
function handleCommand(eventType, command, response) {
  var message = eventType
  switch (eventType) {
    case 'CALL':
      message += '     ' + command
      break
    case 'RESPONSE':
      message += ' ' + command
      if (
        command.indexOf('saveScreenshot') !== 0 &&
        command.indexOf('takeScreenshot') !== 0
      )
        message += ' ' + response
      break
  }
  if (
    stackTrace[stackTrace.length - 1] !== message &&
    stackTrace[stackTrace.length - 2] !== message
  )
    stackTrace.push(message)
}

fs.mkdir(screenshotPath, function() {
  fs.mkdir(failureScreenshotPath)
})

wd.addPromiseChainMethod('logToConsole', function(message) {
  console.log(indentation + message.toString().blue)
})

exports.screenshotPath = screenshotPath

chai.use(chaiAsPromised)
chai.should()
chaiAsPromised.transferPromiseness = wd.transferPromiseness

function debugLogging(browser) {
  // optional extra logging
  if (browser.on) {
    browser.on('status', function(info) {
      console.log(info.cyan)
    })
    browser.on('command', function(eventType, command, response) {
      if (command === 'takeScreenshot()') {
        response = ''
      }
      console.log(' > ' + eventType.cyan, command, (response || '').grey)
    })
    browser.on('http', function(meth, path, data) {
      console.log(' > ' + meth.magenta, path, (data || '').grey)
    })
  }
}

exports.getPathForScreenshot = function(filename) {
  return path.join(screenshotPath, filename)
}

exports.asserters = wd.asserters

exports.iterations = argv.iterations || 1

exports.setup = function() {
  before(function() {
    exports.url = 'http://localhost:' + this.mochaOptions.expressPort
    exports.timeout = argv.timeout || this.mochaOptions.timeout || 30000

    // need reference in order to take screenshots
    browser = this.browser
    // remove listener before adding since this is called before each test suites
    this.browser
      .removeListener('command', handleCommand)
      .on('command', handleCommand)

    if (argv.browser) {
      this.browser = wd
        .promiseChainRemote()
        .init({ browserName: argv.browser || 'chrome' })
    }

    if (argv.verbose) {
      debugLogging(this.browser)
    }

    return this.browser
      .setAsyncScriptTimeout(exports.timeout)
      .setWindowSize(1080, 1080)
      .get(argv.url || exports.url)
  })

  /*
     We clear out the stackTrace of commands before each individual test.
     Unfortunately, we have to use this hacky way (:'s in describes) of getting the proper indentation
     for logging to the console.  Mocha hides the details of tests so that we don't know how deep
     we are (we can get the fullTitle through a function, but we can't access the parent ourselves).
     */
  beforeEach(function() {
    stackTrace = []
    var numOfIndents = this.currentTest.fullTitle().split(':').length
    var indent = '  '
    indentation = ''
    for (var i = 0; i < numOfIndents; i++) {
      indentation += indent
    }
    console.log(
      newline + indentation + this.currentTest.title.blue + ' . . .'.blue
    )
  })

  /*
     Pass 'Error' back into the done callback so further tests in the given suite are aborted
     If we decoupled our tests from one another (cleaned up properly after each one),
     this wouldn't be necessary.
     */
  afterEach(function(done) {
    if (this.currentTest.err) {
      console.error(indentation + this.currentTest.err.toString().red)
      handleError(this.currentTest.fullTitle()).then(
        function() {
          done('Error')
        },
        function() {
          done('Error')
        }
      )
    } else {
      done()
    }
  })

  after(function() {
    if (argv.browser) {
      return this.browser.quit()
    } else {
      return this.browser
    }
  })
}
