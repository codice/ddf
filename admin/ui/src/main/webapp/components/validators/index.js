// validators - validate user input
// If a validator return undefined, the supplied value is valid
// otherwise they should return an error message.

import isInt from 'validator/lib/isInt'

export const port = (string) => {
  if (!isInt(string, { min: 0, max: 65535 })) {
    return 'not a valid port'
  }
}

import isFQDN from 'validator/lib/isFQDN'

export const hostname = (string) => {
  if (!isFQDN(string)) {
    return 'value is not a fully qualified domain name'
  }
}
