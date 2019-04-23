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
const React = require('react')

const withAdapter = Component =>
  class extends React.Component {
    constructor(props) {
      super(props)
      this.state = props.model.toJSON()
    }
    setModelState() {
      this.setState(this.props.model.toJSON())
    }
    componentWillMount() {
      this.props.model.on('change', this.setModelState, this)
    }
    componentWillUnmount() {
      this.props.model.off('change', this.setModelState)
    }
    render() {
      return (
        <Component
          state={this.state}
          options={this.props.options}
          setState={(...args) => this.props.model.set(...args)}
        />
      )
    }
  }

const LocationInput = withAdapter(require('./location'))

const Marionette = require('marionette')
const _ = require('underscore')
const CustomElements = require('../../js/CustomElements.js')
const LocationNewModel = require('./location-new')

module.exports = Marionette.LayoutView.extend({
  template() {
    return (
      <div className="location-input">
        <LocationInput model={this.model} />
      </div>
    )
  },
  tagName: CustomElements.register('location-new'),
  initialize(options) {
    this.propertyModel = this.model
    this.model = new LocationNewModel()
    _.bindAll.apply(_, [this].concat(_.functions(this))) // underscore bindAll does not take array arg
  },
  getCurrentValue() {
    return this.model.getValue()
  },
  isValid() {
    return this.model.isValid()
  },
})
