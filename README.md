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
network. sensd functions as gateway daemon and relay sensors data report to 
it's listeners. sensd uses TCP a temprary port of 1234.

Copyright
---------
Open-Sourrce via GPL

Developing
----------
To develop and build app ATP package is need. The authors are using 
adt-bundle-linux-x86-20130917/

Also update PATH so script and other utilities will work.

Typically in .profile but follow the adt installation instructions:

PATH=$HOME/adt-bundle-linux-x86-20130917/sdk/tools:$PATH
PATH=$HOME/adt-bundle-linux-x86-20130917/sdk/platform-tools:$PATH

Then generate local.properties file so that ant finds the android-sdk:
  android update project -p .

run script has basic commands to to facilitate development, command line based.

Check it out!!

Typical use:
After connecting your mobile on USB with USB debugging enabled. 

./run

References
----------
https://github.com/herjulf/sensd

Enjoy!

