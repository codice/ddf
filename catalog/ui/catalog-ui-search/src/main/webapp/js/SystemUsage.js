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

function hasMessage() {
    return properties.ui.systemUsageTitle;
}

function hasNotSeenMessage() {
    return window.localStorage.getItem('systemUsage') === null;
}

function shownOncePerSession() {
    return properties.ui.systemUsageOncePerSession;
}

function shouldDisplayMessage() {
    if (hasMessage()) {
        if (shownOncePerSession()) {
            return true;
        } else {
            return hasNotSeenMessage();
        }
    } else {
        return false;
    }
}

if (shouldDisplayMessage()) {
    window.localStorage.setItem('systemUsage', 'true');
    var blockingLightbox = BlockingLightbox.generateNewLightbox();
    blockingLightbox.model.updateTitle(properties.ui.systemUsageTitle);
    blockingLightbox.model.open();
    blockingLightbox.lightboxContent.show(new SystemUsageView());
}