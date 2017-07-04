var fs = require('fs');
var path = require('path');

function concatLess(relativePath, fileName) {
    var filePath = path.join(relativePath, fileName + '.less');
    var file = fs.readFileSync(filePath, { encoding: 'utf-8' });
    return file.replace(/@import\s*("|')([^"']+)("|')/g, function (match, _, importPath) {
        if (importPath.match(/\.css$/)) return '';
        var dirname = path.dirname(importPath);
        var basename = path.basename(importPath).replace(/\.less$/, '');
        return concatLess(path.join(relativePath, dirname), basename);
    });
}

module.exports = function (source, map) {
    this.cacheable();
    this.addContextDependency(path.resolve(__dirname, '../../', './src/main/'));
    var less = concatLess(path.resolve(__dirname, '../../', './src/main/webapp/styles/'), 'styles');
    this.callback(null, less, map);
};
