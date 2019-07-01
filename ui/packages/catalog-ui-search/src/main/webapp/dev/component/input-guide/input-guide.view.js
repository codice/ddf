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
const template = require('./input-guide.hbs')
const CustomElements = require('../../../js/CustomElements.js')
const PropertyView = require('../../../component/property/property.view.js')
const Property = require('../../../component/property/property.js')
const Common = require('../../../js/Common.js')

module.exports = BaseGuideView.extend({
  templates: {},
  styles: {},
  template,
  tagName: CustomElements.register('dev-input-guide'),
  regions: {
    exampleOne: '.example:nth-of-type(1) .instance',
    exampleTwo: '.example:nth-of-type(2) .instance',
    exampleThree: '.example:nth-of-type(3) .instance',
    exampleFour: '.example:nth-of-type(4) .instance',
    exampleFive: '.example:nth-of-type(5) .instance',
    exampleSix: '.example:nth-of-type(6) .instance',
    exampleSeven: '.example:nth-of-type(7) .instance',
    exampleEight: '.example:nth-of-type(8) .instance',
    exampleNine: '.example:nth-of-type(9) .instance',
    example10: '.example:nth-of-type(10) .instance',
    example11: '.example:nth-of-type(11) .instance',
    example12: '.example:nth-of-type(12) .instance',
    example13: '.example:nth-of-type(13) .instance',
    example14: '.example:nth-of-type(14) .instance',
    example15: '.example:nth-of-type(15) .instance',
    example16: '.example:nth-of-type(16) .instance',
    example17: '.example:nth-of-type(17) .instance',
    example18: '.example:nth-of-type(18) .instance',
    example19: '.example:nth-of-type(19) .instance',
    example20: '.example:nth-of-type(20) .instance',
    example21: '.example:nth-of-type(21) .instance',
    example22: '.example:nth-of-type(22) .instance',
    example23: '.example:nth-of-type(23) .instance',
    example24: '.example:nth-of-type(24) .instance',
    example25: '.example:nth-of-type(25) .instance',
    example26: '.example:nth-of-type(26) .instance',
  },
  showComponents() {
    this.showExampleOne()
    this.showExampleTwo()
    this.showExampleThree()
    this.showExampleFour()
    this.showExampleFive()
    this.showExampleSix()
    this.showExampleSeven()
    this.showExampleEight()
    this.showExampleNine()
    this.showExample10()
    this.showExample11()
    this.showExample12()
    this.showExample13()
    this.showExample14()
    this.showExample15()
    this.showExample16()
    this.showExample17()
    this.showExample18()
    this.showExample19()
    this.showExample20()
    this.showExample21()
    this.showExample22()
    this.showExample23()
    this.showExample24()
    this.showExample25()
    this.showExample26()
  },
  showExampleOne() {
    this.exampleOne.show(
      new PropertyView({
        model: new Property({
          label: 'Range Input (you can customize units!)',
          value: [100],
          min: 62,
          max: 200,
          units: '%',
          type: 'RANGE',
          isEditing: true,
        }),
      })
    )
    this.listenTo(
      this.exampleOne.currentView.model,
      'change:value',
      this.handleExampleOneValue
    )
  },
  handleExampleOneValue() {
    this.$el.toggleClass(
      'above-100',
      this.exampleOne.currentView.model.getValue()[0] > 100
    )
    this.$el.toggleClass(
      'below-100',
      this.exampleOne.currentView.model.getValue()[0] < 100
    )
  },
  showExampleTwo() {
    this.exampleTwo.show(
      new PropertyView({
        model: new Property({
          label: 'Date Input',
          value: [new Date().toISOString()],
          type: 'DATE',
          isEditing: true,
        }),
      })
    )
  },
  showExampleThree() {
    this.exampleThree.show(
      new PropertyView({
        model: new Property({
          label: 'Location Input',
          value: [''],
          type: 'LOCATION',
          isEditing: true,
        }),
      })
    )
  },
  showExampleFour() {
    this.exampleFour.show(
      new PropertyView({
        model: new Property({
          label: 'Thumbnail Input',
          value: [''],
          type: 'BINARY',
          isEditing: true,
        }),
      })
    )
  },
  showExampleFive() {
    this.exampleFive.show(
      new PropertyView({
        model: new Property({
          label: 'Boolean Input',
          value: [true],
          type: 'BOOLEAN',
          isEditing: true,
        }),
      })
    )
  },
  showExampleSix() {
    this.exampleSix.show(
      new PropertyView({
        model: new Property({
          label: 'Geometry Input (WKT)',
          value: ['POINT (0 0)'],
          type: 'GEOMETRY',
          isEditing: true,
        }),
      })
    )
  },
  showExampleSeven() {
    this.exampleSeven.show(
      new PropertyView({
        model: new Property({
          label: 'Number Input',
          value: [0],
          type: 'INTEGER',
          isEditing: true,
        }),
      })
    )
  },
  showExampleEight() {
    this.exampleEight.show(
      new PropertyView({
        model: new Property({
          label: 'Autocomplete',
          placeholder: 'Pan to a region, country, or city',
          value: [''],
          url: './internal/geofeature/suggestions',
          minimumInputLength: 2,
          type: 'AUTOCOMPLETE',
          isEditing: true,
        }),
      })
    )
  },
  showExampleNine() {
    this.exampleNine.show(
      new PropertyView({
        model: new Property({
          label: 'Color',
          value: ['#741d1d'],
          type: 'COLOR',
          isEditing: true,
        }),
      })
    )
  },
  showExample10() {
    this.example10.show(
      new PropertyView({
        model: new Property({
          label: 'Near',
          value: [
            {
              value: 1,
              distance: 10,
            },
          ],
          type: 'NEAR',
          param: 'within',
          isEditing: true,
        }),
      })
    )
  },
  showExample11() {
    this.example11.show(
      new PropertyView({
        model: new Property({
          label: 'Text',
          value: ['hello'],
          type: 'STRING',
          isEditing: true,
        }),
      })
    )
  },
  showExample12() {
    this.example12.show(
      new PropertyView({
        model: new Property({
          label: 'Textarea',
          value: [
            `
                Lorem Ipsum is simply dummy text of the printing and 
                typesetting industry. Lorem Ipsum has been the industry's 
                standard dummy text ever since the 1500s, when an unknown printer 
                took a galley of type and scrambled it to make a type specimen book. 
                It has survived not only five centuries, but also the leap into 
                electronic typesetting, remaining essentially unchanged. 
                It was popularised in the 1960s with the release of Letraset 
                sheets containing Lorem Ipsum passages, and more recently with 
                desktop publishing software like Aldus PageMaker including versions 
                of Lorem Ipsum.
                `,
          ],
          type: 'TEXTAREA',
          isEditing: true,
        }),
      })
    )
  },
  showExample13() {
    this.example13.show(
      new PropertyView({
        model: new Property({
          label: 'Enum',
          value: [true],
          enum: [
            {
              label: 'On',
              value: true,
            },
            {
              label: 'Off',
              value: false,
            },
          ],
          isEditing: true,
        }),
      })
    )
  },
  showExample14() {
    this.example14.show(
      new PropertyView({
        model: new Property({
          label: 'Enum',
          value: ['more on'],
          enum: [
            {
              label: 'On',
              value: 'on',
            },
            {
              label: 'Off',
              value: 'off',
            },
            {
              label: 'More on',
              value: 'more on',
            },
            {
              label: 'More Off',
              value: 'more Off',
            },
          ],
          enumFiltering: true,
          isEditing: true,
          showValidationIssues: true,
        }),
      })
    )
  },
  showExample15() {
    this.example15.show(
      new PropertyView({
        model: new Property({
          label: 'Enum',
          value: [['more Off', 'on']],
          enum: [
            {
              label: 'On',
              value: 'on',
            },
            {
              label: 'Off',
              value: 'off',
            },
            {
              label: 'More on',
              value: 'more on',
            },
            {
              label: 'More Off',
              value: 'more Off',
            },
          ],
          enumFiltering: true,
          isEditing: true,
          enumMulti: true,
        }),
      })
    )
  },
  showExample16() {
    this.example16.show(
      new PropertyView({
        model: new Property({
          label: 'Enum',
          value: ['not a possible value'],
          enum: [
            {
              label: 'On',
              value: 'on',
            },
            {
              label: 'Off',
              value: 'off',
            },
            {
              label: 'More on',
              value: 'more on',
            },
            {
              label: 'More Off',
              value: 'more Off',
            },
          ],
          isEditing: true,
          showValidationIssues: true,
        }),
      })
    )
  },
  showExample17() {
    this.example17.show(
      new PropertyView({
        model: new Property({
          label: 'Enum',
          value: ['custom values too'],
          enum: [
            {
              label: 'On',
              value: 'on',
            },
            {
              label: 'Off',
              value: 'off',
            },
            {
              label: 'More on',
              value: 'more on',
            },
            {
              label: 'More Off',
              value: 'more Off',
            },
          ],
          isEditing: true,
          showValidationIssues: false,
          enumFiltering: true,
          enumCustom: true,
        }),
      })
    )
  },
  showExample18() {
    this.example18.show(
      new PropertyView({
        model: new Property({
          label: 'Enum',
          value: ['custom values too'],
          enum: [
            {
              label: 'On',
              value: 'on',
            },
            {
              label: 'Off',
              value: 'off',
            },
            {
              label: 'More on',
              value: 'more on',
            },
            {
              label: 'More Off',
              value: 'more Off',
            },
          ],
          isEditing: true,
          showValidationIssues: true,
          enumFiltering: true,
          enumCustom: true,
        }),
      })
    )
  },
  showExample19() {
    this.example19.show(
      new PropertyView({
        model: new Property({
          label: 'Enum',
          value: ['custom values too'],
          enum: [
            {
              label: 'On',
              value: 'on',
            },
            {
              label: 'Off',
              value: 'off',
            },
            {
              label: 'More on',
              value: 'more on',
            },
            {
              label: 'More Off',
              value: 'more Off',
            },
          ],
          isEditing: true,
          showValidationIssues: true,
          enumFiltering: true,
          enumCustom: true,
          multivalued: true,
        }),
      })
    )
  },
  showExample20() {
    this.example20.show(
      new PropertyView({
        model: new Property({
          label: 'Enum',
          value: [],
          values: {
            1: {
              value: [1],
              hits: 1,
              ids: ['fakeid1'],
              hasNoValue: false,
            },
            2: {
              value: [2],
              hits: 2,
              ids: ['fakeid2', 'fakeid3'],
              hasNoValue: false,
            },
            '2,3': {
              value: [2, 3],
              hits: 1,
              ids: ['fakeid4'],
              hasNoValue: false,
            },
            '2,3': {
              value: [2, 3],
              hits: 1,
              ids: ['fakeid4'],
              hasNoValue: false,
            },
            [Common.undefined]: {
              value: [],
              hits: 1,
              ids: ['fakeid5'],
              hasNoValue: true,
            },
          },
          type: 'FLOAT',
          isEditing: true,
          bulk: true,
          multivalued: true,
        }),
      })
    )
  },
  showExample21() {
    this.example21.show(
      new PropertyView({
        model: new Property({
          label: 'Enum',
          value: ['custom values too'],
          enum: [
            {
              label: 'On',
              value: 'on',
            },
            {
              label: 'Off',
              value: 'off',
            },
            {
              label: 'More on',
              value: 'more on',
            },
            {
              label: 'More Off',
              value: 'more Off',
            },
          ],
          isEditing: false,
          showValidationIssues: false,
          hasConflictingDefinition: true,
        }),
      })
    )
    this.example21.currentView.turnOnEditing()
  },
  showExample22() {
    this.example22.show(
      new PropertyView({
        model: new Property({
          label: 'Enum',
          value: ['custom values too'],
          enum: [
            {
              label: 'On',
              value: 'on',
            },
            {
              label: 'Off',
              value: 'off',
            },
            {
              label: 'More on',
              value: 'more on',
            },
            {
              label: 'More Off',
              value: 'more Off',
            },
          ],
          isEditing: false,
          showValidationIssues: false,
          readOnly: true,
        }),
      })
    )
    this.example22.currentView.turnOnEditing()
  },
  showExample23() {
    this.example23.show(
      new PropertyView({
        model: new Property({
          label: 'Enum',
          value: ['custom values too'],
          enum: [
            {
              label: 'On',
              value: 'on',
            },
            {
              label: 'Off',
              value: 'off',
            },
            {
              label: 'More on',
              value: 'more on',
            },
            {
              label: 'More Off',
              value: 'more Off',
            },
          ],
          showValidationIssues: false,
          isEditing: false,
        }),
      })
    )
    this.example23.currentView.updateValidation({
      errors: ['this is an error'],
      warnings: [],
    })
  },
  showExample24() {
    this.example24.show(
      new PropertyView({
        model: new Property({
          label: 'Enum',
          value: ['custom values too'],
          enum: [
            {
              label: 'On',
              value: 'on',
            },
            {
              label: 'Off',
              value: 'off',
            },
            {
              label: 'More on',
              value: 'more on',
            },
            {
              label: 'More Off',
              value: 'more Off',
            },
          ],
          showValidationIssues: false,
          isEditing: false,
        }),
      })
    )
    this.example24.currentView.updateValidation({
      errors: [],
      warnings: ['this is a warning'],
    })
  },
  showExample25() {
    this.example25.show(
      new PropertyView({
        model: new Property({
          label: 'Enum',
          value: ['custom values too'],
          enum: [
            {
              label: 'On',
              value: 'on',
            },
            {
              label: 'Off',
              value: 'off',
            },
            {
              label: 'More on',
              value: 'more on',
            },
            {
              label: 'More Off',
              value: 'more Off',
            },
          ],
          showLabel: false,
          showValidationIssues: false,
          isEditing: true,
        }),
      })
    )
  },
  showExample26() {
    this.example26.show(
      new PropertyView({
        model: new Property({
          label: 'Time Input',
          placeholder:
            'This input only likes times, not dates (or any fruit, really)',
          value: [new Date().toISOString()],
          type: 'TIME',
          isEditing: true,
        }),
      })
    )
  },
})
