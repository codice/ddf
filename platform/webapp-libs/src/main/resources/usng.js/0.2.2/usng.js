// Copyright (c) 2009 Larry Moore, larmoor@gmail.com
//               2014 Mike Adair, Richard Greenwood, Didier Richard, Stephen Irons, Olivier Terral and Calvin Metcalf (proj4js)
//               2014 Codice Foundation
// Released under the MIT License; see
// http://www.opensource.org/licenses/mit-license.php
// or http://en.wikipedia.org/wiki/MIT_License
//
// Permission is hereby granted, free of charge, to any person
// obtaining a copy of this software and associated documentation
// files (the "Software"), to deal in the Software without
// restriction, including without limitation the rights to use,
// copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following
// conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
// OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
// HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
// WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
// OTHER DEALINGS IN THE SOFTWARE.

/*global define,Math,parseInt,parseFloat*/

/**
 * This library was modified from the original usngs.js script to fix bugs, edge conditions, and limitations
 * dealing with the different precision levels in usng/mgrs. It is by no means perfect! If you find a bug,
 * submit it!
 */
(function(root, factory) {

    if (typeof define === 'function' && define.amd) {
        define(['exports'], function (exports) {
            root.usngs = factory(root, exports);
        });
    } else if (typeof exports !== 'undefined') {
        factory(root, exports);
    } else {
        root.usngs = factory(root, {});
    }
}(this, function (root, usngs) {

    function extend(objToExtend, obj) {
        var keys = [];
        for (var key in obj) {
            keys.push(key);
        }
        var length = keys.length;
        for (var i = 0; i < length; i++) {
            objToExtend[keys[i]] = obj[keys[i]];
        }
        return objToExtend;
    }

    usngs.Converter = function (options) {
        options || (options = {});
        this.initialize.apply(this, [options]);
    };

    extend(usngs.Converter.prototype, {

        ngFunctionsPresent: true,
        UNDEFINED_STR: "undefined",


        /********************************* Constants ********************************/

        FOURTHPI: Math.PI / 4,
        DEG_2_RAD: Math.PI / 180,
        RAD_2_DEG: 180.0 / Math.PI,
        BLOCK_SIZE: 100000, // size of square identifier (within grid zone designation),
        // (meters)

        IS_NAD83_DATUM: true,  // if false, assumes NAD27 datum

        // For diagram of zone sets, please see the "United States National Grid" white paper.
        GRIDSQUARE_SET_COL_SIZE: 8,  // column width of grid square set
        GRIDSQUARE_SET_ROW_SIZE: 20, // row height of grid square set

        // UTM offsets
        EASTING_OFFSET: 500000.0,   // (meters)
        NORTHING_OFFSET: 10000000.0, // (meters)

        // scale factor of central meridian
        k0: 0.9996,

        EQUATORIAL_RADIUS: undefined,
        ECC_PRIME_SQUARED: undefined,
        ECC_SQUARED: undefined,

        num100kSets: 6,
        originRowLetters: 'AFAFAF',
        UTMGzdLetters:"NPQRSTUVWX",
        USNGSqEast: "ABCDEFGHJKLMNPQRSTUVWXYZ",
        USNGSqLetOdd: "ABCDEFGHJKLMNPQRSTUV",
        USNGSqLetEven: "FGHJKLMNPQRSTUVABCDE",

        initialize: function (options) {
            if (options.datum && options.datum.toUpperCase() === 'NAD27') {
                this.IS_NAD83_DATUM = false;
            }

            // check for NAD83
            if (this.IS_NAD83_DATUM) {
                this.EQUATORIAL_RADIUS    = 6378137.0; // GRS80 ellipsoid (meters)
                this.ECC_SQUARED = 0.006694380023;
            }
            // else NAD27 datum is assumed
            else {
                this.EQUATORIAL_RADIUS    = 6378206.4;  // Clarke 1866 ellipsoid (meters)
                this.ECC_SQUARED = 0.006768658;
            }

            this.ECC_PRIME_SQUARED = this.ECC_SQUARED / (1 - this.ECC_SQUARED);

            this.E1 = (1 - Math.sqrt(1 - this.ECC_SQUARED)) / (1 + Math.sqrt(1 - this.ECC_SQUARED));
        },



        // Number of digits to display for x,y coords
        //  One digit:    10 km precision      eg. "18S UJ 2 1"
        //  Two digits:   1 km precision       eg. "18S UJ 23 06"
        //  Three digits: 100 meters precision eg. "18S UJ 234 064"
        //  Four digits:  10 meters precision  eg. "18S UJ 2348 0647"
        //  Five digits:  1 meter precision    eg. "18S UJ 23480 06470"


        /************* retrieve zone number from latitude, longitude *************

         Zone number ranges from 1 - 60 over the range [-180 to +180]. Each
         range is 6 degrees wide. Special cases for points outside normal
         [-80 to +84] latitude zone.

         *************************************************************************/

        getZoneNumber: function (lat, lon) {

            lat = parseFloat(lat);
            lon = parseFloat(lon);

            // sanity check on input
            if (lon > 360 || lon < -180 || lat > 84 || lat < -80) {
                throw new Error('usng.js, getZoneNumber: invalid input. lat: ' + lat.toFixed(4) + ' lon: ' + lon.toFixed(4));
            }

            // convert 0-360 to [-180 to 180] range
            var lonTemp = (lon + 180) - parseInt((lon + 180) / 360) * 360 - 180;
            var zoneNumber = parseInt((lonTemp + 180) / 6) + 1;

            // Handle special case of west coast of Norway
            if ( lat >= 56.0 && lat < 64.0 && lonTemp >= 3.0 && lonTemp < 12.0 ) {
                zoneNumber = 32;
            }

            // Special zones for Svalbard
            if ( lat >= 72.0 && lat < 84.0 ) {
                if ( lonTemp >= 0.0  && lonTemp <  9.0 ) {
                    zoneNumber = 31;
                }
                else if ( lonTemp >= 9.0  && lonTemp < 21.0 ) {
                    zoneNumber = 33;
                }
                else if ( lonTemp >= 21.0 && lonTemp < 33.0 ) {
                    zoneNumber = 35;
                }
                else if ( lonTemp >= 33.0 && lonTemp < 42.0 ) {
                    zoneNumber = 37;
                }
            }
            return zoneNumber;
        },

        LLtoKM: function(lat1, lon1, lat2, lon2) {
          var R = 6371000; // metres
            var phi1 = lat1* this.DEG_2_RAD;
            var phi2 = lat2* this.DEG_2_RAD;
            var deltaPhi = (lat2-lat1)* this.DEG_2_RAD;
            var deltaLlamda= (lon2-lon1)* this.DEG_2_RAD;


            var a = Math.sin(deltaPhi/2) * Math.sin(deltaPhi/2) +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(deltaLlamda/2) * Math.sin(deltaLlamda/2);
            var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

            return R * c;
        },

        //this function does a very rough "best fit" to the center point
        //this could definitely be improved
        LLBboxtoUSNG: function (north, south, east, west) {
            var northNum = parseFloat(north);
            var southNum = parseFloat(south);
            var eastNum = parseFloat(east);
            var westNum = parseFloat(west);

            // calculate midpoints for use in USNG string calculation
            var lat = (northNum + southNum) / 2;
            var lon = (eastNum + westNum) / 2;

            // round down edge cases
            if (lon >= 180) {
                lon = 179.9;
            } else if (lon <= -180) {
                lon = -179.9;
            }

            // round down edge cases
            if (lat >= 90) {
                lat = 89.9;
            } else if (lat <= -90) {
                lat = -89.9;
            }

            // calculate distance between two points (North, West) and (South, East)
            var R = 6371000; // metres
            var phi1 = northNum* this.DEG_2_RAD;
            var phi2 = southNum* this.DEG_2_RAD;
            var deltaPhi = (southNum-northNum)* this.DEG_2_RAD;
            var deltaLlamda= (westNum-eastNum)* this.DEG_2_RAD;

            // trigonometry calculate distance

            var height = Math.sin(deltaPhi/2) * Math.sin(deltaPhi/2);
            height = R * 2 * Math.atan2(Math.sqrt(height), Math.sqrt(1-height));
            var length = Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(deltaLlamda/2) * Math.sin(deltaLlamda/2);
            length = R * 2 * Math.atan2(Math.sqrt(length), Math.sqrt(1-length));

            var dist = Math.max(height, length);
            // divide distance by square root of two


            if (lon === 0 && (eastNum > 90 || eastNum < -90) && (westNum > 90 || westNum < -90)) {
                lon = 180;
              }
            // calculate a USNG string with a precision based on distance
            // precision is defined in LLtoUSNG declaration
            var result;
            if (dist > 100000) {
              result = this.LLtoUSNG(lat, lon, 0);
            } else if (dist > 10000) {
              result = this.LLtoUSNG(lat, lon, 1);
            } else if (dist > 1000) {
              result = this.LLtoUSNG(lat, lon, 2);
            } else if (dist > 100) {
              result = this.LLtoUSNG(lat, lon, 3);
            } else if (dist > 10) {
              result = this.LLtoUSNG(lat, lon, 4);
            } else if (dist > 1) {
              result = this.LLtoUSNG(lat, lon, 5);
            } else if (dist >=0) {
                result = this.LLtoUSNG(lat, lon, 6);
            }

            // result is a USNG string of the form DDL LL DDDDD DDDDD
            // length of string will be based on the precision variable
            return result;
        },

        /***************** convert latitude, longitude to UTM  *******************

         Converts lat/long to UTM coords.  Equations from USGS Bulletin 1532
         (or USGS Professional Paper 1395 "Map Projections - A Working Manual",
         by John P. Snyder, U.S. Government Printing Office, 1987.)

         East Longitudes are positive, West longitudes are negative.
         North latitudes are positive, South latitudes are negative
         lat and lon are in decimal degrees

         output is in the input array utmcoords
         utmcoords[0] = easting
         utmcoords[1] = northing (NEGATIVE value in southern hemisphere)
         utmcoords[2] = zone

         ***************************************************************************/
        LLtoUTM: function (lat,lon,utmcoords,zone) {
            // utmcoords is a 2-D array declared by the calling routine
            // note: input of lon = 180 or -180 with zone 60 not allowed; use 179.9999

            lat = parseFloat(lat);
            lon = parseFloat(lon);

        // Constrain reporting USNG coords to the latitude range [80S .. 84N]
        /////////////////
            if (lat > 84.0 || lat < -80.0){
                return this.UNDEFINED_STR;
            }
        //////////////////////


            // sanity check on input - turned off when testing with Generic Viewer
            if (lon > 360 || lon < -180 || lat > 90 || lat < -90) {
                throw new Error('usng.js, LLtoUTM, invalid input. lat: ' + lat.toFixed(4) + ' lon: ' + lon.toFixed(4));
            }


            // Make sure the longitude is between -180.00 .. 179.99..
            // Convert values on 0-360 range to this range.
            var lonTemp = (lon + 180) - parseInt((lon + 180) / 360) * 360 - 180;
            var latRad = lat     * this.DEG_2_RAD;
            var lonRad = lonTemp * this.DEG_2_RAD;

            // user-supplied zone number will force coordinates to be computed in a particular zone
            var zoneNumber;
            if (!zone) {
                zoneNumber = this.getZoneNumber(lat, lon);
            }
            else {
                zoneNumber = zone
            }

            var lonOrigin = (zoneNumber - 1) * 6 - 180 + 3;  // +3 puts origin in middle of zone
            var lonOriginRad = lonOrigin * this.DEG_2_RAD;

            // compute the UTM Zone from the latitude and longitude
            var UTMZone = zoneNumber + "" + this.UTMLetterDesignator(lat) + " ";

            var N = this.EQUATORIAL_RADIUS / Math.sqrt(1 - this.ECC_SQUARED * Math.sin(latRad) * Math.sin(latRad));
            var T = Math.tan(latRad) * Math.tan(latRad);
            var C = this.ECC_PRIME_SQUARED * Math.cos(latRad) * Math.cos(latRad);
            var A = Math.cos(latRad) * (lonRad - lonOriginRad);

            // Note that the term Mo drops out of the "M" equation, because phi
            // (latitude crossing the central meridian, lambda0, at the origin of the
            //  x,y coordinates), is equal to zero for UTM.
            var M = this.EQUATORIAL_RADIUS * (( 1 - this.ECC_SQUARED / 4
            - 3 * (this.ECC_SQUARED * this.ECC_SQUARED) / 64
            - 5 * (this.ECC_SQUARED * this.ECC_SQUARED * this.ECC_SQUARED) / 256) * latRad
            - ( 3 * this.ECC_SQUARED / 8 + 3 * this.ECC_SQUARED * this.ECC_SQUARED / 32
            + 45 * this.ECC_SQUARED * this.ECC_SQUARED * this.ECC_SQUARED / 1024)
            * Math.sin(2 * latRad) + (15 * this.ECC_SQUARED * this.ECC_SQUARED / 256
            + 45 * this.ECC_SQUARED * this.ECC_SQUARED * this.ECC_SQUARED / 1024) * Math.sin(4 * latRad)
            - (35 * this.ECC_SQUARED * this.ECC_SQUARED * this.ECC_SQUARED / 3072) * Math.sin(6 * latRad));

            var UTMEasting = (this.k0 * N * (A + (1 - T + C) * (A * A * A) / 6
            + (5 - 18 * T + T * T + 72 * C - 58 * this.ECC_PRIME_SQUARED )
            * (A * A * A * A * A) / 120)
            + this.EASTING_OFFSET);

            var UTMNorthing = (this.k0 * (M + N * Math.tan(latRad) * ( (A * A) / 2 + (5 - T + 9
            * C + 4 * C * C ) * (A * A * A * A) / 24
            + (61 - 58 * T + T * T + 600 * C - 330 * this.ECC_PRIME_SQUARED )
            * (A * A * A * A * A * A) / 720)));

            utmcoords[0] = UTMEasting;
            utmcoords[1] = UTMNorthing;
            utmcoords[2] = zoneNumber;
        },


        /***************** convert latitude, longitude to USNG  *******************
         Converts lat/lng to USNG coordinates.  Calls LLtoUTM first, then
         converts UTM coordinates to a USNG string.

         Returns string of the format: DDL LL DDDD DDDD (4-digit precision), eg:
         "18S UJ 2286 0705" locates Washington Monument in Washington, D.C.
         to a 10-meter precision.

         Precision refers to levels of USNG precision. Ie a precision of
         0 returns a string in the form DDL
         1 returns a string in the form DDL LL
         2 returns a string in the form DDL LL D D
         etc

         ***************************************************************************/

        LLtoUSNG: function (lat, lon, precision) {

            // make lon between -180 & 180
            if (lon < -180) { lon += 360;}
            else if (lon > 180) { lon -= 360;}

            // parse lat & long parameters to floats
            lat = parseFloat(lat);
            lon = parseFloat(lon);

            // convert lat/lon to UTM coordinates
            var coords = [];
            this.LLtoUTM(lat, lon, coords);
            var UTMEasting = coords[0];
            var UTMNorthing = coords[1];

            // ...then convert UTM to USNG

            // southern hemisphere case
            if (lat < 0) {
                // Use offset for southern hemisphere
                UTMNorthing += this.NORTHING_OFFSET;
            }

            var zoneNumber = this.getZoneNumber(lat, lon);
            var USNGLetters  = this.findGridLetters(zoneNumber, UTMNorthing, UTMEasting);

            // UTM northing and easting is the analogue of USNG letters + USNG northing and easting
            // so remove the component of UTM northing and easting that corresponds with the USNG letters
            var USNGNorthing = Math.round(UTMNorthing) % this.BLOCK_SIZE;
            var USNGEasting  = Math.round(UTMEasting)  % this.BLOCK_SIZE;

            // parse precision to something we understand
            if (typeof precision === 'undefined' || precision < 0) {
                precision = 0;
            }

            // digitPrecision is to account for just the numerical portion of the USNG string
            // the last 0-10 characters of the USNG string
            var digitPrecision = 0;

            // ensure that digitPrecision is between 0-5 because USNG is specified to up to 5 digits
            if (precision > 0) {
                digitPrecision = precision-1;
            }
            if (digitPrecision > 5) {
                digitPrecision = 5;
            }
            // truncate USNG string digits to achieve specified precision
            USNGNorthing = Math.floor(USNGNorthing / Math.pow(10,(5-digitPrecision)));
            USNGEasting = Math.floor(USNGEasting / Math.pow(10,(5-digitPrecision)));

            // begin building USNG string "DDL"
            var USNG = zoneNumber + this.UTMLetterDesignator(lat);

            // add 100k meter grid letters to USNG string "DDL LL"
            if (precision >= 1) {
             USNG += " " + USNGLetters;
            }


            // REVISIT: Modify to incorporate dynamic precision ?

            // if requested precision is higher than USNG northing or easting, pad front
            // with zeros

            // add easting and northing to USNG string "DDL LL D+ D+"
            if (digitPrecision >= 1) {
                USNG += " "
                for (var i = String(USNGEasting).length; i < digitPrecision; i++) {
                        USNG += "0";
                }
                USNG += USNGEasting + " ";
            }

            if (digitPrecision >= 1) {
                for (i = String(USNGNorthing).length; i < digitPrecision; i++) {
                        USNG += "0";
                }
                USNG += USNGNorthing;
            }

            // return USNG string of the form "DDL LL DDDDD DDDDD"
            // length of string depends on precision specified
            return USNG;

        },


        /************** retrieve grid zone designator letter **********************

         This routine determines the correct UTM letter designator for the given
         latitude returns 'Z' if latitude is outside the UTM limits of 84N to 80S

         Returns letter designator for a given latitude.
         Letters range from C (-80 lat) to X (+84 lat), with each zone spanning
         8 degrees of latitude.

         ***************************************************************************/

        UTMLetterDesignator: function (lat) {
            lat = parseFloat(lat);

            var letterDesignator;
            if ((84 >= lat) && (lat >= 72))
                letterDesignator = 'X';
            else if ((72 > lat) && (lat >= 64))
                letterDesignator = 'W';
            else if ((64 > lat) && (lat >= 56))
                letterDesignator = 'V';
            else if ((56 > lat) && (lat >= 48))
                letterDesignator = 'U';
            else if ((48 > lat) && (lat >= 40))
                letterDesignator = 'T';
            else if ((40 > lat) && (lat >= 32))
                letterDesignator = 'S';
            else if ((32 > lat) && (lat >= 24))
                letterDesignator = 'R';
            else if ((24 > lat) && (lat >= 16))
                letterDesignator = 'Q';
            else if ((16 > lat) && (lat >= 8))
                letterDesignator = 'P';
            else if (( 8 > lat) && (lat >= 0))
                letterDesignator = 'N';
            else if (( 0 > lat) && (lat >= -8))
                letterDesignator = 'M';
            else if ((-8> lat) && (lat >= -16))
                letterDesignator = 'L';
            else if ((-16 > lat) && (lat >= -24))
                letterDesignator = 'K';
            else if ((-24 > lat) && (lat >= -32))
                letterDesignator = 'J';
            else if ((-32 > lat) && (lat >= -40))
                letterDesignator = 'H';
            else if ((-40 > lat) && (lat >= -48))
                letterDesignator = 'G';
            else if ((-48 > lat) && (lat >= -56))
                letterDesignator = 'F';
            else if ((-56 > lat) && (lat >= -64))
                letterDesignator = 'E';
            else if ((-64 > lat) && (lat >= -72))
                letterDesignator = 'D';
            else if ((-72 > lat) && (lat >= -80))
                letterDesignator = 'C';
            else
                letterDesignator = 'Z'; // This is here as an error flag to show
                                        // that the latitude is outside the UTM limits
            return letterDesignator;
        },


        /****************** Find the set for a given zone. ************************

         There are six unique sets, corresponding to individual grid numbers in
         sets 1-6, 7-12, 13-18, etc. Set 1 is the same as sets 7, 13, ..; Set 2
         is the same as sets 8, 14, ..

         See p. 10 of the "United States National Grid" white paper.

         ***************************************************************************/

        findSet: function (zoneNum) {

            zoneNum = parseInt(zoneNum);
            zoneNum = zoneNum % 6;
            switch (zoneNum) {

                case 0:
                    return 6;
                    break;

                case 1:
                    return 1;
                    break;

                case 2:
                    return 2;
                    break;

                case 3:
                    return 3;
                    break;

                case 4:
                    return 4;
                    break;

                case 5:
                    return 5;
                    break;

                default:
                    return -1;
                    break;
            }
        },


        /**************************************************************************
         Retrieve the square identification for a given coordinate pair & zone
         See "lettersHelper" function documentation for more details.

         ***************************************************************************/

        findGridLetters: function(zoneNum, northing, easting) {

            zoneNum  = parseInt(zoneNum);
            northing = parseFloat(northing);
            easting  = parseFloat(easting);
            var row = 1;

            // northing coordinate to single-meter precision
            var north_1m = Math.round(northing);

            // Get the row position for the square identifier that contains the point
            while (north_1m >= this.BLOCK_SIZE) {
                north_1m = north_1m - this.BLOCK_SIZE;
                row++;
            }

            // cycle repeats (wraps) after 20 rows
            row = row % this.GRIDSQUARE_SET_ROW_SIZE;
            var col = 0;

            // easting coordinate to single-meter precision
            var east_1m = Math.round(easting);

            // Get the column position for the square identifier that contains the point
            while (east_1m >= this.BLOCK_SIZE){
                east_1m = east_1m - this.BLOCK_SIZE;
                col++;
            }

            // cycle repeats (wraps) after 8 columns
            col = col % this.GRIDSQUARE_SET_COL_SIZE;

            return this.lettersHelper(this.findSet(zoneNum), row, col);
        },


        /**************************************************************************
         Retrieve the Square Identification (two-character letter code), for the
         given row, column and set identifier (set refers to the zone set:
         zones 1-6 have a unique set of square identifiers; these identifiers are
         repeated for zones 7-12, etc.)

         See p. 10 of the "United States National Grid" white paper for a diagram
         of the zone sets.

         ***************************************************************************/

        lettersHelper: function (setter, row, col) {

            // handle case of last row
            if (row == 0) {
                row = this.GRIDSQUARE_SET_ROW_SIZE - 1;
            }
            else {
                row--;
            }

            // handle case of last column
            if (col == 0) {
                col = this.GRIDSQUARE_SET_COL_SIZE - 1;
            }
            else {
                col--;
            }

            var l1, l2;
            switch(setter) {

                case 1:
                    l1="ABCDEFGH";              // column ids
                    l2=this.USNGSqLetOdd;  // row ids
                    return l1.charAt(col) + l2.charAt(row);
                    break;

                case 2:
                    l1="JKLMNPQR";
                    l2=this.USNGSqLetEven;
                    return l1.charAt(col) + l2.charAt(row);
                    break;

                case 3:
                    l1="STUVWXYZ";
                    l2=this.USNGSqLetOdd;
                    return l1.charAt(col) + l2.charAt(row);
                    break;

                case 4:
                    l1="ABCDEFGH";
                    l2=this.USNGSqLetEven;
                    return l1.charAt(col) + l2.charAt(row);
                    break;

                case 5:
                    l1="JKLMNPQR";
                    l2=this.USNGSqLetOdd;
                    return l1.charAt(col) + l2.charAt(row);
                    break;

                case 6:
                    l1="STUVWXYZ";
                    l2=this.USNGSqLetEven;
                    return l1.charAt(col) + l2.charAt(row);
                    break;
            }
        },


        /**************  convert UTM coords to decimal degrees *********************

         Equations from USGS Bulletin 1532 (or USGS Professional Paper 1395)
         East Longitudes are positive, West longitudes are negative.
         North latitudes are positive, South latitudes are negative.

         Expected Input args:
         UTMNorthing   : northing-m (numeric), eg. 432001.8
         southern hemisphere NEGATIVE from equator ('real' value - 10,000,000)
         UTMEasting    : easting-m  (numeric), eg. 4000000.0
         UTMZoneNumber : 6-deg longitudinal zone (numeric), eg. 18

         lat-lon coordinates are turned in the object 'ret' : ret.lat and ret.lon

         ***************************************************************************/

        UTMtoLL: function (UTMNorthing, UTMEasting, UTMZoneNumber, accuracy) {

            // remove 500,000 meter offset for longitude
            var xUTM = parseFloat(UTMEasting) - this.EASTING_OFFSET;
            var yUTM = parseFloat(UTMNorthing);
            var zoneNumber = parseInt(UTMZoneNumber);

            // origin longitude for the zone (+3 puts origin in zone center)
            var lonOrigin = (zoneNumber - 1) * 6 - 180 + 3;

            // M is the "true distance along the central meridian from the Equator to phi
            // (latitude)
            var M = yUTM / this.k0;
            var mu = M / ( this.EQUATORIAL_RADIUS * (1 - this.ECC_SQUARED / 4 - 3 * this.ECC_SQUARED *
                this.ECC_SQUARED / 64 - 5 * this.ECC_SQUARED * this.ECC_SQUARED * this.ECC_SQUARED / 256 ));

            // phi1 is the "footprint latitude" or the latitude at the central meridian which
            // has the same y coordinate as that of the point (phi (lat), lambda (lon) ).
            var phi1Rad = mu + (3 * this.E1 / 2 - 27 * this.E1 * this.E1 * this.E1 / 32 ) * Math.sin( 2 * mu)
                + ( 21 * this.E1 * this.E1 / 16 - 55 * this.E1 * this.E1 * this.E1 * this.E1 / 32) * Math.sin( 4 * mu)
                + (151 * this.E1 * this.E1 * this.E1 / 96) * Math.sin(6 * mu);
            var phi1 = phi1Rad * this.RAD_2_DEG;

            // Terms used in the conversion equations
            var N1 = this.EQUATORIAL_RADIUS / Math.sqrt( 1 - this.ECC_SQUARED * Math.sin(phi1Rad) *
                Math.sin(phi1Rad));
            var T1 = Math.tan(phi1Rad) * Math.tan(phi1Rad);
            var C1 = this.ECC_PRIME_SQUARED * Math.cos(phi1Rad) * Math.cos(phi1Rad);
            var R1 = this.EQUATORIAL_RADIUS * (1 - this.ECC_SQUARED) / Math.pow(1 - this.ECC_SQUARED *
                Math.sin(phi1Rad) * Math.sin(phi1Rad), 1.5);
            var D = xUTM / (N1 * this.k0);

            // Calculate latitude, in decimal degrees
            var lat = phi1Rad - ( N1 * Math.tan(phi1Rad) / R1) * (D * D / 2 - (5 + 3 * T1 + 10
                * C1 - 4 * C1 * C1 - 9 * this.ECC_PRIME_SQUARED) * D * D * D * D / 24 + (61 + 90 *
                T1 + 298 * C1 + 45 * T1 * T1 - 252 * this.ECC_PRIME_SQUARED - 3 * C1 * C1) * D * D *
                D * D * D * D / 720);
            lat = lat * this.RAD_2_DEG;

            if (lat === 0) {
                lat = 0.001;
            }

            // Calculate longitude, in decimal degrees
            var lon = (D - (1 + 2 * T1 + C1) * D * D * D / 6 + (5 - 2 * C1 + 28 * T1 - 3 *
                C1 * C1 + 8 * this.ECC_PRIME_SQUARED + 24 * T1 * T1) * D * D * D * D * D / 120) /
                Math.cos(phi1Rad);

            lon = lonOrigin + lon * this.RAD_2_DEG;
            var result = {};
            if (accuracy) {
                if (accuracy <= 100000) {
                    var northEast = this.UTMtoLL(UTMNorthing + accuracy, UTMEasting + accuracy, UTMZoneNumber);
                    result.north = northEast.lat;
                    result.east = northEast.lon;
                    result.south = lat;
                    result.west = lon;
                } else {
                    var zoneLetter = this.UTMLetterDesignator(lat);
                    var lats = this.zoneLetterLats(zoneLetter);
                    var lons = this.zoneNumberLons(UTMZoneNumber);
                    if (lats && lons) {
                        result.north = lats.north;
                        result.south = lats.south;
                        result.east = lons.east;
                        result.west = lons.west;
                    }
                }
            } else {
                result.lat = lat;
                result.lon = lon;
            }
            return result;
        },


        zoneNumberLons: function(zone) {
            var east = -180.0 + (6 * zone);
            var west = east - 6;

            return {
                east: east,
                west: west
            }
        },

        zoneLetterLats: function(letter) {
            switch (letter) {
                case 'C':
                    return {south: -80.0, north: -72.0};
                case 'D':
                    return {south: -72.0, north: -64.0};
                case 'E':
                    return {south: -64.0, north: -56.0};
                case 'F':
                    return {south: -56.0, north: -48.0};
                case 'G':
                    return {south: -48.0, north: -40.0};
                case 'H':
                    return {south: -40.0, north: -32.0};
                case 'J':
                    return {south: -32.0, north: -24.0};
                case 'K':
                    return {south: -24.0, north: -16.0};
                case 'L':
                    return {south: -16.0, north: -8.0};
                case 'M':
                    return {south: -8.0, north: -0.01};
                case 'N':
                    return {south: 0.01, north: 8.0};
                case 'P':
                    return {south: 8.0, north: 16.0};
                case 'Q':
                    return {south: 16.0, north: 24.0};
                case 'R':
                    return {south: 24.0, north: 32.0};
                case 'S':
                    return {south: 32.0, north: 40.0};
                case 'T':
                    return {south: 40.0, north: 48.0};
                case 'U':
                    return {south: 48.0, north: 56.0};
                case 'V':
                    return {south: 56.0, north: 64.0};
                case 'W':
                    return {south: 64.0, north: 72.0};
                case 'X':
                    return {south: 72.0, north: 84.0};
            }
        },

        /********************** USNG to UTM **************************************

         The Follwing functions are used to convert USNG Cords to UTM Cords.

         ***************************************************************************/

        /***********************************************************************************

         USNGtoUTM(zone,let,sq1,sq2,east,north,ret)
         Expected Input args:
         zone: Zone (integer), eg. 18
         let: Zone letter, eg S
         sq1:  1st USNG square letter, eg U
         sq2:  2nd USNG square Letter, eg J
         east:  Easting digit string, eg 4000
         north:  Northing digit string eg 4000
         ret:  saves zone,let,Easting and Northing as properties ret

         ***********************************************************************************/

        USNGtoUTM: function(zone,letter,sq1,sq2,east,north,ret)  {

            // easting goes from 100,000 - 800,000 and repeats across zones
            // A,J,S correspond with 100,000, B,K,T correspond with 200,000 etc
            var eastingArray = ["","AJS","BKT","CLU","DMV","ENW","FPX","GQY","HRZ"];

            // zoneBase - southern edge of N-S zones of millions of meters
            var zoneBase = [1.1,2.0,2.8,3.7,4.6,5.5,6.4,7.3,8.2,9.1,   0, 0.8, 1.7, 2.6, 3.5, 4.4, 5.3, 6.2, 7.0, 7.9];
            
            // multiply zone bases by 1 million to get the proper length for each
            for (var i = 0; i < zoneBase.length; i++) {
                zoneBase[i] = zoneBase[i] * 1000000;
            }

            // northing goes from 0 - 1,900,000. A corresponds with 0, B corresponds with 200,000, V corresponds with 1,900,000
            var northingArrayOdd = "ABCDEFGHJKLMNPQRSTUV";

            // even numbered zones have the northing letters offset from the odd northing. So, F corresponds with 0, G corresponds
            // with 100,000 and E corresponds with 1,900,000
            var northingArrayEven = "FGHJKLMNPQRSTUVABCDE";

            var easting = -1;

            for (var i = 0; i < eastingArray.length; i++) {
                
                // loop through eastingArray until sq1 is found
                // the index of the string the letter is in will be the base easting, as explained in the declaration
                // of eastingArray 
                if ( eastingArray[i].indexOf(sq1) != -1) {

                    // multiply by 100,000 to get the proper base easting
                    easting = i*100000;

                    // add the east parameter to get the total easting
                    if (east) {
                        easting = easting + Number(east)*Math.pow(10,5-east.length);
                    }
                    break;
                }
            }

            var northing = 0;
            
            if (sq2) {
            // if zone number is even, use northingArrayEven, if odd, use northingArrayOdd
            // similar to finding easting, the index of sq2 corresponds with the base easting
            if (zone%2 == 0) {
                northing = northingArrayEven.indexOf(sq2)*100000;
                } else if (zone %2 == 1) {
                northing = northingArrayOdd.indexOf(sq2)*100000;
                }

            // we can exploit the repeating behavior of northing to find what the total northing should be
            // iterate through the horizontal zone bands until our northing is greater than the zoneBase of our zone
            
                while (northing < zoneBase["CDEFGHJKLMNPQRSTUVWX".indexOf(letter)]) {
                    northing = northing + 2000000;
                    }

                if (north) {

                    // add the north parameter to get the total northing
                    northing = northing+Number(north)*Math.pow(10,5-north.length);
                }
            }
            else {
                // add approximately half of the height of one large region to ensure we're in the right zone
                northing = zoneBase["CDEFGHJKLMNPQRSTUVWX".indexOf(letter)]+499600;
            }

            // set return object
            ret.N = parseInt(northing);
            ret.E = parseInt(easting);
            ret.zone = zone;
            ret.letter = letter;
            

        },

        get100kSetForZone: function(zoneNumber) {
            var setParm = zoneNumber % this.num100kSets;
            if (setParm == 0)
                setParm = this.num100kSets;

            return setParm;
        },

        getNorthingFromChar: function (letter, setVal) {
            if (letter === '' || typeof letter === 'undefined') {
                return 0;
            }

            if (letter > 'V') {
                throw("MGRSPoint given invalid Northing "
                + letter);
            }

            // rowOrigin is the letter at the origin of the set for the
            // column
            var curRow = this.originRowLetters.charCodeAt(setVal - 1);
            var northingValue = 0.0;
            var rewindMarker = false;

            while (curRow !== letter.charCodeAt(0)) {
                curRow++;
                if (curRow === 'I'.charCodeAt(0))
                    curRow++;
                if (curRow === 'O'.charCodeAt(0))
                    curRow++;
                // fixing a bug making whole application hang in this loop
                // when 'n' is a wrong character
                if (curRow > 'V'.charCodeAt(0)) {
                    if (rewindMarker) { // making sure that this loop ends
                        throw("Bad character: " + String.fromCharCode(curRow));
                    }
                    curRow = 'A'.charCodeAt(0);
                    rewindMarker = true;
                }
                northingValue += 0.1;
            }

            return northingValue;
        },


        // parse a USNG string and feed results to USNGtoUTM, then the results of that to UTMtoLL

        USNGtoLL: function (usngStr_input,getCenter) {

            var usngp = {};

            this.parseUSNG_str(usngStr_input,usngp);
            var coords = {};

            // convert USNG coords to UTM; this routine counts digits and sets precision
            this.USNGtoUTM(usngp.zone,usngp.let,usngp.sq1,usngp.sq2,usngp.east,usngp.north,coords);

            // southern hemisphere case
            if (usngp.let < 'N') {
                coords.N -= this.NORTHING_OFFSET;
            }

            var accuracy;

            if (!getCenter) {
                accuracy = 100000.0 / Math.pow(10, usngp.precision);
            }

            var result = this.UTMtoLL(coords.N, coords.E, usngp.zone, accuracy);
            return result;
        },


        // convert lower-case characters to upper case, remove space delimeters, separate string into parts
        parseUSNG_str: function (usngStr_input, parts)
        {
            var j = 0;
            var usngStr;
            var usngStr_temp;

            if (!usngStr_input) {
                return 0;
            }

            usngStr_temp = usngStr_input.toUpperCase();

            // put usgn string in 'standard' form with no space delimiters
            var regexp = /%20/g;
            usngStr = usngStr_temp.replace(regexp,"");
            regexp = / /g;
            usngStr = usngStr.replace(regexp,"");

            if (usngStr.length < 2) {
                return 0;
            }

            // break usng string into its component pieces
            // if 2 digit zone
            if(!isNaN(parseFloat(usngStr.charAt(1))) && isFinite(usngStr.charAt(1))) {
                parts.zone = usngStr.charAt(j++)*10 + usngStr.charAt(j++)*1;
            } else { // else single digit zone
                parts.zone = usngStr.charAt(j++)*1;
            }
            parts.let = usngStr.charAt(j++);
            parts.sq1 = usngStr.charAt(j++);
            parts.sq2 = usngStr.charAt(j++);

            parts.precision = (usngStr.length-j) / 2;
            parts.east='';
            parts.north='';
            var k;
            for (k=0; k<parts.precision; k++) {
                parts.east += usngStr.charAt(j++)
            }

            if (usngStr[j] == " ") { j++ }
            for (k=0; k<parts.precision; k++) {
                parts.north += usngStr.charAt(j++)
            }
        },


        // checks a string to see if it is valid USNG;
        //    if so, returns the string in all upper case, no delimeters
        //    if not, returns 0
        isUSNG: function (inputStr) {
            var usngStr;
            var strregexp;

            // convert all letters to upper case
            usngStr = inputStr.toUpperCase();

            // get rid of space delimeters
            var regexp = /%20/g;
            usngStr = usngStr.replace(regexp,"");
            regexp = / /g;
            usngStr = usngStr.replace(regexp,"");

            if (usngStr.length > 15) {
                return 0;
            }

            strregexp = new RegExp("^[0-9]{2}[CDEFGHJKLMNPQRSTUVWX]$");
            if (usngStr.match(strregexp)) {
                return 0;
            }

            strregexp = new RegExp("^[0-9]{2}[CDEFGHJKLMNPQRSTUVWX][ABCDEFGHJKLMNPQRSTUVWXYZ][ABCDEFGHJKLMNPQRSTUV]([0-9][0-9]){0,5}$");
            if (!usngStr.match(strregexp)) {
                return 0;
            }

            if (usngStr.length < 7) {
                return 0;
            }

            // all tests passed...return the upper-case, non-delimited string
            return usngStr;

        },


        // create a Military Grid Reference System string.  this is the same as a USNG string, but
        //    with no spaces.  space delimiters are optional but allowed in USNG, but are not allowed
        //    in MGRS notation.  but the numbers are the same.
        LLtoMGRS: function (lat, lon, precision)
        {
            var mgrs_str;
            var usng_str = this.LLtoUSNG(lat, lon, precision);

            // remove space delimiters to conform to mgrs spec
            var regexp = / /g;
            mgrs_str = usng_str.replace(regexp,"");

            return mgrs_str;
        },


        // wrapper function specific to Google Maps, to make a converstion to lat/lng return a GLatLon instance.
        // takes a usng string, converts it to lat/lng using a call to USNGtoLL,
        // and returns an instance of GLatLng
        GUsngtoLL: function(str) {
            if (typeof GLatLng === 'function') {
                var latlng = [];
                this.USNGtoLL(str, latlng);
                return new GLatLng(latlng[0], latlng[1]);
            } else {
                throw new Error("GLatLng not defined.");
            }
        },


        LLtoUSNG_nad27: function(lat, lon, precision) {
            var usngstr;

            // set ellipsoid to Clarke 1866 (meters)
            this.EQUATORIAL_RADIUS = 6378206.4;
            this.ECC_SQUARED = 0.006768658;

            usngstr = this.LLtoUSNG(lat, lon, precision);

            // reset GRS80 ellipsoid
            this.EQUATORIAL_RADIUS    = 6378137.0;
            this.ECC_SQUARED = 0.006694380023;

            return usngstr + " (NAD27)";
        }

    });

    return usngs;

}));
