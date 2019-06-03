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

const filterHelper = require('./filterHelper.js')

function testAllSubstrings(testString, str, matchcase, expectation) {
  let i
  for (i = 0; i < testString.length; i++) {
    const filterValue = testString.substring(0, i)
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
    const str = 'anyText'
    const matchcase = false

    const positiveTestStrings = ['text', 'any', 'anytext', 'ANYTEXT', 'aNyTeXt']
    const negativeTestStrings = ['test', 'anyG', 'any text', 'anyText ', 'az']

    positiveTestStrings.forEach(string => {
      testAllSubstrings(string, str, matchcase, true)
    })

    negativeTestStrings.forEach(string => {
      testWholeStrings(string, str, matchcase, false)
    })
  })

  it('filter aString.WithDots-andDashes', () => {
    const str = 'aString.WithDots-andDashes'
    const matchcase = false

    const positiveTestStrings = [
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
    const negativeTestStrings = ['az', 'thdo', 'ndDash', 'aString-']

    positiveTestStrings.forEach(string => {
      testAllSubstrings(string, str, matchcase, true)
    })
    negativeTestStrings.forEach(string => {
      testWholeStrings(string, str, matchcase, false)
    })
  })

  it('filter ALLCAPSSTRING', () => {
    const str = 'ALLCAPSSTRING'
    const matchcase = false

    const positiveTestStrings = ['ALLCAPSSTRING', 'allcapsstring']
    const negativeTestStrings = ['z', 'CA', 'STR', 'ING']

    positiveTestStrings.forEach(string => {
      testAllSubstrings(string, str, matchcase, true)
    })
    negativeTestStrings.forEach(string => {
      testWholeStrings(string, str, matchcase, false)
    })
  })

  it('filter matches case', () => {
    const str = 'aCamelCasedString'
    const matchcase = true

    const positiveTestStrings = [
      'aCamelCasedString',
      'a',
      'Camel',
      'Cased',
      'String',
    ]
    const negativeTestStrings = [
      'A',
      'camel',
      'cased',
      'string',
      'acamelcasedstring',
    ]

    positiveTestStrings.forEach(string => {
      testAllSubstrings(string, str, matchcase, true)
    })
    negativeTestStrings.forEach(string => {
      testWholeStrings(string, str, matchcase, false)
    })
  })

  it('filter spaces', () => {
    const str = 'A few strings divided by spaces'
    const matchcase = false

    const positiveTestStrings = [
      'A few strings divided by spaces',
      'A',
      'few',
      'strings',
      'divided',
      'by',
      'spaces',
    ]
    const negativeTestStrings = [
      'A few strings divided by spaces ',
      'vid',
      'rings',
    ]

    positiveTestStrings.forEach(string => {
      testAllSubstrings(string, str, matchcase, true)
    })
    negativeTestStrings.forEach(string => {
      testWholeStrings(string, str, matchcase, false)
    })
  })
})
