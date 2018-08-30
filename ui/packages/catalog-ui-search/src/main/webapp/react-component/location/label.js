const React = require('react')

const Label = ({ children }) => (
  <span className="input-group-addon">
    {children}
    &nbsp;
  </span>
)

module.exports = Label
