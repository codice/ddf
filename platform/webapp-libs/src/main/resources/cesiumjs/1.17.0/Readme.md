# Cesium Bower Package #

## What is Cesium Bower ##
There is no officially supported Bower package for Cesium.  This project is a direct build of Cesium with no
code modifications of from that of the official library.  What we've done is build a library that moves the necessary
files and excludes all of the rest so that Cesium can be easily used as a dependency in an application.

## Using Cesium Bower ##

The best way to install cesium bower is via the command line

    cd <your projects root dir>
    bower install cesiumjs --save

This will automatically download the Cesium library into your project.

During your initial development you are going to want to use the UnminifiedCesium version of Cesium.  It contains more
robust debugging messages and have stricter validation for data.

TODO:  Give examples to add to GruntFile and GulpFiles to support dev/prod builds that auto toggle which js and css
files to include.

