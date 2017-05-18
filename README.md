Flying Andrdoid Bot (FAB)
=========

This app is used to autonomously control a quadrotor platform using an Android smartphone via a serial MAVLink <-> WiFi bridge communication between the Pixhawk flight controller and the Android device.

The objective of this experiment is to demonstrate how standard hardware platforms like smartphones and flight controllers can be integrated through simple software architecture to build autonomous quadrotors that can navigate indoor environments. This opens up the possibility for any consumer to take a commercially available platforms like the Google Tango Project device and automate the process of generating three-dimensional (3-D) maps for exploring and mapping indoor environments. The quadrotor platform was chosen due to its mechanical simplicity and ease of control. Moreover, its ability to operate in confined spaces, hover in space, and perch or land on a flat surfaces; makes it a very attractive aerial platform with tremendous potential.

The experimental MAV platform is made from carbon fiber COTS components and is equipped with 4 brushless motors, and an autopilot board consisting of an IMU and a user–programmable microcontroller. The only other addition to this setup is a forward–pointing smartphone Nexus 5X, an optical flow, and a stand-alone ESP8266 based serial wireless bridge module to deal with the communication between the smartphone and the flight controller. The total mass of the platform is 1150g (including battery & smartphone).

+ Nexus 5X
+ Pixhawk
+ PX4Flow
+ Android SDK-25
+ PX4 Fligth Stack
+ DroneKit
+ MAVBridge

<a href="https://www.youtube.com/watch?v=TwOflR7KXuQ" target="_blank"><img src="https://img.youtube.com/vi/CSt2krIDdPI/0.jpg" 
alt="IMAGE ALT TEXT HERE" width="240" height="180" border="10" /></a>

##### Acknowledgements:
###### None of the work presented in this project would have become a reality without the support from Lorenz Meier, creator of Pixhawk and MAVLink, the PX4 community (i.e. Daniel Agar), and Bill Bonney from the DroneKit project.

##### References:
###### G. Loianno, Y. Mulgaonkar†, C. Brunner, D. Ahuja, A. Ramanandan, M. Chari, S. Diaz, and V. Kumar, “Smartphones Power Flying Robots”, 2015 IEEE/RSJ International Conference on Intelligent Robots and Systems (IROS), Hamburg, Germany
