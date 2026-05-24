# Namma Vastra Android

This workspace now contains a Kotlin Android app built with Jetpack Compose.

## Build

Install or configure the Android SDK, then run:

```powershell
.\gradlew.bat assembleDebug
```

If Android Studio is installed but Gradle is using the wrong JDK, use the bundled JBR:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat assembleDebug
```

## Current App

- Branded splash screen
- Phone and Google sign-in prototype flow
- Weaver or buyer role selection
- Weaver onboarding with profile steps
- Dashboard with trend palettes and quick actions
- Loom gallery
- Saree upload draft form
- Fair price calculator
- Weaver story and settings screens

Firebase Auth, Firestore, Storage, camera, and gallery integrations are left as the next implementation layer because this machine does not currently have an Android SDK configured for a full build.
