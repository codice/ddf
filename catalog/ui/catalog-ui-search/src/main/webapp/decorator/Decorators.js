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
/*global require*/
var _ = require('underscore');

function mergeProperty(baseObject, objectToMerge, property) {
    if (baseObject[property] && ((baseObject[property].constructor !== objectToMerge[property].constructor) || baseObject[property].constructor !== Object)) {
        console.groupCollapsed('Overwriting object property:');
        console.trace();
        console.warn(baseObject[property]);
        console.warn('will be overwritten by ');
        console.warn(objectToMerge[property]);
        console.groupEnd();
        baseObject[property] = objectToMerge[property];
    } else if (baseObject[property]) {
        console.groupCollapsed('Merging object properties:');
        console.trace();
        console.warn(baseObject[property]);
        console.warn('will be merged with');
        console.warn(objectToMerge[property]);
        console.warn('to create');
        var mergedProperty = _.extend(baseObject[property], objectToMerge[property]);
        console.warn(mergedProperty);
        console.groupEnd();
    } else {
        baseObject[property] = objectToMerge[property];
    }
}

function mergeObjects(baseObject, objectToMerge) {
    for (var property in objectToMerge) {
        if (objectToMerge.hasOwnProperty(property)) {
            mergeProperty(baseObject, objectToMerge, property);
        }
    }
}

module.exports = {
    decorate: function() {
        var args = [];
        for (var i = 0; i < arguments.length; ++i) {
            args[i] = arguments[i];
            if (args[i].constructor !== Object) {
                throw 'Each argument to decorate must be an object';
            }
        }
        if (args.length === 0 || args.length === 1) {
            throw 'Must pass at least two arguments to decorate';
        }
        var baseObject = args[0];
        for (i = 1; i < args.length; ++i) {
            mergeObjects(baseObject, args[i]);
        }
        return baseObject;
    }
};