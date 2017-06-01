module.exports = function (source, map) {
    this.cacheable();
    this.callback(null, [
        'var expect = require(\'chai\').expect;',
        'describe(__filename, function () {',
        source,
        '});'
    ].join(''), map);
};
