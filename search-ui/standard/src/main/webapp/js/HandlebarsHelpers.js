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
        'moment',
        'handlebars'
    ],
    function (ich, _, moment, Handlebars) {
        "use strict";

        // The module to be exported
        var helper, helpers = {
            /*
             * Handlebars Helper: Moment.js
             * @author: https://github.com/Arkkimaagi
             * Built for Assemble: the static site generator and
             * component builder for Node.js, Grunt.js and Yeoman.
             * http://assemble.io
             *
             * Copyright (c) 2013, Upstage
             * Licensed under the MIT license.
             */
            momentHelp: function (context, block) {
                var momentObj, date, i;
                if (context && context.hash) {
                    block = _.cloneDeep(context);
                    context = undefined;
                }
                momentObj = moment(context);

                // Reset the language back to default before doing anything else
                momentObj.lang('en');

                for (i in block.hash) {
                    if (momentObj[i]) {
                        if(typeof momentObj[i] === 'function') {
                            var func = momentObj[i];
                            date = func.call(momentObj, block.hash[i]);
                        }
                    } else {
                        if(typeof console !== 'undefined') {
                            console.log('moment.js does not support "' + i + '"');
                        }
                    }
                }
                return date;
            },
            duration: function(context, block) {
                if (context && context.hash) {
                    block = _.cloneDeep(context);
                    context = 0;
                }
                var duration = moment.duration(context);

                // Reset the language back to default before doing anything else
                duration = duration.lang('en');

                for (var i in block.hash) {
                    if (duration[i]) {
                        duration = duration[i](block.hash[i]);
                    } else {
                        console.log('moment.js duration does not support "' + i + '"');
                    }
                }
                return duration;
            },
            fileSize: function (item) {
                var bytes = parseInt(item, 10);
                if (isNaN(bytes)) {
                    return item;
                }
                var size, index,
                    type = ['bytes', 'KB', 'MB', 'GB', 'TB'];
                if(bytes === 0) {
                    return "0 bytes";
                }
                else {
                    index = Math.floor(Math.log(bytes) / Math.log(1000));
                    if(index > 4) {
                        index = 4;
                    }

                    size = (bytes / Math.pow(1000, index)).toFixed(index < 2 ? 0 : 1);
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
                if (value && value !== "") {
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
                _.each(items, function(item) {
                    if(!item) {
                        result = false;
                    }
                });
                if(result) {
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
                _.each(items, function(item) {
                    if(item) {
                        result = true;
                    }
                });
                if(result) {
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
                _.each(items, function(item) {
                    if(!item) {
                        result = false;
                    }
                });
                if(result) {
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
                _.each(items, function(item) {
                    if(item) {
                        result = true;
                    }
                });
                if(result) {
                    return block.inverse(this);
                }
                else {
                    return block.fn(this);
                }
            },
            propertyTitle: function (str) {
                if(str && typeof str === "string") {
                    return str.split("-").join(" ").replace(/\w\S*/g, function (word) {
                        return word.charAt(0).toUpperCase() + word.substr(1);
                    });
                }
            },
            safeString: function (str) {
                if(str && typeof str === "string") {
                    return new Handlebars.SafeString(str);
                }
            }
        };

        // Export helpers
        for (helper in helpers) {
            if (helpers.hasOwnProperty(helper)) {
                ich.addHelper(helper, helpers[helper]);
            }
        }
});
