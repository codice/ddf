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
/*global define*/
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const CustomElements = require('../../js/CustomElements.js')
const sources = require('../singletons/sources-instance.js')
import React from 'react'

module.exports = Marionette.ItemView.extend({
  template(props) {
    return (
      <React.Fragment key={Math.random()}>
        <div className="choice is-all is-available">
          <span className="choice-text">All Sources</span>
          <span className="choice-selected fa fa-check" />
        </div>
        {props.map(source => {
          return (
            <div
              key={source.id}
              className={`choice is-specific ${
                source.available ? 'is-available' : ''
              }`}
              data-value={source.id}
            >
              <span className="choice-text">
                {source.available ? (
                  ''
                ) : (
                  <span className="fa fa-exclamation-triangle" />
                )}
                <span
                  className={`fa source-icon ${
                    source.local ? 'fa-home' : 'fa-cloud'
                  }`}
                />
                {source.id}
              </span>
            </div>
          )
        })}
      </React.Fragment>
    )
  },
  tagName: CustomElements.register('query-src'),
  className: 'is-action-list',
  modelEvents: {
    change: 'render',
  },
  events: {
    'click .choice:not(.is-all)': 'handleChoice',
    'click .choice.is-all': 'handleChoiceAll',
  },
  ui: {},
  initialize: function() {},
  onRender: function() {
    this.handleValue()
  },
  handleValue: function() {
    switch (this.model.get('federation')) {
      case 'enterprise':
        this.$el.find('.choice.is-all').addClass('is-selected')
        break
      case 'selected':
        var srcs = this.model.get('value')
        srcs.forEach(
          function(src) {
            this.$el.find('[data-value="' + src + '"]').addClass('is-selected')
          }.bind(this)
        )
        break
      default:
        break
    }
  },
  handleChoiceAll: function(e) {
    $(e.currentTarget).toggleClass('is-selected')
    this.model.set({
      federation:
        this.model.get('federation') === 'enterprise'
          ? 'selected'
          : 'enterprise',
    })
  },
  handleChoice: function(e) {
    $(e.currentTarget).toggleClass('is-selected')
    this.updateValue()
  },
  updateValue: function() {
    var srcs = _.map(this.$el.find('.is-specific.is-selected'), function(
      choice
    ) {
      return $(choice).attr('data-value')
    })
    this.model.set({
      value: srcs,
      federation: 'selected',
    })
  },
  serializeData: function() {
    return sources.toJSON()
  },
})
