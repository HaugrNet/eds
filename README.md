# CWS - Cryptographic Web Store
CWS, Cryptographic Web Store, is designed to be used as a backend component in
other systems. Facilitating protected storing, sharing or collaboration of data
between entrusted parties. It is written purely in Java EE, allowing it to be
deployed in a wide range of different environments, depending on the need.

The CWS focuses on Circles of Trust, where Members of a Circle will have varying
level of access to the Data belonging to the Circle. This is achieved by a
combination of Symmetric and Asymmetric Encryption, guaranteeing that only
Members with Access can view Data. Storage is safe as all data is stored
encrypted.

Communication is achieved via a public API, which allows both REST and SOAP
based WebService requests. This will give a high degree of flexibility for
anyone to integrate it into their system.

# Who is this for
It is not designed for anyone particular, but as it provides a rather general
API and scales depending on the deployment - it can be used by anyone who finds
it useful. 

# Release Plan
The CWS has left the initial design, and is currently being implemented. The
first working alpha release is scheduled for Summer 2017 with the final version
1.0 release later in 2017.

# Software License
The CWS is released under Apache License 2 or APL2.

# Contact
Kim Jensen <kim at javadog.io>
