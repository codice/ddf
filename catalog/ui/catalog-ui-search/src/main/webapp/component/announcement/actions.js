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
var uuid = require('uuid');
var _ = require('underscore');

var remove = exports.remove = function (id) {
    return {
        type: 'REMOVE_ANNOUNCEMENT',
        id: id
    };
};

exports.announce = function (announcement, timeout) {
    var id = uuid.v4();

    return function (dispatch) {
        dispatch({
            type: 'ADD_ANNOUNCEMENT',
            announcement: _.extend({ id: id }, announcement)
        });

        if (announcement.type !== 'error') {
            setTimeout(function () {
                dispatch(remove(id));
            }, timeout || 5000);
        }
    };
};
