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

import React from 'react'
const RawExample = require('!raw-loader!./params')

module.exports = BaseGuideView.extend({
  tagName: CustomElements.register('dev-region-guide'),
  jsxTemplate: RawExample,
  template() {
    return (
      <React.Fragment>
        {' '}
        {/* surround with multiple child roots with this to avoid wrapper divs */}
        <div className="section">
          <div className="is-header">Examples</div>
          <div className="examples is-list has-list-highlighting">
            <div className="is-medium-font example-title">Region Behavior</div>
            <div className="editor" data-js="jsxTemplate" data-raw="true" />
          </div>
        </div>
        <div className="section">
          <div className="is-header">When to Use</div>
          <div className="is-medium-font">
            <div className="section">
              The region behavior is the preferred way of utilizing regions in
              Marionette going forward.
            </div>
            <div className="section">
              An alternative to default marionette regions that allows
              rerendering without loss of regions. At it's core, this depends on
              the react renderer to work since it can handle reconciliation.
            </div>
            <div className="section">
              This behavior works across handlebars and JSX templates, but try
              to transition to JSX if possible.
            </div>
            <div className="section">
              If you're dealing with a handlebars template and want to avoid
              other elements rerendering when you render and remove some part of
              the DOM, wrap that part of the DOM that sometimes disappears in a
              wrapper element such as a div. Alternatively, use JSX and you're
              free to disappear parts of the dom with much lower probability
              that untouched parts of the DOM get rerendered as well.
            </div>
          </div>
        </div>
        <div className="section">
          <div className="is-header">How to Use</div>
          <div className="is-medium-font">
            <div className="section">
              Import the region.behavior into your view. Then utilize it as seen
              above.
            </div>
            <div className="section">
              See the workspace-item component and the workspace-templates
              components to get a better idea of how this is used.
            </div>
          </div>
        </div>
      </React.Fragment>
    )
  },
})
