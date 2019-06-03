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

const _ = require('underscore')
const _merge = require('lodash/merge')
const _debounce = require('lodash/debounce')
const $ = require('jquery')
const wreqr = require('../../js/wreqr.js')
const template = require('./golden-layout.hbs')
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const GoldenLayout = require('golden-layout')
const properties = require('../../js/properties.js')
const Common = require('../../js/Common.js')
const store = require('../../js/store.js')
const user = require('../singletons/user-instance.js')
const VisualizationDropdown = require('../dropdown/visualization-selector/dropdown.visualization-selector.view.js')
const DropdownModel = require('../dropdown/dropdown.js')
const sanitize = require('sanitize-html')
import ExtensionPoints from '../../extension-points'

const treeMap = (obj, fn, path = []) => {
  if (Array.isArray(obj)) {
    return obj.map((v, i) => treeMap(v, fn, path.concat(i)))
  }

  if (obj !== null && typeof obj === 'object') {
    return Object.keys(obj)
      .map(k => [k, treeMap(obj[k], fn, path.concat(k))])
      .reduce((o, [k, v]) => {
        o[k] = v
        return o
      }, {})
  }

  return fn(obj, path)
}

const sanitizeTree = tree =>
  treeMap(tree, obj => {
    if (typeof obj === 'string') {
      return sanitize(obj, {
        allowedTags: [],
        allowedAttributes: [],
      })
    }
    return obj
  })

const defaultGoldenLayoutContent = {
  content: properties.defaultLayout || [
    {
      type: 'stack',
      content: [
        {
          type: 'component',
          componentName: 'cesium',
          title: '3D Map',
        },
        {
          type: 'component',
          componentName: 'inspector',
          title: 'Inspector',
        },
      ],
    },
  ],
}

function getGoldenLayoutSettings() {
  const minimumScreenSize = 20 //20 rem or 320px at base font size
  const fontSize = parseInt(
    user
      .get('user')
      .get('preferences')
      .get('fontSize')
  )
  const theme = user
    .get('user')
    .get('preferences')
    .get('theme')
    .getTheme()
  return {
    settings: {
      showPopoutIcon: false,
      responsiveMode: 'none',
    },
    dimensions: {
      borderWidth: 0.5 * parseFloat(theme.minimumSpacing) * fontSize,
      minItemHeight: minimumScreenSize * fontSize,
      minItemWidth: minimumScreenSize * fontSize,
      headerHeight: parseFloat(theme.minimumButtonSize) * fontSize,
      dragProxyWidth: 300,
      dragProxyHeight: 200,
    },
  }
}

// see https://github.com/deepstreamIO/golden-layout/issues/239 for details on why the setTimeout is necessary
// The short answer is it mostly has to do with making sure these ComponentViews are able to function normally (set up events, etc.)
function registerComponent(
  marionetteView,
  name,
  ComponentView,
  componentOptions
) {
  const options = _.extend({}, marionetteView.options, componentOptions)
  marionetteView.goldenLayout.registerComponent(
    name,
    (container, componentState) => {
      container.on('open', () => {
        setTimeout(() => {
          const componentView = new ComponentView(
            _.extend({}, options, componentState, {
              container,
            })
          )
          container.getElement().append(componentView.el)
          componentView.render()
          container.on('destroy', () => {
            componentView.destroy()
          })
        }, 0)
      })
      container.on('tab', tab => {
        tab.closeElement.off('click').on('click', event => {
          if (
            tab.header.parent.isMaximised &&
            tab.header.parent.contentItems.length === 1
          ) {
            tab.header.parent.toggleMaximise()
          }
          tab._onCloseClickFn(event)
        })
      })
    }
  )
}

function unMaximize(contentItem) {
  if (contentItem.isMaximised) {
    contentItem.toggleMaximize()
    return true
  } else if (contentItem.contentItems.length === 0) {
    return false
  } else {
    return _.some(contentItem.contentItems, isMaximised)
  }
}

function isMaximised(contentItem) {
  if (contentItem.isMaximised) {
    return true
  } else if (contentItem.contentItems.length === 0) {
    return false
  } else {
    return _.some(contentItem.contentItems, isMaximised)
  }
}

function removeActiveTabInformation(config) {
  if (config.activeItemIndex !== undefined) {
    config.activeItemIndex = 0
  }
  if (config.content === undefined || config.content.length === 0) {
    return
  } else {
    return _.forEach(config.content, removeActiveTabInformation)
  }
}

function removeMaximisedInformation(config) {
  delete config.maximisedItemId
}

