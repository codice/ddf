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
/*global define,FileReader,Image,document,Math,setTimeout*/
/*jshint bitwise: false*/

define([
        'jquery',
        'underscore',
        'backbone',
        'marionette',
        'icanhaz',
        'wreqr',
        'text!templates/ingest/metacardEdit.handlebars',
        'q',
        'spin',
        'spinnerConfig'
    ],
    function ($, _, Backbone, Marionette, ich, wreqr, metacardEditTemplate, Q, Spinner, spinnerConfig) {
        "use strict";

        var Metacard = {};

        ich.addTemplate('metacardEditTemplate', metacardEditTemplate);

        function scaleSize(maxW, maxH, currW, currH){

            var ratio = currH / currW;

            if (currW >= maxW && ratio <= 1){
                currW = maxW;
                currH = currW * ratio;
            } else if(currH >= maxH){
                currH = maxH;
                currW = currH / ratio;
            }

            return [currW, currH];
        }

        /*
         * MIT License
         *  You may use this code as long as you retain this notice.  Use at your own risk! :)
         *  https://github.com/danschumann/limby-resize
         *  0.0.4
         */
        function canvasResize(original, canvas) {

            var
                w1 = original.width,
                h1 = original.height,
                w2 = canvas.width,
                h2 = canvas.height,
                img = original.getContext("2d").getImageData(0, 0, w1, h1),
                img2 = canvas.getContext("2d").getImageData(0, 0, w2, h2);

            if (w2 > w1 || h2 > h1) {
                canvas.getContext('2d').drawImage(original, 0, 0, w2, h2);
                return;
            }


            var data = img.data;
            // we don't use this much, as working with doubles isn't great
            var _data2 = img2.data;

            // We enforce float type for every entity in the array
            // this prevents weird faded lines when things get rounded off
            var data2 = new Array(_data2.length);
            var i;
            for (i = 0; i < _data2.length; i++){
                data2[i] = 0.0;
            }

            // We track alphas, since we need to use alphas to correct colors later on
            var alphas = new Array(_data2.length >> 2);
            for (i = 0; i < _data2.length >> 2; i++){
                alphas[i] = 1;
            }

            // when resizing down, this will be decimal
            var xScale = w2 / w1;
            var yScale = h2 / h1;

            // For every pixel in the original, we tally it up in the new one
            var nextY = function(y1){
                for (var x1 = 0; x1 < w1; x1++) {

                    var

                    // the original pixel is split between two pixels in the output, we do an extra step
                        extraX = false,
                        extraY = false,

                    // the output pixel
                        targetX = Math.floor(x1 * xScale),
                        targetY = Math.floor(y1 * yScale),

                    // The percentage of this pixel going to the output pixel
                        xFactor = xScale,
                        yFactor = yScale,

                    // The percentage of this pixel going to the right neighbor or bottom neighbor
                        bottomFactor = 0,
                        rightFactor = 0,

                    // positions of pixels in the array
                        offset = (y1 * w1 + x1) * 4,
                        targetOffset = (targetY * w2 + targetX) * 4;

                    // Right side goes into another pixel
                    if (targetX < Math.floor((x1 + 1) * xScale)) {

                        rightFactor = (((x1 + 1) * xScale) % 1);
                        xFactor -= rightFactor;

                        extraX = true;

                    }

                    // Bottom side goes into another pixel
                    if (targetY < Math.floor((y1 + 1) * yScale)) {

                        bottomFactor = (((y1 + 1) * yScale) % 1);
                        yFactor -= bottomFactor;

                        extraY = true;

                    }

                    var a;

                    a = (data[offset + 3] / 255);

                    var alphaOffset = targetOffset / 4;

                    if (extraX) {

                        // Since we're not adding the color of invisible pixels,  we multiply by a
                        data2[targetOffset + 4] += data[offset] * rightFactor * yFactor * a;
                        data2[targetOffset + 5] += data[offset + 1] * rightFactor * yFactor * a;
                        data2[targetOffset + 6] += data[offset + 2] * rightFactor * yFactor * a;

                        data2[targetOffset + 7] += data[offset + 3] * rightFactor * yFactor;

                        // if we left out the color of invisible pixels(fully or partly)
                        // the entire average we end up with will no longer be out of 255
                        // so we subtract the percentage from the alpha ( originally 1 )
                        // so that we can reverse this effect by dividing by the amount.
                        // ( if one pixel is black and invisible, and the other is white and visible,
                        // the white pixel will weight itself at 50% because it does not know the other pixel is invisible
                        // so the total(color) for the new pixel would be 128(gray), but it should be all white.
                        // the alpha will be the correct 128, combinging alphas, but we need to preserve the color
                        // of the visible pixels )
                        alphas[alphaOffset + 1] -= (1 - a) * rightFactor * yFactor;
                    }

                    if (extraY) {
                        data2[targetOffset + w2 * 4]     += data[offset] * xFactor * bottomFactor * a;
                        data2[targetOffset + w2 * 4 + 1] += data[offset + 1] * xFactor * bottomFactor * a;
                        data2[targetOffset + w2 * 4 + 2] += data[offset + 2] * xFactor * bottomFactor * a;

                        data2[targetOffset + w2 * 4 + 3] += data[offset + 3] * xFactor * bottomFactor;

                        alphas[alphaOffset + w2] -= (1 - a) * xFactor * bottomFactor;
                    }

                    if (extraX && extraY) {
                        data2[targetOffset + w2 * 4 + 4]     += data[offset] * rightFactor * bottomFactor * a;
                        data2[targetOffset + w2 * 4 + 5] += data[offset + 1] * rightFactor * bottomFactor * a;
                        data2[targetOffset + w2 * 4 + 6] += data[offset + 2] * rightFactor * bottomFactor * a;

                        data2[targetOffset + w2 * 4 + 7] += data[offset + 3] * rightFactor * bottomFactor;

                        alphas[alphaOffset + w2 + 1] -= (1 - a) * rightFactor * bottomFactor;
                    }

                    data2[targetOffset]     += data[offset] * xFactor * yFactor * a;
                    data2[targetOffset + 1] += data[offset + 1] * xFactor * yFactor * a;
                    data2[targetOffset + 2] += data[offset + 2] * xFactor * yFactor * a;

                    data2[targetOffset + 3] += data[offset + 3] * xFactor * yFactor;

                    alphas[alphaOffset] -= (1 - a) * xFactor * yFactor;
                }

                if (y1++ < h1)
                    setTimeout(nextY.bind(this, y1), 0); // this allows other processes to tick
                else
                    done();

            };

            nextY(0);

            var done = function(){

                // fully distribute the color of pixels that are partially full because their neighbor is transparent
                var i;
                for (i = 0; i < (_data2.length >> 2); i++){
                    if (alphas[i] && alphas[i] < 1) {
                        data2[(i<<2)] /= alphas[i];     // r
                        data2[(i<<2) + 1] /= alphas[i]; // g
                        data2[(i<<2) + 2] /= alphas[i]; // b
                    }
                }

                // re populate the actual imgData
                for (i = 0; i < data2.length; i++){
                    _data2[i] = Math.round(data2[i]);
                }

                var context = canvas.getContext("2d");
                context.putImageData(img2, 0, 0);
                deferred.resolve();

            };

            var deferred = Q.defer();
            return deferred.promise;

        }

        Metacard.MetacardEditView = Marionette.ItemView.extend({
            template: 'metacardEditTemplate',
            events: {
                'click .save-metacard': 'saveMetacard',
                'click .cancel-metacard' : 'cancelMetacard',
                'click .delete-metacard' : 'deleteMetacard',
                'change input[name=thumbFile]' : 'setThumbnail'
            },
            spinner: new Spinner(spinnerConfig),
            initialize: function () {
                this.modelBinder = new Backbone.ModelBinder();
            },
            onRender: function () {
                var metacardBindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
                this.modelBinder.bind(this.model.get('properties'), this.$el, metacardBindings);
                this.$('#effective-time-ingest').datetimepicker({
                    dateFormat: $.datepicker.ATOM,
                    timeFormat: "HH:mm:ss.lz",
                    separator: "T",
                    timezoneIso8601: true,
                    useLocalTimezone: true,
                    showHour: true,
                    showMinute: true,
                    showSecond: false,
                    showMillisec: false,
                    showTimezone: false,
                    minDate: new Date(100, 0, 2),
                    maxDate: new Date(9999, 11, 30),
                    beforeShow: this.beforeShowDatePicker
                });
            },
            beforeShowDatePicker: function(picker){
                picker.style.zIndex = 200;
            },
            cancelMetacard: function () {
                wreqr.vent.trigger('ingest:metacardEditDone');
                this.destroy();
            },
            saveMetacard: function () {
                var view = this;
                $.ajax({
                    type: "PUT",
                    url: '/services/catalog/' + this.model.get('properties>id'),
                    data: JSON.stringify(this.model.toJSON()),
                    contentType: "application/json",
                    error: function(e) {
                        view.$('.error-text').val(e);
                    },
                    success: function() {
                        view.$('.error-text').val('');
                        view.destroy();
                    }
                });
                wreqr.vent.trigger('ingest:metacardEditDone');
            },
            deleteMetacard: function () {
                var view = this;
                $.ajax({
                    type: "DELETE",
                    url: '/services/content/' + this.model.get('properties>resource-uri').split(':')[1],
                    error: function(e) {
                        view.$('.error-text').val(e);
                    },
                    success: function() {
                        view.$('.error-text').val('');
                        view.destroy();
                    }
                });
                wreqr.vent.trigger('ingest:metacardEditDone');
            },
            setThumbnail: function () {
                var view = this;
                var img, file = this.$('input[name=thumbFile]')[0].files[0];
                var fileReader = new FileReader();
                this.spinner.spin(this.el);
                fileReader.onload = function () {
                    img = new Image();
                    img.onload = function () {
                        var canvas1 = document.createElement('canvas');
                        canvas1.width = img.width;
                        canvas1.height = img.height;
                        var canvas2 = document.createElement('canvas');
                        var size = scaleSize(200, 200, img.width, img.height);
                        canvas2.width = size[0];
                        canvas2.height = size[1];
                        var ctx1 = canvas1.getContext('2d');
                        ctx1.drawImage(img, 0, 0);
                        canvasResize(canvas1, canvas2).done(function () {
                            var dataUrl = canvas2.toDataURL('image/png');
                            var parts = dataUrl.split(',');
                            view.model.set('properties>thumbnail', parts[1]);
                            view.spinner.stop();
                            view.render();
                        });
                    };
                    img.src = fileReader.result;
                };
                fileReader.readAsDataURL(file);
            }
        });

        return Metacard;

    });