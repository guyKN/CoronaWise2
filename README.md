# CoronaWise
IMPORTANT NOTE: this app does ask you to provide your location when first launched, but that location is not at all used. It just has to be enable in order to be able to scan for bluetooth devices. 

ANOTHER IMPORTANT NOTE: After further testing, it seems that this app only works for certain types of android devices based on the way diffrent devices handle bluetooth. I can't gurantee that It'll work on all types of devices. 

## Goal

The goal of this app is to use bluetooth to contact trace without being Invasive to people's privacy. 


## How this project works

* When the app is opened for the first time, it creates a private key and a public key unique to the user. 

* The app continously scans for nearby bluetooth devices. 
  * When it finds a device, it checks the device's RSSI (Remote Signal Strengh Index). If the RSSI is greater than a certain threshold, then the other device is likely within 6 feet.
  * The app checks if the other device's mac address is on the list of macAddresses that have already been connected to. This is done so 2 devices won't continously connect and disconnect. 
  * If both the phones are close to each other, and the macAddress is not on the list the app starts a new thread that starts a connection. 
  * The app uses an insecure connection because an insecure connection doesn't require user interaction, and the public key is not secretive. 
  * Each device sends their public key to the other device using the created bluetooth socket, which is stored in a Room SQL Database, together with the other device's macAddress and the time of the encounter. 
  * Finally, the connection is closed.

* When a user is sick, the app generates a signature for the user using the user's Private key by using the RSA algorithm, and uploads it to the AWS server. 

* To check if a user has been exposed, the app downloads all signatures from the AWS server, and for each one, it compares with all the keys on the device. If a signature matches a key, then the user is has been exposed. Otherwise, the user has not been exposed. 

## Future improvement

* Test more devices: currently, it seems that the app only works with some devices, and has difficulty connecting. I intend to improve this. 
* Test Bluetooth RSSI: Currently the thresholds for the RSSI to be within 6 feet was tested on just two devices, so that means that if some devices have stronger or weaker bluetooth signals, the distance won't be mesured accuratly. 
* Battery life: Currently, the app continously scans for devices, which drains the battery very quickly. Optimization needs to be done in order to reduce battery usage and heat. 
