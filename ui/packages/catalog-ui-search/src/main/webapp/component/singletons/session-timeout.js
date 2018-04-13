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
/*global require*/
//meant to be used for just in time feature detection

var Backbone = require('backbone');
var $ = require('jquery');
var _ = require('underscore');
var properties = require('properties');

var invalidateUrl = '/services/internal/session/invalidate?prevurl=';

var idleNoticeDuration = 60000;
// Length of inactivity that will trigger user timeout (15 minutes in ms by default)
// See STIG V-69243
var idleTimeoutThreshold = parseInt(properties.ui.timeout) > 0 ? parseInt(properties.ui.timeout) : 900000;

function getIdleTimeoutDate() {
    return idleTimeoutThreshold + Date.now();
}

var sessionTimeoutModel = new (Backbone.Model.extend({
    defaults: {
        showPrompt: false,
        idleTimeoutDate: 0
    },
    initialize: function () {
        this.listenTo(this, 'change:idleTimeoutDate', this.handleIdleTimeoutDate);
        this.listenTo(this, 'change:showPrompt', this.handleShowPrompt);
        this.handleShowPrompt();
    },
    handleIdleTimeoutDate: function () {
        this.clearPromptTimer();
        this.setPromptTimer();
        this.clearLogoutTimer();
        this.setLogoutTimer();
    },
    handleShowPrompt: function () {
        if (this.get('showPrompt')) {
            this.stopListeningForUserActivity();
        } else {
            this.resetIdleTimeoutDate();
            this.startListeningForUserActivity();
        }
    },
    setPromptTimer: function () {
        var timeout = this.get('idleTimeoutDate') - idleNoticeDuration - Date.now();
        timeout = Math.max(0, timeout);
        this.promptTimer = setTimeout(this.showPrompt.bind(this), timeout);
    },
    showPrompt: function () {
        this.set('showPrompt', true);
    },
    hidePrompt: function () {
        this.set('showPrompt', false);
    },
    clearPromptTimer: function () {
        clearTimeout(this.promptTimer);
    },
    setLogoutTimer: function () {
        var timeout = this.get('idleTimeoutDate') - Date.now();
        timeout = Math.max(0, timeout);
        this.logoutTimer = setTimeout(this.logout.bind(this), timeout);
    },
    clearLogoutTimer: function () {
        clearTimeout(this.logoutTimer);
    },
    resetIdleTimeoutDate: function () {
        this.set('idleTimeoutDate', getIdleTimeoutDate());
    },
    startListeningForUserActivity: function () {
        $(document).on('keydown.sessionTimeout mousedown.sessionTimeout mousemove.sessionTimeout', _.throttle(this.resetIdleTimeoutDate.bind(this), 5000));
    },
    stopListeningForUserActivity: function () {
        $(document).off('keydown.sessionTimeout mousedown.sessionTimeout mousemove.sessionTimeout');
    },
    logout: function () {
        if (window.onbeforeunload != null) {
          window.onbeforeunload = null;
        }
        window.location.replace(invalidateUrl + window.location.pathname);
    },
    renew: function () {
        this.hidePrompt();
    },
    getIdleSeconds: function () {
        return parseInt((this.get('idleTimeoutDate') - Date.now()) / 1000);
    }
}))();

module.exports = sessionTimeoutModel;
