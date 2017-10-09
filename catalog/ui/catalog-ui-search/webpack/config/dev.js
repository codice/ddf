var webpack = require('webpack');
var merge = require('webpack-merge');
var base = require('./base');

module.exports = merge.smart(base, {
    entry: [
        'stack-source-map/register',
        'console-polyfill'
    ]
});