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
        var x = 0,
            y = 0,
            i,
            j,
            f,
            point1,
            point2;

        for (i = 0, j = this.length - 1; i < this.length; i += 1, j = i) {
            point1 = this.points[i];
            point2 = this.points[j];
            f = point1.longitude * point2.latitude - point2.longitude * point1.latitude;
            x += (point1.longitude + point2.longitude) * f;
            y += (point1.latitude + point2.latitude) * f;
        }

        f = this.area() * 6;

        return new Point(x / f, y / f);
    };

    return {Region: Region};

});