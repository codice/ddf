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
const Marionette = require('marionette')
const template = require('./guide.hbs')
const CustomElements = require('../../../js/CustomElements.js')
const PropertyView = require('../../../component/property/property.view.js')
const Property = require('../../../component/property/property.js')
const CardGuideView = require('../card-guide/card-guide.view.js')
const ButtonGuideView = require('../button-guide/button-guide.view.js')
const StaticDropdownGuideView = require('../static-dropdown-guide/static-dropdown-guide.view.js')
const DropdownGuideView = require('../dropdown-guide/dropdown-guide.view.js')
const InputGuideView = require('../input-guide/input-guide.view.js')
const JSXGuideView = require('../jsx-guide/jsx-guide.view.js')
const RegionGuideView = require('../region-guide/region-guide.view.js')
import Button from '../button'
import Checkbox from '../checkbox'
import Text from '../text'
import Range from '../range'
import DateComponent from '../date'
import Location from '../location'
import Thumbnail from '../thumbnail'
import Geometry from '../geometry'
import NumberComponent from '../number'
import Color from '../color'
import Autocomplete from '../autocomplete'
import Enum from '../enum'
import InputWithParam from '../input-with-param'
import Textarea from '../textarea'
import MultiEnum from '../multi-enum'
import Password from '../password'
import MarionetteRegionContainer from '../../../react-component/container/marionette-region-container'
import React from 'react'

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('dev-guide'),
  className: 'pad-bottom',
  initialize() {
    this.componentGuideModel = new Property({
      enumFiltering: true,
      showLabel: false,
      value: ['Card'],
      isEditing: true,
      enum: [
        {
          label: 'Card (Marionette)',
          value: 'Card',
        },
        {
          label: 'Button (Marionette)',
          value: 'Button',
        },
        {
          label: 'Button (react)',
          value: 'ButtonReact',
        },
        {
          label: 'Checkbox',
          value: 'checkbox',
        },
        {
          label: 'Text',
          value: 'text',
        },
        {
          label: 'Range',
          value: 'range',
        },
        {
          label: 'Date',
          value: 'date',
        },
        {
          label: 'Location',
          value: 'location',
        },
        {
          label: 'Thumbnail',
          value: 'thumbnail',
        },
        {
          label: 'Geometry',
          value: 'geometry',
        },
        {
          label: 'Number',
          value: 'number',
        },
        {
          label: 'Autocomplete',
          value: 'autocomplete',
        },
        {
          label: 'Enum',
          value: 'enum',
        },
        {
          label: 'MultiEnum',
          value: 'multienum',
        },
        {
          label: 'Text Area',
          value: 'textarea',
        },
        {
          label: 'Input with Param',
          value: 'inputwithparam',
        },
        {
          label: 'Color',
          value: 'color',
        },
        {
          label: 'Static Dropdowns (deprecated)',
          value: 'Static Dropdowns',
        },
        {
          label: 'Dropdowns (Marionette)',
          value: 'Dropdowns',
        },
        {
          label: 'Inputs (Marionette)',
          value: 'Inputs',
        },
        {
          label: 'JSX',
          value: 'JSX',
        },
        {
          label: 'Password',
          value: 'Password',
        },
        {
          label: 'Regions (Layout Views)',
          value: 'Regions',
        },
      ].sort((a, b) => a.label.localeCompare(b.label)),
      id: 'component',
    })
    this.listenTo(this.componentGuideModel, 'change:value', this.render)
  },
  template(data) {
    const ComponentToShow = this.getComponentToShow()
    return (
      <React.Fragment>
        <div className="container limit-to-center">
          <div className="section">
            <div className="is-header">Component / Pattern</div>
            <div className="component">
              <MarionetteRegionContainer
                view={PropertyView}
                viewOptions={() => {
                  return {
                    model: this.componentGuideModel,
                  }
                }}
              />
            </div>
          </div>
          {ComponentToShow.prototype._isMarionetteView ? (
            <MarionetteRegionContainer
              className="component-details"
              view={ComponentToShow}
            />
          ) : (
            <ComponentToShow />
          )}
        </div>
      </React.Fragment>
    )
  },
  getComponentToShow() {
    let componentToShow
    switch (this.componentGuideModel.get('value')[0]) {
      case 'Card':
        componentToShow = CardGuideView
        break
      case 'Button':
        componentToShow = ButtonGuideView
        break
      case 'ButtonReact':
        componentToShow = Button
        break
      case 'checkbox':
        componentToShow = Checkbox
        break
      case 'text':
        componentToShow = Text
        break
      case 'range':
        componentToShow = Range
        break
      case 'date':
        componentToShow = DateComponent
        break
      case 'location':
        componentToShow = Location
        break
      case 'thumbnail':
        componentToShow = Thumbnail
        break
      case 'geometry':
        componentToShow = Geometry
        break
      case 'number':
        componentToShow = NumberComponent
        break
      case 'color':
        componentToShow = Color
        break
      case 'autocomplete':
        componentToShow = Autocomplete
        break
      case 'inputwithparam':
        componentToShow = InputWithParam
        break
      case 'enum':
        componentToShow = Enum
        break
      case 'multienum':
        componentToShow = MultiEnum
        break
      case 'textarea':
        componentToShow = Textarea
        break
      case 'Password':
        componentToShow = Password
        break
      case 'Dropdowns':
        componentToShow = DropdownGuideView
        break
      case 'Inputs':
        componentToShow = InputGuideView
        break
      case 'Static Dropdowns':
        componentToShow = StaticDropdownGuideView
        break
      case 'JSX':
        componentToShow = JSXGuideView
        break
      case 'Regions':
        componentToShow = RegionGuideView
        break
      default:
        componentToShow = CardGuideView
        break
    }
    return componentToShow
  },
})
