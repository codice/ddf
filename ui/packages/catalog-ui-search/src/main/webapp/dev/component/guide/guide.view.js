const Marionette = require('marionette')
const template = require('./guide.hbs')
const CustomElements = require('js/CustomElements')
const PropertyView = require('component/property/property.view')
const Property = require('component/property/property')
const CardGuideView = require('dev/component/card-guide/card-guide.view')
const ButtonGuideView = require('dev/component/button-guide/button-guide.view')
const StaticDropdownGuideView = require('dev/component/static-dropdown-guide/static-dropdown-guide.view')
const DropdownGuideView = require('dev/component/dropdown-guide/dropdown-guide.view')
const InputGuideView = require('dev/component/input-guide/input-guide.view')
const JSXGuideView = require('dev/component/jsx-guide/jsx-guide.view')
const RegionGuideView = require('dev/component/region-guide/region-guide.view')
import Button from '../button'
import MarionetteRegionContainer from '../../../react-component/container/marionette-region-container'
import React from 'react'

module.exports = Marionette.LayoutView.extend({
  template: template,
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
          label: 'Card',
          value: 'Card',
        },
        {
          label: 'Button',
          value: 'Button',
        },
        {
          label: 'Button (react)',
          value: 'ButtonReact',
        },
        {
          label: 'Static Dropdowns (deprecated)',
          value: 'Static Dropdowns',
        },
        {
          label: 'Dropdowns',
          value: 'Dropdowns',
        },
        {
          label: 'Inputs',
          value: 'Inputs',
        },
        {
          label: 'JSX',
          value: 'JSX',
        },
        {
          label: 'Regions (Layout Views)',
          value: 'Regions',
        },
      ],
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
