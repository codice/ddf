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
/*global define,FileReader*/
define([
    'underscore',
    'backbone'
], function (_, Backbone) {

    var FileHelper = Backbone.Model.extend({
        isValid: function () {
            return this.get('name') !== undefined;
        },
        resolveType: function (name, type) {
            if (type === '') {
                if (name.lastIndexOf('kar') === (name.length - 3)) {
                    return 'KAR';
                } else if (name.lastIndexOf('jar') === (name.length - 3)) {
                    return 'JAR';
                }
            }
            return type;
        },
        setData: function (data) {
            var file = this.f = data.files[0];
            this.set({
                name: file.name,
                size: file.size,
                type: this.resolveType(file.name.toLowerCase(), file.type)
            });
        },
        load: function (done) {
            var reader = new FileReader();
            reader.onloadend = _.bind(function () {
                this.set('data', reader.result.split(',')[1]);
                done();
            }, this);
            reader.readAsDataURL(this.f);
        },
        toJSON: function () {
            return {
                name: this.get('name'),
                type: this.get('type'),
                size: this.get('size'),
                data: this.get('data')
            };
        }
    });

    return FileHelper;
});
