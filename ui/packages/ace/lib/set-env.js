const spawn = require('cross-spawn')

module.exports = ({ args: [cmd, ...args], pkg }) => {
  spawn(cmd, args, {
    stdio: 'inherit',
    env: Object.assign({}, process.env, { ACE_BUILD: Date.now() }),
  }).on('exit', process.exit)
}
