# Metacard Access Control
---

This readme describes the workflow of access control and what conditions must be satisfied to be granted/denied access. Before 
continuing, it will be beneficial to clarify some definitions.

The set of security attributes or security taxonomy will be referred to as the ACL (access control list). The access control list will be an all-encompassing set of security attributes containing the following:

```$xslt
• security.access-individuals
• security.access-groups
• security.access-administrators
```

### What Can Each Security Grouping Do?
    
1. **Am I an access administrator?**</br>
    a. Yes – Then I can explicitly grant permission capabilities to other users</br>
    b. No – I cannot grant permission capabilities to other users

    
2. **Am I an access individual?**</br>
    a. Yes – I can read/write to metacards, but I cannot grant permissions</br>
    b. No – I cannot read/write to metacards unless I am in the access group, nor
can I grant permissions.

3. **Am I an access group member?**</br>
a. Yes – I can read/write to metacards, but I cannot grant permissions</br>
b. No – I cannot read/write to metacards unless I am in the access-individuals
list, nor can I grant permissions.


### Security Plugin Execution Overview

The trivial flow is a metacard gets passed through the system that has no ACL present. In this case, we pass through an empty policy map and all permissions are implied due to the lacking presence of any extraneous data left on the security map.

The non trivial flow involves execution through the *access control access plugin*, *access control policy plugin* and *access control policy extension plugin*. These plugins are defined below:

**Access Control Access Plugin**

- Simply does a diff on the ACL to ask 
	- Has the ACL changed?
		- Yes – Ensure the subject identity is within the ACL access admin or fail</br>		- No – No-Op
		
**Access Control Policy Plugin**

- We need to populate the transient policy map to ensure we can later imply permissions. This population will ultimately aide in determining whether or not something is accessible.

- First, we want to ensure that the metacard has an ACL. As mentioned above, the lack of ACL is trivial as we pass through an empty policy map. Contrary, the non-trivial case is the presence of an ACL.
	- Is ACL present?
		- Yes - Attach ACL attributes in security policy map to later imply
		- No – Do nothing, PDP will need to imply the empty set.

**Access Control Policy Extension Plugin**

While the policy plugin has the sole responsibility of populating the policy map to curry through the system, the extension has the job of attempting to imply permissions and remove them from the map. This permission implication process is what ultimately allows something to be accessible or non-accessible based on various conditionals. 

The conditionals that ultimately decide if access control permissions can be implied are referenced in the above section. 

Simple example:

If the requesting subjectIdentity exists within the access individuals list of the current metacard, access control permissions can be implied and removed from the policy map. 