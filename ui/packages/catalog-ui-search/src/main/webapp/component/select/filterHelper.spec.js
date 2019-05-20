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

function testAllSubstrings(
  testFilterString,
  stringToEval,
  matchcase,
  expectation
) {
  for (let i = 0; i < testFilterString.length; i++) {
    const filterValue = testFilterString.substring(0, i)
    expect(
      filterHelper.matchesFilter(filterValue, stringToEval, matchcase)
    ).to.equal(expectation)
  }
}

function testWholeStrings(
  testFilterString,
  stringToEval,
  matchcase,
  expectation
) {
  expect(
    filterHelper.matchesFilter(testFilterString, stringToEval, matchcase)
  ).to.equal(expectation)
}

describe('filter helper functions', () => {
  it('filter anyText', () => {
    const stringToEval = 'anyText'
    const matchcase = false

    const positiveTestStrings = ['text', 'any', 'anytext', 'ANYTEXT', 'aNyTeXt']
    const negativeTestStrings = ['test', 'anyG', 'any text', 'anyText ', 'az']

    positiveTestStrings.forEach(testFilterString => {
      testAllSubstrings(testFilterString, stringToEval, matchcase, true)
    })

    negativeTestStrings.forEach(testFilterString => {
      testWholeStrings(testFilterString, stringToEval, matchcase, false)
    })
  })

  it('filter aString.WithDots-andDashes', () => {
    const stringToEval = 'aString.WithDots-andDashes'
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

    positiveTestStrings.forEach(testFilterString => {
      testAllSubstrings(testFilterString, stringToEval, matchcase, true)
    })
    negativeTestStrings.forEach(testFilterString => {
      testWholeStrings(testFilterString, stringToEval, matchcase, false)
    })
  })

  it('filter ALLCAPSSTRING', () => {
    const stringToEval = 'ALLCAPSSTRING'
    const matchcase = false

    const positiveTestStrings = ['ALLCAPSSTRING', 'allcapsstring']
    const negativeTestStrings = ['z', 'CA', 'STR', 'ING']

    positiveTestStrings.forEach(testFilterString => {
      testAllSubstrings(testFilterString, stringToEval, matchcase, true)
    })
    negativeTestStrings.forEach(testFilterString => {
      testWholeStrings(testFilterString, stringToEval, matchcase, false)
    })
  })

  it('filter matches case', () => {
    const stringToEval = 'aCamelCasedString'
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

    positiveTestStrings.forEach(testFilterString => {
      testAllSubstrings(testFilterString, stringToEval, matchcase, true)
    })
    negativeTestStrings.forEach(testFilterString => {
      testWholeStrings(testFilterString, stringToEval, matchcase, false)
    })
  })

  it('can filter spaces', () => {
    const stringToEval = 'A few strings divided by spaces'
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

    positiveTestStrings.forEach(testFilterString => {
      testAllSubstrings(testFilterString, stringToEval, matchcase, true)
    })
    negativeTestStrings.forEach(testFilterString => {
      testWholeStrings(testFilterString, stringToEval, matchcase, false)
    })
  })

  // Note that dot "." cannot be escaped since it's one of the attribute name delimiters AND a regex symbol
  it('treat regex literals no differently except dot', () => {
    const regexSymbols = [
      '\\',
      '*',
      '+',
      '^',
      '$',
      '?',
      '(',
      ')',
      '[',
      ']',
      '|',
      '{',
      '}',
    ]
    regexSymbols.forEach(symbol => {
      testWholeStrings(symbol, symbol, false, true)
    })
  })
})
