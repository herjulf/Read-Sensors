Read-Sensors - Wireless Sensor data in your Andriod
===================================================

Authors
--------					
Robert Olsson <robert@Radio-Sensors.COM>
Olof Hagsand <olof@hagsand.se>

Abstract
--------
This a project for reading remote and realtime sensor data. Typically this 
android app is used with the sensd package which acts as a gateway to WSN
network. send functions as gateway daemon and relay senors data report to 
it's listeners. sensd is used TCP and uses a temprary port of 1234.




All programs are written C, Java-script and bash. And designed for for small
footprint and minimal dependencies. sensd runs on Raspberry Pi and openwrt.

Copyright
---------
Open-Sourrce via GPL

To develop and build app ATP package is need. Also update PATH so script
and utilities will work.

Then generate local.properties file so that ant finds the android-sdk:
  android update project -p .

The author is using adt-bundle-linux-x86-20130917/

run script has basic commands to to facilitate development, command line based.
Check it out!

Typical use:

After connecting your mobile on USB with USB debugging enabled. 

./run


Enjoy!