function removeEphemeralState(config) {
  removeMaximisedInformation(config)
  removeActiveTabInformation(config)
}

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('golden-layout'),
  template,
  className: 'is-minimised',
  events: {
    'click > .golden-layout-toolbar .to-toggle-size': 'handleToggleSize',
  },
  regions: {
    toolbar: '> .golden-layout-toolbar',
    widgetDropdown: '> .golden-layout-toolbar .to-add',
  },
  initialize(options) {
    this.options.selectionInterface = options.selectionInterface || store
  },
  updateFontSize() {
    const goldenLayoutSettings = getGoldenLayoutSettings()
    this.goldenLayout.config.dimensions.borderWidth =
      goldenLayoutSettings.dimensions.borderWidth
    this.goldenLayout.config.dimensions.minItemHeight =
      goldenLayoutSettings.dimensions.minItemHeight
    this.goldenLayout.config.dimensions.minItemWidth =
      goldenLayoutSettings.dimensions.minItemWidth
    this.goldenLayout.config.dimensions.headerHeight =
      goldenLayoutSettings.dimensions.headerHeight
    Common.repaintForTimeframe(2000, () => {
      this.goldenLayout.updateSize()
    })
  },
  updateSize() {
    this.goldenLayout.updateSize()
  },
  showWidgetDropdown() {
    this.widgetDropdown.show(
      new VisualizationDropdown({
        model: new DropdownModel(),
        goldenLayout: this.goldenLayout,
      })
    )
  },
  showGoldenLayout() {
    this.goldenLayout = new GoldenLayout(
      this.getGoldenLayoutConfig(),
      this.el.querySelector('.golden-layout-container')
    )
    this.registerGoldenLayoutComponents()
    this.goldenLayout.on(
      'stateChanged',
      _.debounce(this.handleGoldenLayoutStateChange.bind(this), 200)
    )
    this.goldenLayout.on('stackCreated', this.handleGoldenLayoutStackCreated)
    this.goldenLayout.on(
      'initialised',
      this.handleGoldenLayoutInitialised.bind(this)
    )
    this.goldenLayout.init()
  },
  getGoldenLayoutConfig() {
    let currentConfig = user
      .get('user')
      .get('preferences')
      .get(this.options.configName)
    if (currentConfig === undefined) {
      currentConfig = defaultGoldenLayoutContent
    }
    _merge(currentConfig, getGoldenLayoutSettings())
    return sanitizeTree(currentConfig)
  },
  registerGoldenLayoutComponents() {
    ExtensionPoints.visualizations.forEach(viz => {
      registerComponent(this, viz.id, viz.view, viz.options)
    })
  },
  detectIfGoldenLayoutMaximised() {
    this.$el.toggleClass('is-maximised', isMaximised(this.goldenLayout.root))
  },
  detectIfGoldenLayoutEmpty() {
    this.$el.toggleClass(
      'is-empty',
      this.goldenLayout.root.contentItems.length === 0
    )
  },
  handleGoldenLayoutInitialised() {
    this.detectIfGoldenLayoutMaximised()
    this.detectIfGoldenLayoutEmpty()
  },
  handleGoldenLayoutStackCreated(stack) {
    stack.header.controlsContainer
      .find('.lm_close')
      .off('click')
      .on('click', event => {
        if (stack.isMaximised) {
          stack.toggleMaximise()
        }
        stack.remove()
      })
  },
  handleGoldenLayoutStateChange(event) {
    this.detectIfGoldenLayoutMaximised()
    this.detectIfGoldenLayoutEmpty()
    //https://github.com/deepstreamIO/golden-layout/issues/253
    if (this.goldenLayout.isInitialised) {
      const currentConfig = this.goldenLayout.toConfig()
      removeEphemeralState(currentConfig)
      user
        .get('user')
        .get('preferences')
        .set(this.options.configName, currentConfig)
      wreqr.vent.trigger('resize')
      //do not add a window resize event, that will cause an endless loop.  If you need something like that, listen to the wreqr resize event.
    }
  },
  setupListeners() {
    this.listenTo(
      user.get('user').get('preferences'),
      'change:theme',
      this.updateFontSize
    )
    this.listenTo(
      user.get('user').get('preferences'),
      'change:fontSize',
      this.updateFontSize
    )
    this.listenForResize()
  },
  onRender() {
    this.showGoldenLayout()
    this.showWidgetDropdown()
    this.setupListeners()
  },
  handleToggleSize() {
    this.$el.toggleClass('is-minimised')
    this.goldenLayout.updateSize()
  },
  listenForResize() {
    $(window).on(
      'resize.' + this.cid,
      _debounce(
        event => {
          this.updateSize()
        },
        100,
        {
          leading: false,
          trailing: true,
        }
      )
    )
  },
  stopListeningForResize() {
    $(window).off('resize.' + this.cid)
  },
  onDestroy() {
    this.stopListeningForResize()
    if (this.goldenLayout) {
      this.goldenLayout.destroy()
    }
  },
})
