module.exports = function (source, map) {
  this.cacheable()
  this.callback(null, [
    'describe(__filename, function () {',
    source,
    '});'
  ].join(''), map)
}
