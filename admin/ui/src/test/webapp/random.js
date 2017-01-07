
export const action = (label, method, url) => ({
  label: label || 'check',
  method: method || 'POST',
  url: url || '/admin/beta/wizard/network'
})

export const childrenFn = (id, label, type, value) => ({
  id: id || 0,
  label: label || 'label',
  type: type || 'HOSTNAME',
  value: value || 'value'
})

export const stage = (actions, label, children) => ({
  actions: actions || [action()],
  form: {
    type: 'PANEL',
    label: label || 'title',
    children: children || [childrenFn()]
  }
})
