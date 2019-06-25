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
/**
 ICanHandlebarz.js is Copyright (c) 2011 Mitchell Johnson and is MIT licensed.

 ---------------------------------------------------------------------
 Copyright (c) 2010 Henrik Joreteg (ICanHaz.js)
 Copyright (C) 2011 by Yehuda Katz (Handlebars.js)

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ---------------------------------------------------------------------
 **/

define(['handlebars', 'jquery'], function(Handlebars, $) {
  'use strict'
  var ich = {}

  ich.templates = {}

  // public function for adding templates
  // We're enforcing uniqueness to avoid accidental template overwrites.
  // If you want a different template, it should have a different name.
  ich.addTemplate = function(name, templateString) {
    if (ich[name]) throw 'Invalid name: ' + name + '.'
    if (ich.templates[name]) throw 'Template " + name + " exists'

    ich.templates[name] = Handlebars.compile(templateString)
    ich[name] = function(data, raw) {
      data = data || {}
      var result = ich.templates[name](data)
      return raw ? result : $(result)
    }
  }

  // public function for adding partials
  ich.addPartial = function(name, templateString) {
    if (Handlebars.partials[name]) throw 'Partial " + name + " exists'
    Handlebars.registerPartial(name, templateString)
  }

  ich.addHelper = function(name, func) {
    if (Handlebars.helpers[name]) throw 'Helper " + name + " exists'
    if (typeof func === 'function') {
      Handlebars.registerHelper(name, func)
    }
  }

  return ich
})
