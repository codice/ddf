The certificates located in this folder are all self-signed and were creating specifically for this unit test. 

The following certificates are available:
ca.crt - this is the main root certificate authority that signs all other certificates and is the creator of the CRL.
ia.crt - this is a valid intermediate certificate authority that has been signed by the CA.
ia-bad.crt - this is an invalid intermediate certificate authority that has been marked for revocation by the CRL.

Creating these certificates and CRL was done by following the steps on the blog http://blog.didierstevens.com/2013/05/08/howto-make-your-own-cert-and-revocation-list-with-openssl/

In short they were:

1) Creating the CA:
	a) openssl genrsa -out ca.key 4096
	b) openssl req -new -x509 -days 3655 -key ca.key -out ca.crt
	c) create ca files
		i) touch certindex
		ii) echo 01 > certserial
		iii) echo 01 > crlnumber
		iv) make sure ca.conf is in current folder

2) Creating Valid IA:
	a) openssl genrsa -out ia.key 4096
	b) openssl req -new -key ia.key -out ia.csr
	c) openssl ca -batch -config ca.conf -notext -in ia.csr -out ia.crt

3) Creating Invalid IA:
	a) openssl genrsa -out ia-bad.key 4096
	b) openssl req -new -key ia-bad.key -out ia-bad.csr
	c) openssl ca -batch -config ca.conf -notext -in ia-bad.csr -out ia-bad.crt
	
4) Revoking Invalid IA:
	a) openssl ca -config ca.conf -revoke ia.crt -keyfile ca.key -cert ca.crt
	
5) Generating CRL:
	a) openssl ca -config ca.conf -gencrl -keyfile ca.key -cert ca.crt -out root.crl.pem
	b) openssl crl -inform PEM -in root.crl.pem -outform DER -out root.crl
	
After performing those steps, the CRL (root.crl) was created with the ia-bad certificate being revoked.

Viewing certificates can be done by doing:
openssl x509 -in <certificate file> -text -noout

Viewing CRLs can be done by doing:
openssl crl -inform DER -text -in <crl file>
