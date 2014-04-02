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
/*global define*/
define(['backbone'], function (Backbone) {

    var Installer = {};

    var _step = function (direction) {
        var changeObj = {};
        changeObj.stepNumber = this.get('stepNumber') + direction;
        if(changeObj.stepNumber < this.get('totalSteps')) {
            changeObj.hasNext = true;
        } else {
            changeObj.stepNumber = this.get('totalSteps');
            changeObj.hasNext = false;
        }

        if(changeObj.stepNumber > 0) {
            changeObj.hasPrevious = true;
        } else {
            changeObj.stepNumber = 0;
            changeObj.hasPrevious = false;
        }

        changeObj.percentComplete = Math.round(changeObj.stepNumber / this.get('totalSteps') * 100);

        return changeObj;
    };

    Installer.Model = Backbone.Model.extend({
        defaults: {
            hasNext: true,
            hasPrevious: false,
            totalSteps: 3,
            stepNumber: 0,
            percentComplete: 0
        },
        nextStep: function() {
            this.set(_step.call(this, 1));
        },
        previousStep: function() {
            this.set(_step.call(this, -1));
        }
    });

    return Installer;

});