# SMP Flutter Client

Simple Flutter frontend for SMP doctor-patient conversation APIs, targeting iOS, Android, and Web.

## Features

- Doctor login and patient login
- Create or open 1-to-1 conversation thread
- View messages
- Send text messages
- Upload attachment files
- Doctor can save attachment into patient profile

## Prerequisites

1. Install Flutter SDK and make sure `flutter` is on PATH.
2. Start backend on port 8080.

## Generate platform folders

This folder contains app source files. Generate Android/iOS/Web wrappers once:

```bash
cd flutter_client
flutter create . --platforms=android,ios,web
```

## Run

```bash
cd flutter_client
flutter pub get
flutter run -d chrome
```

For Android or iOS, choose device:

```bash
flutter devices
flutter run -d <device-id>
```

## Backend URL

Default backend URL in app is:

http://localhost:8080

Change it in the UI if needed.

## Notes

- For web, your backend must allow CORS for the frontend origin.
- For Android emulator, use http://10.0.2.2:8080 instead of localhost.
