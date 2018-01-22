# react-mount

A functional stateless component that wraps the react mount/unmount
life-cycle hooks.

# usage

```javascript
import Mount from 'react-mount'

const MyComponent = ({ onMount, offMount }) => (
  <Mount on={onMount} off={offMount}>
    {/* my component */}
  </Mount>
)

```
