# CWS - Cryptographic Web Store
CWS, Cryptographic Web Store, is designed to be used as a backend component in
other systems. Facilitating protected storing, sharing or collaborating on data
between entrusted parties. It is written purely in Java EE, allowing it to be
deployed in a wide range of different environments, depending on the need.

The CWS uses a combination of public/private keys for both Users & Groups, to
provide the control over data, and data itself is stored encrypted together with
enough information to allow other systems to process it, as the CWS is not having
any knowledge about it, other than the keys used to protect it.

Communication is achieved via a public API, which allows both REST and SOAP
based WebService requests. This will give a high degree of flexibility for
anyone to integrate it into their system.

# Who is this for
It is not designed for anyone particular, but as it provides a rather general
API and scales depending on the deployment - it can be used by anyone who finds
it useful. 

# Release Plan
Currently, the CWS is undergoing initial design and implementation is scheduled
to begin shortly, with the intention to have version 1.0 ready during 2017.

# Software License
The CWS is released under Apache License 2 or APL2.

# Contact
Kim Jensen <kim at javadog.io>
