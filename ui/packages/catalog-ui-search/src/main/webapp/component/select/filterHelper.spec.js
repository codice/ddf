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
import { expect } from 'chai'

var filterHelper = require('./filterHelper.js')

function testAllSubstrings(testString, str, matchcase, expectation) {
  var i
  for (i = 0; i < testString.length; i++) {
    var filterValue = testString.substring(0, i)
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      expectation
    )
  }
}

function testWholeStrings(testString, str, matchcase, expectation) {
  expect(filterHelper.matchesFilter(testString, str, matchcase)).to.equal(
    expectation
  )
}

describe('filter attrs', () => {
  it('filter anyText', () => {
    var str = 'anyText'
    var matchcase = false

    var positiveTestStrings = ['text', 'any', 'anytext', 'ANYTEXT', 'aNyTeXt']
    var negativeTestStrings = ['test', 'anyG', 'any text', 'anyText ', 'az']

    positiveTestStrings.forEach(function(string) {
      testAllSubstrings(string, str, matchcase, true)
    })

    negativeTestStrings.forEach(function(string) {
      testWholeStrings(string, str, matchcase, false)
    })
  })

  it('filter aString.WithDots-andDashes', () => {
    var str = 'aString.WithDots-andDashes'
    var matchcase = false

    var positiveTestStrings = [
      'aString.WithDots-andDashes',
      'a',
      'string',
      'with',
      'dots',
      'and',
      'dashes',
      'withdots-',
      '-andDashes',
    ]
    var negativeTestStrings = ['az', 'thdo', 'ndDash', 'aString-']

    positiveTestStrings.forEach(function(string) {
      testAllSubstrings(string, str, matchcase, true)
    })
    negativeTestStrings.forEach(function(string) {
      testWholeStrings(string, str, matchcase, false)
    })
  })

  it('filter ALLCAPSSTRING', () => {
    var str = 'ALLCAPSSTRING'
    var matchcase = false

    var positiveTestStrings = ['ALLCAPSSTRING', 'allcapsstring']
    var negativeTestStrings = ['z', 'CA', 'STR', "ING"]

    positiveTestStrings.forEach(function(string) {
      testAllSubstrings(string, str, matchcase, true)
    })
    negativeTestStrings.forEach(function(string) {
      testWholeStrings(string, str, matchcase, false)
    })
  })

  it('filter matches case', () => {
    var str = 'aCamelCasedString'
    var matchcase = true

    var positiveTestStrings = ['aCamelCasedString', 'a', 'Camel', 'Cased', 'String']
    var negativeTestStrings = ['A', 'camel', 'cased', 'string', 'acamelcasedstring']

    positiveTestStrings.forEach(function(string) {
      testAllSubstrings(string, str, matchcase, true)
    })
    negativeTestStrings.forEach(function(string) {
      testWholeStrings(string, str, matchcase, false)
    })
  })

  it('filter spaces', () => {
    var str = 'A few strings divided by spaces'
    var matchcase = false

    var positiveTestStrings = ['A few strings divided by spaces', 'A', 'few', 'strings', 'divided', 'by', 'spaces']
    var negativeTestStrings = ['A few strings divided by spaces ', 'vid', 'rings']

    positiveTestStrings.forEach(function(string) {
      testAllSubstrings(string, str, matchcase, true)
    })
    negativeTestStrings.forEach(function(string) {
      testWholeStrings(string, str, matchcase, false)
    })
  })
})
