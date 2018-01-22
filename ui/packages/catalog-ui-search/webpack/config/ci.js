var path = require('path');
var glob = require('glob');
var merge = require('webpack-merge');
var HtmlWebpackPlugin = require('html-webpack-plugin');

var base = require('./base');

var config =  merge.smart(base, {
    devtool: 'source-map',
    node: {
        __filename: true
    },
    entry: glob.sync('src/main/webapp/**/*.spec.js*', {
            cwd: path.resolve(__dirname, '../../')
        }).map(function (spec) { return path.resolve(spec) }),
    output: {
        path: path.resolve(__dirname, '../../target/test/'),
        filename: 'test.js'
    },
    plugins: [
        new HtmlWebpackPlugin(),
    ],
    module: {
        loaders: [
            {
                test: /\.spec\./,
                loaders: [path.resolve(__dirname, '../loaders', 'patched-mocha-loader.js'),
                    path.resolve(__dirname, '../loaders', 'spec-loader.js')],
                exclude: /(node_modules|target)/
            }
        ]
    }
});

config.entry.shift(); // remove main.js

module.exports = config;
