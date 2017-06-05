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
/*global define*/

define([
        'jquery'
    ],
    function ($) {

        $.whenAll = function( firstParam ) {
            var args = arguments,
                sliceDeferred = [].slice,
                i = 0,
                length = args.length,
                count = length,
                rejected,
                deferred = length <= 1 && firstParam && $.isFunction( firstParam.promise ) ? firstParam : $.Deferred();

            function resolveFunc( i, reject ) {
                return function( value ) {
                    rejected = rejected || reject;
                    args[ i ] = arguments.length > 1 ? sliceDeferred.call( arguments, 0 ) : value;
                    if ( !( --count ) ) {
                        // Strange bug in FF4:
                        // Values changed onto the arguments object sometimes end up as undefined values
                        // outside the $.when method. Cloning the object into a fresh array solves the issue
                        var fn = rejected ? deferred.rejectWith : deferred.resolveWith;
                        fn.call(deferred, deferred, sliceDeferred.call( args, 0 ));
                    }
                };
            }

            if ( length > 1 ) {
                for( ; i < length; i++ ) {
                    if ( args[ i ] && $.isFunction( args[ i ].promise ) ) {
                        args[ i ].promise().then( resolveFunc(i), resolveFunc(i, true) );
                    } else {
                        --count;
                    }
                }
                if ( !count ) {
                    deferred.resolveWith( deferred, args );
                }
            } else if ( deferred !== firstParam ) {
                deferred.resolveWith( deferred, length ? [ firstParam ] : [] );
            }
            return deferred.promise();
        };
    });