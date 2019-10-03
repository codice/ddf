There was a timing issue where the Ecache dependency brought in by a few classes from the `cxf-ws-security` and `wss4j` features were not available at the time the `security-sts-server` bundle required it, causing NoClassDefFoundError exceptions to occur.
This issue caused `security-sts-server` to fail when restarting DDF while the (defunct) broker-app was installed. For more information on this issue see: DDF-3364.

The issue has been fixed by directly copying those classes from `cxf-ws-security` and `wss4j` that depend on Ecache into `security-sts-server`, allowing Ecache to become immediately available.

The following classes have been copied directly into `security-sts-server`:
DefaultInMemoryTokenStore.java
EHCacheManagerHolder.java
EHCacheTokenStore.java
EHCacheUtils.java