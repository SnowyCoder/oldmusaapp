# OldMusa Android App
## A bit of backstory
This is an old project that I developed with CNR (Italy's National Research Institute).
Initially CNR contacted our high-school class to develop the initial project that you can still find [here](https://github.com/OldMusa-5H/OldMusaServer).
After we finished that project and we graduated they contacted me to continue working but offering payment, and I accepted.
During covid (after 5 months of work) they ghosted me and their promise for some retribution has been sweeped under the rug, they haven't responded since.
So just take it as a lesson, contracts are good and legally binding, use them folks.
Does this have documentation? No, and it never will, I don't want to waste even more time on this.

## About OldMusa
This is an app to monitor sensors in museums using CNR database.
The android frontend can be used to have useful data visualizations, navigate sensor maps and research
historic data. You can also setup ranges for each sensor and when the value goes off range (ex. a
temperature gets too high, or a place too moist) you will be alerted with a notification.

## About Project
The server is the counterpart of [OldMusa's server](https://gitlab.com/oldmusa/oldmusaserver), and a fork of [An old app](https://github.com/OldMusa-5H/OldMusaApp).
This manages sites (museums), sensors (channel container with a location), channels (single measurement channel) and measurements.
It has lots of features:
- You can view all of the sensor locations in a nice map that you can upload.
- You can create quick channel measurement plots.
- You can create complex plot merging datas from different channels (ex. want to see temperature/humidity together?)
- You can manage other users

## Code
This is one of my first android projects, I worked upon the previous app's source code and I've changed almost every line.
Still, code is old and I'd probably write it differently today, I've changed, kotlin changed and android probably transmuted a hundred times.

