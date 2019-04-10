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

//get descriptors for well known registry slot fields.

define(['underscore'], function(_) {
  var FieldDescriptors = {
    configurations: {},
    isCustomizableSegment: function(name) {
      return _.contains(
        [
          'General',
          'Person',
          'Service',
          'ServiceBinding',
          'Content',
          'Organization',
        ],
        name
      )
    },
    isContainerOnly: function(name) {
      return _.contains(['Address', 'TelephoneNumber', 'EmailAddress'], name)
    },

    getSlotTypes: function() {
      return {
        string: 'xs:string',
        date: 'xs:dateTime',
        number: 'xs:decimal',
        boolean: 'xs:boolean',
        point: 'urn:ogc:def:dataType:ISO-19107:2003:GM_Point',
        bounds: 'urn:ogc:def:dataType:ISO-19107:2003:GM_Envelope',
      }
    },
    getFieldType: function(name) {
      var types = this.getSlotTypes()
      var fieldType
      _.each(_.keys(types), function(key) {
        if (types[key] === name) {
          fieldType = key
        }
      })
      return fieldType
    },
    retrieveFieldDescriptors: function() {
      var descriptors = {}
      if (this.customFields) {
        for (var prop in this.customFields) {
          if (this.customFields.hasOwnProperty(prop)) {
            if (prop === 'Configuration') {
              this.configurations = this.customFields[prop]
            } else {
              descriptors[prop] = {}
              this.addSegment(descriptors[prop], this.customFields[prop])
            }
          }
        }
      }
      return descriptors
    },
    addSegment: function(base, custom) {
      var that = this
      _.each(custom, function(field) {
        base[field.key] = field
        base[field.key].isSlot = _.isUndefined(field.isSlot)
          ? true
          : field.isSlot
        if (
          field.constructTitle &&
          typeof field.constructTitle !== 'function'
        ) {
          base[field.key].constructTitle = that[field.constructTitle]
        }
        if (
          field.autoPopulateFunction &&
          typeof field.autoPopulateFunction !== 'function'
        ) {
          base[field.key].autoPopulateFunction =
            that[field.autoPopulateFunction]
        }
      })
    },
    constructEmailTitle: function() {
      var title = []
      title.push(this.getField('type').get('value'))
      title.push(this.getField('address').get('value'))
      var stringTitle = title
        .filter(function(val) {
          return val !== undefined
        })
        .join(' ')
        .trim()
      if (!stringTitle) {
        stringTitle = 'Empty Email'
      }
      return stringTitle
    },
    constructPhoneTitle: function() {
      var title = []
      title.push(this.getField('phoneType').get('value'))
      if (this.getField('areaCode').get('value')) {
        title.push('(' + this.getField('areaCode').get('value') + ')')
      }
      title.push(this.getField('number').get('value'))
      if (this.getField('extension').get('value')) {
        title.push(' x' + this.getField('extension').get('value'))
      }
      var stringTitle = title
        .filter(function(val) {
          return val !== undefined
        })
        .join(' ')
        .trim()
      if (!stringTitle || stringTitle === '()  x') {
        stringTitle = 'Empty Phone Number'
      }
      return stringTitle
    },
    constructAddressTitle: function() {
      var title = []
      title.push(this.getField('street').get('value'))
      title.push(this.getField('city').get('value'))
      title.push(this.getField('stateOrProvince').get('value'))
      var stringTitle = title
        .filter(function(val) {
          return val !== undefined
        })
        .join(' ')
        .trim()
      if (!stringTitle) {
        stringTitle = 'Empty Address'
      }
      return stringTitle
    },
    constructNameTitle: function() {
      var title = this.getField('Name').get('value')
      if (!title || (_.isArray(title) && title.length === 0)) {
        title = this.get('segmentType')
      }
      return title
    },
    constructNameVersionTitle: function() {
      var name = this.getField('Name').get('value')
      var version = this.getField('VersionInfo').get('value')
      var title
      if (!name || (_.isArray(name) && name.length === 0)) {
        title = this.get('segmentType')
      } else {
        title = name + '  Version: ' + version
      }
      return title
    },
    constructPersonNameTitle: function() {
      var personName
      for (var index = 0; index < this.get('segments').models.length; index++) {
        if (
          this.get('segments').models[index].get('segmentType') === 'PersonName'
        ) {
          personName = this.get('segments').models[index]
          break
        }
      }
      var title = []
      if (personName) {
        title.push(personName.getField('firstName').get('value'))
        title.push(personName.getField('lastName').get('value'))
        if (this.getField('Name').get('value').length > 0) {
          title.push(' [ ' + this.getField('Name').get('value') + ' ]')
        }
      }

      var stringTitle = title
        .filter(function(val) {
          return val !== undefined
        })
        .join(' ')
        .trim()
      if (!stringTitle) {
        stringTitle = this.get('segmentType')
      }
      return stringTitle
    },
    populateFromEndpointProps: function(segment, prePopObj) {
      segment.getField('Name').set('value', prePopObj.name)
      segment.getField('Description').set('value', prePopObj.description)
      segment.getField('VersionInfo').set('value', prePopObj.version)
      segment.getField('accessUri').set('value', prePopObj.url)

      for (var prop in prePopObj) {
        if (
          prePopObj.hasOwnProperty(prop) &&
          prop !== 'name' &&
          prop !== 'description' &&
          prop !== 'version' &&
          prop !== 'accessURI' &&
          prop !== 'id'
        ) {
          var field = segment.getField(prop)
          if (!field) {
            segment.addField(prop, 'string', [prePopObj[prop]])
          } else {
            segment.setFieldValue(field, [prePopObj[prop]])
          }
        }
      }
    },
  }
  return FieldDescriptors
})
