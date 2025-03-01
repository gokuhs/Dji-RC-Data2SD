# DJI RC Data2SD

Expands Unlocked DJI RC (rm330) internal storage using a microSD card. 

## Description

DJI RC Data2SD is an Android application designed to expand the internal storage of the Unlocked DJI RC (rm330) remote controller by utilizing an external microSD card. This application allows users to transfer data to the microSD card, effectively freeing up valuable internal storage space.

I only test this with my rm330 unlocked with the method of DJI RC FCC: https://www.djircfcc.com/

## Key Features

* **Extended Storage:** Seamlessly expand the DJI RC's storage capacity.
* **Safe Testing Environment:** The system partition remains untouched, providing a secure environment for software testing.
* **Automatic Restoration:** Upon device restart, the original system configuration is restored.
* **Ext4 File System:** Utilizes the Ext4 file system for the data partition, ensuring optimal performance.

## Important Notes

* This application has been tested exclusively on the DJI RC (rm330) model. Compatibility with other models is not guaranteed.
* Root access is required for this application to function.
* Users assume all responsibility for any consequences arising from the use of this software.
* This software is provided without warranty.

## Instructions

1.  Download the latest APK version from the [releases](https://github.com/gokuhs/Dji-RC-Data2SD//releases) section.
2.  Connect your DJI RC to a computer with ADB installed.
3.  Ensure your DJI RC has root access.
4.  Execute `adb install <package name>`, for example: `adb install djircdata2sd_v1.apk`.
5.  Insert a microSD card into the device.
6.  Run the DJI RC Data2SD application from the device.
7.  Follow the on-screen instructions.

## Known Issues

* Sometimes, if you attempt to mount the SD card after starting DJI Fly, the device may get stuck on the boot screen. Simply restart the device and run DJI RC Data2SD before starting DJI Fly.

## Disclaimer

Use this application at your own risk. The developer is not responsible for any damage or data loss that may occur.

## License

This application is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License (CC BY-NC-SA 4.0).

[![CC BY-NC-SA 4.0](https://licensebuttons.net/l/by-nc-sa/4.0/88x31.png)](https://creativecommons.org/licenses/by-nc-sa/4.0/)

For more details, please visit: [https://creativecommons.org/licenses/by-nc-sa/4.0/](https://creativecommons.org/licenses/by-nc-sa/4.0/)
