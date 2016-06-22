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
/*global define*/
define(['underscore'], function (_) {
    var FieldDescriptors = {
        isCustomizableSegment: function (name) {
            return _.contains(['General', 'Person', 'Service', 'ServiceBinding', 'Content', 'Organization'], name);
        },
        isContainerOnly: function (name) {
            return _.contains(['Address', 'TelephoneNumber', 'EmailAddress'], name);
        },

        getSlotTypes: function () {
            return {
                string: 'xs:string',
                date: 'xs:dateTime',
                number: 'xs:decimal',
                boolean: 'xs:boolean',
                point: 'urn:ogc:def:dataType:ISO-19107:2003:GM_Point'
            };
        },
        getFieldType: function (name) {
            var types = this.getSlotTypes();
            var fieldType;
            _.each(_.keys(types), function (key) {
                if (types[key] === name) {
                    fieldType = key;
                }
            });
            return fieldType;
        },
        retrieveFieldDescriptors: function () {
            var descriptors = {
                General: {
                    Name: {
                        displayName: 'Node Name',
                        description: 'This node\'s name as it should appear to external systems',
                        values: [],
                        type: 'string',
                        regex: '/^(\\w|\\d)/',
                        regexMessage: "Must supply a name starting with a letter or digit",
                        required: true
                    },
                    Description: {
                        displayName: 'Node Description',
                        description: 'Short description for this node',
                        values: [],
                        type: 'string'
                    },
                    VersionInfo: {
                        displayName: 'Node Version',
                        description: 'This node\'s Version',
                        values: [],
                        type: 'string'
                    }
                },
                Organization: {
                    Name: {
                        displayName: 'Organization Name',
                        description: 'This organization\'s name',
                        values: [],
                        type: 'string',
                        required: true
                    },
                    Address: {
                        isGroup: true,
                        displayName: "Address",
                        multiValued: true,
                        constructTitle: this.constructAddressTitle
                    },
                    TelephoneNumber: {
                        isGroup: true,
                        displayName: "Phone Number",
                        multiValued: true,
                        constructTitle: this.constructPhoneTitle
                    },
                    EmailAddress: {
                        isGroup: true,
                        displayName: "Email",
                        multiValued: true,
                        constructTitle: this.constructEmailTitle
                    }
                },
                Person: {
                    Name: {
                        displayName: 'Contact Title',
                        description: 'Contact Title',
                        values: [],
                        type: 'string'
                    },
                    PersonName: {
                        isGroup: true,
                        multiValued: false
                    },
                    Address: {
                        isGroup: true,
                        displayName: "Address",
                        multiValued: true,
                        constructTitle: this.constructAddressTitle
                    },
                    TelephoneNumber: {
                        isGroup: true,
                        displayName: "Phone Number",
                        multiValued: true,
                        constructTitle: this.constructPhoneTitle
                    },
                    EmailAddress: {
                        isGroup: true,
                        displayName: "Email",
                        multiValued: true,
                        constructTitle: this.constructEmailTitle
                    }
                },
                Service: {
                    Name: {
                        displayName: 'Service Name',
                        description: 'This service name',
                        values: [],
                        type: 'string'
                    },
                    Description: {
                        displayName: 'Service Description',
                        description: 'Short description for this service',
                        values: [],
                        type: 'string'
                    },
                    VersionInfo: {
                        displayName: 'Service Version',
                        description: 'This service version',
                        values: [],
                        type: 'string'
                    },
                    objectType: {
                        displayName: 'Service Type',
                        description: 'Identifies the type of service this is by a urn',
                        values: 'urn:registry:federation:service',
                        type: 'string',
                        multiValued: false
                    },
                    ServiceBinding: {
                        isGroup: true,
                        displayName: 'Bindings',
                        multiValued: true,
                        constructTitle: this.constructNameVersionTitle,
                        autoPopulateFunction: this.populateFromEndpointProps,
                        autoPopulateId: 'id',
                        autoPopulateName: 'name'
                    }
                },
                ServiceBinding: {
                    Name: {
                        displayName: 'Binding Name',
                        description: 'This binding name',
                        values: [],
                        type: 'string'
                    },
                    Description: {
                        displayName: 'Binding Description',
                        description: 'Short description for this binding',
                        values: [],
                        type: 'string'
                    },
                    VersionInfo: {
                        displayName: 'Binding Version',
                        description: 'This binding version',
                        values: [],
                        type: 'string'
                    },
                    accessUri: {
                        displayName: 'Access URL',
                        description: 'The url used to access this binding',
                        values: [],
                        type: 'string',
                        required: true
                    }
                },
                Content: {
                    Name: {
                        displayName: 'Content Name',
                        description: 'Name for this metadata content',
                        values: [],
                        type: 'string',
                        required: true
                    },
                    Description: {
                        displayName: 'Content Description',
                        description: 'Short description for this metadata content',
                        values: [],
                        type: 'string'
                    },
                    objectType: {
                        displayName: 'Content Object Type',
                        description: 'The kind of content object this will be. Default value should be used in most cases.',
                        type: 'string',
                        values: 'urn:registry:content:collection',
                        multiValued: false
                    }
                },
                PersonName: {
                    firstName: {
                        displayName: 'First Name',
                        description: 'First name',
                        values: [],
                        type: 'string'
                    },
                    lastName: {
                        displayName: 'Last Name',
                        description: 'Last name',
                        values: [],
                        type: 'string',
                        required: true
                    }
                },
                TelephoneNumber: {
                    phoneType: {
                        displayName: 'Phone Type',
                        description: 'Phone type could be work, home, mobile etc.',
                        type: 'string'
                    },
                    countryCode: {
                        displayName: 'Country Code',
                        description: 'Country code, i.e. USA=1',
                        type: 'number',
                        value: 1
                    },
                    areaCode: {
                        displayName: 'Area Code',
                        description: 'Area Code',
                        type: 'number'
                    },
                    number: {
                        displayName: 'Number',
                        description: 'Number',
                        type: 'string',
                        required: true
                    },
                    extension: {
                        displayName: 'Extension',
                        description: 'Extension',
                        type: 'number'
                    }
                },
                EmailAddress: {
                    type: {
                        displayName: 'Email Type',
                        description: 'Email Type could be work, personal, primary etc.',
                        values: [],
                        type: 'string'
                    },
                    address: {
                        displayName: 'Address',
                        description: 'Email Address',
                        values: [],
                        type: 'string',
                        required: true
                    }
                },
                Address: {
                    street: {
                        displayName: 'Street',
                        description: 'Street',
                        values: [],
                        type: 'string'
                    },
                    city: {
                        displayName: 'City',
                        description: 'City',
                        values: [],
                        type: 'string'
                    },
                    country: {
                        displayName: 'Country',
                        description: 'Country',
                        values: [],
                        type: 'string'
                    },
                    stateOrProvince: {
                        displayName: 'State or Province',
                        description: 'State or Province',
                        values: [],
                        type: 'string'
                    },
                    postalCode: {
                        displayName: 'Postal Code',
                        description: 'Postal Code',
                        values: [],
                        type: 'string'
                    }
                }
            };
            if (this.customSlots) {
                for (var prop in this.customSlots) {
                    if (this.customSlots.hasOwnProperty(prop)) {
                        this.mixInCustomSlots(descriptors[prop], this.customSlots[prop]);
                    }
                }
            }
            return descriptors;
        },
        mixInCustomSlots: function (base, custom) {
            for (var prop in custom) {
                if (custom.hasOwnProperty(prop)) {
                    base[prop] = custom[prop];
                    base[prop].isSlot = true;
                }
            }
        },
        constructEmailTitle: function () {
            var title = [];
            title.push(this.getField('type').get('value'));
            title.push(this.getField('address').get('value'));
            var stringTitle = title.filter(function(val){return val !== undefined;}).join(' ').trim();
            if (!stringTitle) {
                stringTitle = 'Empty Email';
            }
            return stringTitle;
        },
        constructPhoneTitle: function () {
            var title = [];
            title.push(this.getField('phoneType').get('value'));
            if (this.getField('areaCode').get('value')) {
                title.push('(' + this.getField('areaCode').get('value') + ')' );
            }
            title.push(this.getField('number').get('value'));
            if (this.getField('extension').get('value')) {
                title.push(' x' + this.getField('extension').get('value'));
            }
            var stringTitle = title.filter(function(val){return val !== undefined;}).join(' ').trim();
            if (!stringTitle || stringTitle === '()  x') {
                stringTitle = 'Empty Phone Number';
            }
            return stringTitle;
        },
        constructAddressTitle: function () {
            var title = [];
            title.push(this.getField('street').get('value'));
            title.push(this.getField('city').get('value'));
            title.push(this.getField('stateOrProvince').get('value'));
            var stringTitle = title.filter(function(val){return val !== undefined;}).join(' ').trim();
            if (!stringTitle) {
                stringTitle = 'Empty Address';
            }
            return stringTitle;
        },
        constructNameTitle: function () {
            var title = this.getField('Name').get('value');
            if (!title || (_.isArray(title) && title.length === 0)) {
                title = this.get('segmentType');
            }
            return title;
        },
        constructNameVersionTitle: function () {
            var name = this.getField('Name').get('value');
            var version = this.getField('VersionInfo').get('value');
            var title;
            if (!name || (_.isArray(name) && name.length === 0)) {
                title = this.get('segmentType');
            } else {
                title = name + '  Version: ' + version;
            }
            return title;
        },
        constructPersonNameTitle: function () {
            var personName;
            for (var index = 0; index < this.get('segments').models.length; index++) {
                if (this.get('segments').models[index].get('segmentType') === 'PersonName') {
                    personName = this.get('segments').models[index];
                    break;
                }
            }
            var title = [];
            if (personName) {
                title.push(personName.getField('firstName').get('value'));
                title.push(personName.getField('lastName').get('value'));
                if (this.getField('Name').get('value').length > 0) {
                    title.push(' [ ' + this.getField('Name').get('value') + ' ]');
                }
            }

            var stringTitle = title.filter(function(val){return val !== undefined;}).join(' ').trim();
            if (!stringTitle) {
                stringTitle = this.get('segmentType');
            }
            return stringTitle;
        },
        populateFromEndpointProps: function (segment, prePopObj) {
            segment.getField('Name').set('value', prePopObj.name);
            segment.getField('Description').set('value', prePopObj.description);
            segment.getField('VersionInfo').set('value', prePopObj.version);
            segment.getField('accessUri').set('value', prePopObj.url);

            for (var prop in prePopObj) {
                if (prePopObj.hasOwnProperty(prop) && prop !== 'name' && prop !== 'description' && prop !== 'version' && prop !== 'accessURI' && prop !== 'id') {
                    var field = segment.getField(prop);
                    if (!field) {
                        segment.addField(prop, 'string', [prePopObj[prop]]);

                    } else {
                        segment.setFieldValue(field, [prePopObj[prop]]);
                    }
                }
            }
        }
    };
    return FieldDescriptors;
});
