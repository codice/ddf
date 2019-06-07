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
const BaseGuideView = require('../base-guide/base-guide.view.js')
const Marionette = require('marionette')
const template = require('./static-dropdown-guide.hbs')
const CustomElements = require('../../../js/CustomElements.js')
const PopoutView = require('../../../component/dropdown/popout/dropdown.popout.view.js')
const Backbone = require('backbone')
const exampleTwoTemplate = require('./exampleTwo.hbs')
const exampleFourTemplate = require('./exampleFour.hbs')
const exampleFiveTemplate = require('./exampleFive.hbs')

const exampleBackboneModel = new Backbone.Model({
  title: 'Dropdown with View Listening',
})

module.exports = BaseGuideView.extend({
  templates: {
    exampleFiveTemplate,
  },
  template,
  tagName: CustomElements.register('dev-static-dropdown-guide'),
  regions: {
    exampleOne: '.example:nth-of-type(1) .instance',
    exampleTwo: '.example:nth-of-type(2) .instance',
    exampleThree: '.example:nth-of-type(3) .instance',
    exampleFour: '.example:nth-of-type(4) .instance',
    exampleFive: '.example:nth-of-type(5) .instance',
  },
  model: exampleBackboneModel,
  showComponents() {
    this.showExampleOne()
    this.showExampleTwo()
    this.showExampleThree()
    this.showExampleFour()
    this.showExampleFive()
  },
  showExampleFive() {
    this.exampleFive.show(
      PopoutView.createSimpleDropdown({
        // typically componentToShow wouldn't be an inline definition of a Marionette View like this, you'd be importing it from another file
        componentToShow: Marionette.LayoutView.extend({
          className: 'composed-menu', // necessary for navigation behavior
          regions: {
            composedMenu: '.composed-menu.region',
          },
          events: {
            'click *:not(.composed-menu)': 'takeMenuAction',
          },
          takeMenuAction() {
            this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
          },
          template: exampleFiveTemplate,
          onBeforeShow() {
            if (this.options.nestedDepth >= 2) {
              return
            }
            this.composedMenu.show(
              new this.constructor({
                nestedDepth: this.options.nestedDepth + 1,
              })
            )
          },
        }),
        modelForComponent: this.model,
        leftIcon: 'fa fa-bars',
        label: 'Menu',
        dropdownCompanionBehaviors: {
          navigation: {},
        },
        options: {
          nestedDepth: 1, // you can also pass options to your componentToShow
        },
      })
    )
  },
  showExampleFour() {
    this.exampleFour.show(
      PopoutView.createSimpleDropdown({
        // typically componentToShow wouldn't be an inline definition of a Marionette View like this, you'd be importing it from another file
        componentToShow: Marionette.ItemView.extend({
          template: exampleFourTemplate,
          events: {
            'click button': 'updateTitle',
          },
          updateTitle() {
            this.model.set('title', Math.random())
            this.closeDropdown()
          },
          closeDropdown() {
            // Trigger this method when it makes sense (say after a save in the dropdown, or simply pressing the close button)
            this.$el.trigger('closeDropdown.' + CustomElements.getNamespace()) // safe to do even when not in a dropdown!
            // Use the inception dropdown and notice it doesn't propagate up through nested dropdowns
            // You could however watch the dropdown model and listen to the close event to propagate this event upwards!
          },
        }),
        modelForComponent: this.model,
        leftIcon: 'fa fa-headphones',
        label: 'The View is Listening',
      })
    )
    const onTitleChange = () => {
      this.$el.toggleClass('flip-background')
    }
    this.stopListening(this.model, 'change:title', onTitleChange).listenTo(
      this.model,
      'change:title',
      onTitleChange
    )
  },
  showExampleThree() {
    this.exampleThree.show(
      PopoutView.createSimpleDropdown({
        componentToShow: this.constructor,
        leftIcon: 'fa fa-plane', // right or left doesn't matter when label is omitted
      })
    )
  },
  showExampleTwo() {
    this.exampleTwo.show(
      PopoutView.createSimpleDropdown({
        // typically componentToShow wouldn't be an inline definition of a Marionette View like this, you'd be importing it from another file
        componentToShow: Marionette.ItemView.extend({
          template: exampleTwoTemplate,
          events: {
            'click button': 'closeDropdown',
          },
          closeDropdown() {
            // Trigger this method when it makes sense (say after a save in the dropdown, or simply pressing the close button)
            this.$el.trigger('closeDropdown.' + CustomElements.getNamespace()) // safe to do even when not in a dropdown!
            // Use the inception dropdown and notice it doesn't propagate up through nested dropdowns
            // You could however watch the dropdown model and listen to the close event to propagate this event upwards!
          },
        }),
        leftIcon: 'fa fa-ellipsis-v',
        rightIcon: 'fa fa-truck',
        label: 'Without passing in a modelForComponent',
      })
    )
  },
  showExampleOne() {
    this.exampleOne.show(
      PopoutView.createSimpleDropdown({
        componentToShow: this.constructor,
        /*
                Typically modelForComponent is a model you're passing along from the view you're in.  You're planning to utilize it to
                display something in the dropdown and also allow the view in the dropdown to change that model.  The view that made the dropdown is
                typically listening for changes to the model and then takes actions based on them.
            */
        modelForComponent: new Backbone.Model(),
        leftIcon: 'fa fa-ellipsis-v',
        rightIcon: 'fa fa-truck',
        label: 'Inception', // leave off for an icon only dropdown
      })
    )
  },
  styles: {},
})
