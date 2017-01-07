export default {
  'actions': [
    {
      'label': 'check',
      'method': 'POST',
      'url': '/network-settings'
    }
  ],
  'form': {
    'title': 'LDAP Network Settings',
    'questions': [
      {
        'id': 'hostName',
        'label': 'LDAP Host name',
        'type': 'HOSTNAME',
        'value': 'localhost'
      },
      {
        'defaults': [
          389,
          636
        ],
        'id': 'port',
        'label': 'LDAP Port',
        'type': 'PORT',
        'value': 636
      },
      {
        'defaults': [
          'No encryption',
          'Use LDAPS',
          'Use startTLS'
        ],
        'id': 'ldapEncryptionMethod',
        'label': 'Encryption method',
        'type': 'STRING_ENUM',
        'value': 'No encryption'
      }
    ]
  }
}
