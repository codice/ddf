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
/*global require, window*/
var properties = require('properties');
var BlockingLightbox = require('component/lightbox/blocking/lightbox.blocking.view');
var SystemUsageView = require('component/system-usage/system-usage.view');
var user = require('component/singletons/user-instance');

function hasMessage() {
    return properties.ui.systemUsageTitle;
}

function hasNotSeenMessage() {
    var systemUsage = window.sessionStorage.getItem("systemUsage");
    if (systemUsage === null) {
        window.sessionStorage.setItem("systemUsage", "{}");
        return true;
    } else {
        return JSON.parse(systemUsage)[user.get('user').get('username')] === undefined;
    }
}

function shownOncePerSession() {
    return properties.ui.systemUsageOncePerSession;
}

function shouldDisplayMessage() {
    if (hasMessage()) {
        if (!shownOncePerSession() || user.get('user').isGuestUser()) {
            return true;
        } else {
            return hasNotSeenMessage();
        }
    } else {
        return false;
    }
}

function displayMessage() {
    if (shouldDisplayMessage()) {
        var blockingLightbox = BlockingLightbox.generateNewLightbox();
        blockingLightbox.model.updateTitle(properties.ui.systemUsageTitle);
        blockingLightbox.model.open();
        blockingLightbox.lightboxContent.show(new SystemUsageView());
    }
}

function attemptToDisplayMessage() {
    if (user.fetched) {
        displayMessage();
    } else {
        user.once('sync', function() {
            attemptToDisplayMessage();
        });
    }
}

attemptToDisplayMessage();