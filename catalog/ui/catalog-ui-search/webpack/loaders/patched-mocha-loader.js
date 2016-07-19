// workaround to support mocha-phantomjs 4.x
// https://github.com/webpack/mocha-loader/pull/27
var loader = require("mocha-loader");

module.exports = function() {};
module.exports.pitch = function(req) {
    var originalSource = loader.pitch.call(this, req);

    if (this.target != "web") {
        return originalSource;
    }

    var source = originalSource.split("\n");
    source.splice(1, 0, "if(typeof window !== 'undefined' && window.initMochaPhantomJS) { window.initMochaPhantomJS(); }");
    return source.join("\n");
};