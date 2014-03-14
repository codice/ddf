/*jslint sub: true, maxerr: 50, indent: 4, browser: true */
/*global define */
define(function () {
    // blatently stolen and modified from http://stackoverflow.com/questions/16282330/find-centerpoint-of-polygon-in-javascript

    "use strict";

    function Point(x, y) {
        this.longitude = x;
        this.latitude = y;
    }

    function Region(points) {
        this.points = points || [];
        this.length = points.length;
    }

    Region.prototype.area = function () {
        var area = 0,
            i,
            j,
            point1,
            point2;

        for (i = 0, j = this.length - 1; i < this.length; i += 1, j = i) {
            point1 = this.points[i];
            point2 = this.points[j];
            area += point1.longitude * point2.latitude;
            area -= point1.latitude * point2.longitude;
        }
        area /= 2;

        return area;
    };

    Region.prototype.centroid = function () {
        var X = 0.0,
            Y = 0.0,
            Z = 0.0,
            i,
            lat,
            lon,
            hyp,
            a,
            b,
            c;

        // Subtract 1 from the length to ignore the duplicate point
        for (i = 0; i < this.length-1; i += 1){
            lat = this.points[i].latitude * Math.PI / 180;
            lon = this.points[i].longitude * Math.PI / 180;

            a = Math.cos(lat) * Math.cos(lon);
            b = Math.cos(lat) * Math.sin(lon);
            c = Math.sin(lat);

            X += a;
            Y += b;
            Z += c;
        }

        X /= this.length;
        Y /= this.length;
        Z /= this.length;

        lon = Math.atan2(Y, X);
        hyp = Math.sqrt(X * X + Y * Y);
        lat = Math.atan2(Z, hyp);

        return new Point(lon * 180 / Math.PI, lat * 180 / Math.PI);
    };

    return {Region: Region};

});