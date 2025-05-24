# Where's My Device

A secure Android application that helps you locate and control your lost or stolen device remotely via SMS commands.

## Features

- 📍 **Locate Device**: Get precise GPS coordinates of your device with Google Maps link
- 🔊 **Ring Device**: Make your device ring at maximum volume even when silenced
- 📱 **Device Info**: Get battery level, charging status, screen status, and more
- 📞 **Call Me**: Make your device call you back immediately
- 🔐 **Security Options**: Command password protection and phone number whitelist

## How It Works

Send SMS commands to your device from any phone to control it remotely. The app runs in the background and processes SMS messages that start with your customized command prefix and password.

### Example Commands:

```
WMD locate mypassword         → Returns device location
WMD ring mypassword           → Makes device ring at max volume
WMD info mypassword           → Shows battery, charging status, etc.
WMD callme mypassword         → Device calls you back
WMD help mypassword           → Shows all available commands
```

## Security Features

- **Command Password**: All commands require your secret password
- **Whitelist**: Optionally restrict which phone numbers can control your device
- **Command Prefix**: Customize the prefix that triggers commands

## Setup Instructions

1. Install the app on your Android device
2. Grant all required permissions (SMS, Location, Phone, etc.)
3. Configure your command prefix and password
4. Optionally set up your phone number whitelist

## Requirements

- Android 8.0 (API level 26) or higher
- SMS capabilities
- Location services

## Permissions Explained

- **SMS**: To receive and send command messages
- **Location**: To find your device's position
- **Phone**: To make callback calls
- **Display Over Other Apps**: To show alerts when device is found

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the GPL License - see the LICENSE file for details. 