/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
define([
        'icanhaz',
        'underscore',
        'handlebars'
    ],
    function (ich, _, Handlebars) {
        "use strict";

        // The module to be exported
        var helper, helpers = {
            fileSize: function (item) {
                if (_.isUndefined(item)) {
                    return 'Unknown Size';
                }
                item += '';
                var givenProductSize = item.replace(/[,]+/g, '').trim();    //remove any commas and trailing whitespace
                var bytes = parseInt(givenProductSize, 10);
                var noUnitsGiven = /[0-9]$/;    //number without a word following
                var reformattedProductSize = givenProductSize.replace(/\s\s+/g, ' ');   //remove extra whitespaces
                var finalFormatProductSize = reformattedProductSize.replace(/([0-9])([a-zA-Z])/g, '$1 $2'); //make sure there is exactly one space between number and unit
                var sizeArray = finalFormatProductSize.split(' ');  //splits size into number and unit

                if (isNaN(bytes)) {
                    return 'Unknown Size';
                }

                if (noUnitsGiven.test(givenProductSize)) {   //need to parse number given and add units, number is assumed to be bytes
                    var size, index,
                        type = ['bytes', 'KB', 'MB', 'GB', 'TB'];
                    if (bytes === 0) {
                        return "0 bytes";
                    } else {
                        index = Math.floor(Math.log(bytes) / Math.log(1024));
                        if (index > 4) {
                            index = 4;
                        }

                        size = (bytes / Math.pow(1024, index)).toFixed(index < 2 ? 0 : 1);
                    }
                    return size + " " + type[index];

                } else {  //units were included with size

                    switch (sizeArray[1].toLowerCase()) {
                        case 'bytes':
                            return sizeArray[0] + ' bytes';
                        case 'b':
                            return sizeArray[0] + ' bytes';
                        case 'kb':
                            return sizeArray[0] + ' KB';
                        case 'kilobytes':
                            return sizeArray[0] + ' KB';
                        case 'kbytes':
                            return sizeArray[0] + ' KB';
                        case 'mb':
                            return sizeArray[0] + ' MB';
                        case 'megabytes':
                            return sizeArray[0] + ' MB';
                        case 'mbytes':
                            return sizeArray[0] + ' MB';
                        case 'gb':
                            return sizeArray[0] + ' GB';
                        case 'gigabytes':
                            return sizeArray[0] + ' GB';
                        case 'gbytes':
                            return sizeArray[0] + ' GB';
                        case 'tb':
                            return sizeArray[0] + ' TB';
                        case 'terabytes':
                            return sizeArray[0] + ' TB';
                        case 'tbytes':
                            return sizeArray[0] + ' TB';
                        default:
                            return 'Unknown Size';
                    }
                }
            },
            fileSizeGuaranteedInt: function (item) {
                if (_.isUndefined(item)) {
                    return 'Unknown Size';
                }
                var bytes = parseInt(item, 10);
                if (isNaN(bytes)) {
                    return item;
                }
                var size, index,
                    type = ['bytes', 'KB', 'MB', 'GB', 'TB'];
                if (bytes === 0) {
                    return "0 bytes";
                }
                else {
                    index = Math.floor(Math.log(bytes) / Math.log(1024));
                    if (index > 4) {
                        index = 4;
                    }

                    size = (bytes / Math.pow(1024, index)).toFixed(index < 2 ? 0 : 1);
                }
                return size + " " + type[index];
            },
            isNotBlank: function (context, block) {
                if (context && context !== "") {
                    return block.fn(this);
                }
                else {
                    return block.inverse(this);
                }
            },
            is: function (value, test, options) {
                if (value === test) {
                    return options.fn(this);
                } else {
                    return options.inverse(this);
                }
            },
            isnt: function (value, test, options) {
                if (value !== test) {
                    return options.fn(this);
                } else {
                    return options.inverse(this);
                }
            },
            isUrl: function (value, options) {
                if (value && value !== "" && _.isString(value)) {
                    var protocol = value.toLowerCase().split("/")[0];
                    if (protocol && (protocol === "http:" || protocol === "https:")) {
                        return options.fn(this);
                    }
                }
                return options.inverse(this);
            },
            gt: function (value, test, options) {
                if (value > test) {
                    return options.fn(this);
                } else {
                    return options.inverse(this);
                }
            },

            gte: function (value, test, options) {
                if (value >= test) {
                    return options.fn(this);
                } else {
                    return options.inverse(this);
                }
            },
            lt: function (value, test, options) {
                if (value < test) {
                    return options.fn(this);
                } else {
                    return options.inverse(this);
                }
            },

            lte: function (value, test, options) {
                if (value <= test) {
                    return options.fn(this);
                } else {
                    return options.inverse(this);
                }
            },
            ifAnd: function () {
                var args = _.flatten(arguments);
                var items = _.initial(args);
                var result = true;
                var block = _.last(args);
                _.each(items, function (item) {
                    if (!item) {
                        result = false;
                    }
                });
                if (result) {
                    return block.fn(this);
                }
                else {
                    return block.inverse(this);
                }
            },
            ifOr: function () {
                var args = _.flatten(arguments);
                var items = _.initial(args);
                var result = false;
                var block = _.last(args);
                _.each(items, function (item) {
                    if (item) {
                        result = true;
                    }
                });
                if (result) {
                    return block.fn(this);
                }
                else {
                    return block.inverse(this);
                }
            },
            ifNotAnd: function () {
                var args = _.flatten(arguments);
                var items = _.initial(args);
                var result = true;
                var block = _.last(args);
                _.each(items, function (item) {
                    if (!item) {
                        result = false;
                    }
                });
                if (result) {
                    return block.inverse(this);
                }
                else {
                    return block.fn(this);
                }
            },
            ifNotOr: function () {
                var args = _.flatten(arguments);
                var items = _.initial(args);
                var result = false;
                var block = _.last(args);
                _.each(items, function (item) {
                    if (item) {
                        result = true;
                    }
                });
                if (result) {
                    return block.inverse(this);
                }
                else {
                    return block.fn(this);
                }
            },
            propertyTitle: function (str) {
                if (_.isString(str)) {
                    return _.chain(str).words().map(function (word) {
                        return _.capitalize(word);
                    }).join(' ');
                }
                return str;
            },
            safeString: function (str) {
                if (_.isString(str)) {
                    return new Handlebars.SafeString(str);
                }
                return str;
            },
            splitDashes: function (str) {
                return str.split('-').join(' ');
            },
            encodeString: function (str) {
                if (_.isString(str)) {
                    return encodeURIComponent(str);
                }
                return str;
            }
        };

        // Export helpers
        for (helper in helpers) {
            if (helpers.hasOwnProperty(helper)) {
                ich.addHelper(helper, helpers[helper]);
            }
        }
    });
