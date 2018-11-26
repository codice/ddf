const BaseGuideView = require('../base-guide/base-guide.view.js')
const Marionette = require('marionette')
const template = require('./dropdown-guide.hbs')
const CustomElements = require('../../../js/CustomElements.js')
const Backbone = require('backbone')
const exampleOne = require('./exampleOne.hbs')
const exampleOneDropdown = require('./exampleOneDropdown.hbs')
const exampleTwoDropdown = require('./exampleTwoDropdown.hbs')
const exampleThreeDropdown = require('./exampleThreeDropdown.hbs')
const DropdownBehavior = require('../../../behaviors/dropdown.behavior.js')

module.exports = BaseGuideView.extend({
  templates: {
    exampleOne,
    exampleOneDropdown,
    exampleTwoDropdown,
    exampleThreeDropdown,
  },
  styles: {},
  template: template,
  tagName: CustomElements.register('dev-dropdown-guide'),
  regions: {
    exampleOne: '.example:nth-of-type(1) .instance',
    exampleTwo: '.example:nth-of-type(2) .instance',
    exampleThree: '.example:nth-of-type(3) .instance',
    exampleFour: '.example:nth-of-type(4) .instance',
    exampleFive: '.example:nth-of-type(5) .instance',
  },
  showComponents() {
    this.showExampleOne()
    this.showExampleTwo()
    this.showExampleThree()
    // this.showExampleThree();
    // this.showExampleFour();
    // this.showExampleFive();
  },
  exampleOneDropdown() {
    return Marionette.ItemView.extend({
      template: exampleOneDropdown,
    })
  },
  exampleOneView() {
    const OtherView = this.exampleOneDropdown()
    return Marionette.ItemView.extend({
      template: exampleOne,
      behaviors() {
        return {
          dropdown: {
            dropdowns: [
              {
                selector: '> div > button:first-of-type',
                view: OtherView,
                viewOptions: {
                  model: undefined,
                },
              },
            ],
          },
        }
      },
    })
  },
  exampleTwoDropdown() {
    return Marionette.ItemView.extend({
      template: exampleTwoDropdown,
      behaviors() {
        return {
          dropdown: {
            dropdowns: [
              {
                selector: '> div > button:first-of-type',
                view: this.constructor,
                viewOptions: {
                  model: undefined,
                },
              },
            ],
          },
        }
      },
    })
  },
  exampleTwoView() {
    const OtherView = this.exampleTwoDropdown()
    return Marionette.ItemView.extend({
      template: exampleOne,
      behaviors() {
        return {
          dropdown: {
            dropdowns: [
              {
                selector: '> div > button:first-of-type',
                view: OtherView,
                viewOptions: {
                  model: undefined,
                },
              },
            ],
          },
        }
      },
    })
  },
  exampleThreeDropdown() {
    return Marionette.ItemView.extend({
      template: exampleThreeDropdown,
      events: {
        'click button': 'takeAction',
      },
      takeAction(e) {
        e.currentTarget.innerHTML = 'I got clicked!'
        DropdownBehavior.closeParentDropdown(this) //harmless if not in a dropdown
      },
    })
  },
  exampleThreeView() {
    const OtherView = this.exampleThreeDropdown()
    return Marionette.ItemView.extend({
      template: exampleOne,
      behaviors() {
        return {
          dropdown: {
            dropdowns: [
              {
                selector: '> div > button:first-of-type',
                view: OtherView.extend({
                  behaviors: {
                    navigation: {},
                  },
                }),
                viewOptions: {
                  model: undefined,
                },
              },
            ],
          },
        }
      },
    })
  },
  showExampleOne() {
    this.exampleOne.show(new (this.exampleOneView())())
  },
  showExampleTwo() {
    this.exampleTwo.show(new (this.exampleTwoView())())
  },
  showExampleThree() {
    this.exampleThree.show(new (this.exampleThreeView())())
  },
})
