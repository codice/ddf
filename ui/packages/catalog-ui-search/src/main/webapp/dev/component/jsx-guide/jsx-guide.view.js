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
const CustomElements = require('../../../js/CustomElements.js')
const BaseGuideView = require('../base-guide/base-guide.view.js')

const Example = require('./example.view')
const RawExample = require('!raw-loader!./example.view.js')
import React from 'react'

module.exports = BaseGuideView.extend({
  tagName: CustomElements.register('dev-jsx-guide'),
  regions: {
    jsx: '.example.jsx',
  },
  jsxTemplate: RawExample,
  onRender() {
    this.jsx.show(new Example())
  },
  template() {
    return (
      <React.Fragment>
        {' '}
        {/* surround with multiple child roots with this to avoid wrapper divs */}
        <div className="section">
          <div className="is-header">Examples</div>
          <div className="examples is-list has-list-highlighting">
            <div className="is-medium-font example-title">JSX template</div>
            <div className="jsx is-list is-inline example" />
            <div className="editor" data-js="jsxTemplate" data-raw="true" />
          </div>
        </div>
        <div className="section">
          <div className="is-header">When to Use</div>
          <div className="is-medium-font">
            This is preferred compared to handlebars going forward. It gives us
            a lot more flexibility when rendering, and as we transition to react
            it'll mean less code that we need to rewrite.
          </div>
        </div>
        <div className="section">
          <div className="is-header">How to Use</div>
          <div>
            <div className="is-medium-font">
              Import 'React' into your view, then simply have your template be a
              function and return jsx (wrapped in the React.Fragment element).
              You can use `this` to access members of the view (such as
              methods), and the template function also gets pass the result of
              serializeData (model.toJSON() by default). Notice that you have to
              wrap children in `React.Fragment` elements if they are at the
              root. Also notice that when looping you need to set a key on the
              element to avoid a warning. This is important to do as we
              transition to react, since it helps react make smart decisions.
              Make sure the key is unique!
            </div>
          </div>
        </div>
      </React.Fragment>
    )
  },
})
