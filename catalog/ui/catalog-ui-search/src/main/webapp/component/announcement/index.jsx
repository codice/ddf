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

var $ = require('jquery');
var React = require('react');
var render = require('react-dom').render;

var reducer = require('./reducer');
var actions = require('./actions');

var Announcments = require('./announcements');

var region = $('<div id="announcments">').get(0);
$(window.document.body).append(region);


var state = reducer();

var dispatch = function (action) {
    if (typeof action === 'function') {
        action(dispatch);
    } else {
        state = reducer(state, action);
        render(<Announcments list={state} dispatch={dispatch} />, region);
    }
};

exports.announce = function (announcement) {
    dispatch(actions.announce(announcement));
};

if (process.env.NODE_ENV !== 'production') {
    module.hot.accept();
}
