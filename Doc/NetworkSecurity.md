# Creating a self-signed certificate for the point server

Obviously we want to use https to connect to the point server but for that your server needs a certificate.
If it is running on an internal server, you probably want to use a self-signed certificate, but browser and
Android apps will not accept one, because they do not trust self-signed certificates in general.

So what we need to do is not only create a self-signed certificate for the point server, but also
import the certificate we use to sign it into the app as a trusted source.

That is all a bit of a hassle but the steps below document how to do this (on Linux). It is based on information
collected from https://developer.android.com/training/articles/security-config and the accepted answer on
https://stackoverflow.com/questions/7580508/getting-chrome-to-accept-self-signed-localhost-certificate

## creating the certificates

Generate your root certificate key pair. The `myCA.pem` is what you will later incorporate in your Android app security.
Note that we do all the work in the directory `Certficates` to keep things tidy. It would be a good idea to limit access to this directory to root only.

```bash
cd Certificates
openssl genrsa -des3 -out myCA.key 2048
openssl req -x509 -new -nodes -key myCA.key -sha256 -days 825 -out myCA.pem
```

Create a certificate request for mydomain.com. The result is a file called `mydomain.com.csr`

```bash
NAME=mydomain.com
openssl genrsa -out $NAME.key 2048
openssl req -new -key $NAME.key -out $NAME.csr
```

Create a file `mydomain.com.ext` with the requirements for the certificate. The important bit here is to
include all domain names, server names and ip-addresses that the server will be listening on.

Note that inside an Android emulator, 127.0.0.1 is the address of the emulated phone itself,
and you will need to configure the app to connect to 10.0.2.2 to connect to localhost on the machine
where where run Android Studio! Later, when you move the server to the actual RaspberryPi that controls your servos,
your app will need to connect to the Pi. In this example we assume we have a hostname `pointserver.mydomain.com` for
the Pi and that its ip-address is 192.168.0.163.

```bash
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
subjectAltName = @alt_names
[alt_names]
DNS.1 = mydomain.com # primary domain
DNS.2 = pointserver.mydomain.com # subdomain
IP.1 = 192.168.0.163 # all ip-addresses, the first one is the actual ip-address of my raspberry
IP.2 = 127.0.0.1 # the second one is localhost on the server where I test the stuff
IP.3 = 10.0.2.2 # the third one is the address inside the Android emulator that is mapped to 127.0.0.1 on the host machine
```

Then generate (and self-sign) the actual certificate 

```bash
openssl x509 -req -in $NAME.csr -CA myCA.pem -CAkey myCA.key -CAcreateserial -out $NAME.crt -days 825 -sha256 -extfile $NAME.ext
```

This will result in two file `mydomain.com.key` and `mydomain.com.crt` that you need when you start up the server like

```bash
sudo PYTHONPATH=./src python -m point -m --cert ../Certificates/mydomain.com.crt --key ../Certificates/mydomain.com.key
```

## Configuring your app

First make sure the application element inside the `Androidmanifest.xml` file points to a security configuration:

```xml
    <application
        ...
        android:networkSecurityConfig="@xml/network_security_config"
        ... >
```

The create the file 'res/xml/network_security_config.xml' and make sure the include a <domain> element for each ip-adress (or hostname+domain) you want to connect to:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config>
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">127.0.0.1</domain>
        <domain includeSubdomains="true">192.168.0.163</domain>
        <trust-anchors>
            <certificates src="@raw/my_ca"/>
        </trust-anchors>
    </domain-config>
</network-security-config>
```

And finally, create a file `res/raw/my_ca` and copy the contents of the file `myCA.pem` created earlier (I am not showing the whole contents here):

```
-----BEGIN CERTIFICATE-----
MIIDfTCCAmWgAwIBAgIUVbdcNZOD ...

...

... vm+su4GxMbRezhORJqXklBL+ZaTZ
-----END CERTIFICATE-----
```

Now if you're using an Android emulator, you can configure the Point app (in Settings) to use `https://10.0.2.2:8080` and it will able to connect to the point server using SSL.
