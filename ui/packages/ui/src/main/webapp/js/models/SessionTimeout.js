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
/*jshint browser: true */

var invalidateUrl = '/services/internal/session/invalidate?prevurl=';

var idleNoticeDuration = 60000;
// Length of inactivity that will trigger user timeout (15 minutes in ms by default)
// See STIG V-69243
var idleTimeoutThreshold = 900000;

function getIdleTimeoutDate() {
    return idleTimeoutThreshold + Date.now();
}

function storageAvailable(type) {
    var test = 'test', storage;
    try {
        storage = window[type];
        storage.setItem(test, test);
        storage.removeItem(test);
        return true;
    } catch(e) {
        /*global DOMException */
        return e instanceof DOMException && (
            // everything except Firefox
            e.code === 22 ||
            // Firefox
            e.code === 1014 ||
            // test name field too, because code might not be present
            // everything except Firefox
            e.name === 'QuotaExceededError' ||
            // Firefox
            e.name === 'NS_ERROR_DOM_QUOTA_REACHED') &&
            // acknowledge QuotaExceededError only if there's something already stored
            storage.length !== 0;
    }
}

define([
        'backbone',
        'jquery',
        'underscore',
        'properties'
    ],
    function (Backbone, $, _, properties) {
        idleTimeoutThreshold = parseInt(properties.ui.timeout) > 0 ? parseInt(properties.ui.timeout) : idleTimeoutThreshold;
        console.log(properties);
        console.log("will logout in " + idleTimeoutThreshold + " ms");

        var sessionTimeoutModel = new (Backbone.Model.extend({
            defaults: {
                showPrompt: false,
                idleTimeoutDate: 0
            },
            initialize: function () {
                if (!storageAvailable('localStorage')) {
                    console.log("WARNING: localStorage is unavailable. Unexpected logout may occur.");
                }
                $(window).on("storage", this.handleLocalStorageChange.bind(this));
                this.listenTo(this, 'change:idleTimeoutDate', this.handleIdleTimeoutDate);
                this.listenTo(this, 'change:showPrompt', this.handleShowPrompt);
                this.resetIdleTimeoutDate();
                this.handleShowPrompt();
            },
            handleLocalStorageChange: function() {
                console.log("Activity detected in another tab!");
                this.set('idleTimeoutDate', parseInt(localStorage.getItem('idleTimeoutDate')));
                this.hidePrompt();
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
                    this.startListeningForUserActivity();
                }
            },
            setPromptTimer: function () {
                this.promptTimer = setTimeout(this.showPrompt.bind(this), this.get('idleTimeoutDate') - idleNoticeDuration - Date.now());
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
                this.logoutTimer = setTimeout(this.logout.bind(this), this.get('idleTimeoutDate') - Date.now());
            },
            clearLogoutTimer: function () {
                clearTimeout(this.logoutTimer);
            },
            resetIdleTimeoutDate: function () {
                var idleTimeoutDate = getIdleTimeoutDate();
                localStorage.setItem('idleTimeoutDate', idleTimeoutDate);
                this.set('idleTimeoutDate', idleTimeoutDate);
            },
            startListeningForUserActivity: function () {
                $(document).on('keydown.sessionTimeout mousedown.sessionTimeout', _.throttle(this.resetIdleTimeoutDate.bind(this), 5000));
            },
            stopListeningForUserActivity: function () {
                $(document).off('keydown.sessionTimeout mousedown.sessionTimeout');
            },
            logout: function () {
                window.location.replace(invalidateUrl + window.location.pathname);
            },
            renew: function () {
                this.hidePrompt();
                this.resetIdleTimeoutDate();
            },
            getIdleSeconds: function () {
                return parseInt((this.get('idleTimeoutDate') - Date.now()) / 1000);
            }
        }))();

        return sessionTimeoutModel;
    });
