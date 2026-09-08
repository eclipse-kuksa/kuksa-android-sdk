# Troubleshooting Guide

This guide covers common issues encountered when developing with the KUKSA Android SDK and how to resolve them.

---

## Cannot Connect to Local KUKSA Databroker from Android Device

### Problem
Your KUKSA Databroker is running locally on your development machine (PC / Mac / Linux), but your Android app cannot connect to it (e.g., connection timed out, connection refused, or `UNAVAILABLE: io exception`).

### Why this happens
By default, `localhost` or `127.0.0.1` inside an Android application refers to the **Android device itself**, not your host PC where the Databroker is running.

---

### Solution 1: Physical Android Device via USB (Recommended)

When testing on a real physical Android device connected via a USB cable, use ADB reverse port forwarding. This redirects network requests from a port on the Android device to the corresponding port on your development PC.

#### Step 1: Enable USB Debugging
1. On your Android device, go to **Settings** > **About phone**.
2. Tap **Build number** 7 times to enable **Developer options**.
3. Go to **Settings** > **System** > **Developer options** and enable **USB debugging**.

#### Step 2: Verify Device Connection
Connect your device to your PC via USB and verify that ADB detects it:
```bash
adb devices
```
You should see your device listed with the `device` status (e.g., `<device_id>    device`). If it says `unauthorized`, accept the USB debugging authorization prompt on your phone screen.

#### Step 3: Set Up Reverse Port Forwarding
Run the following command in your terminal:
```bash
adb reverse tcp:55555 tcp:55555
```
> **Note:** Replace `55555` with the port your Databroker is listening on if different (e.g., `55556` for the sample testapp on Linux/Windows).
>
> **Important (macOS):** On macOS, the Databroker **cannot run on port `55556`**. Always use port `55555` (or another available port) on Mac. If you are using the sample test app (which defaults to `55556`), update the port in the app's connection settings to `55555`.

#### Step 4: Connect from the App
In your Android app, configure the Databroker host and port as:
- **Host:** `localhost` (or `127.0.0.1`)
- **Port:** `55555` (or your configured port)

#### Useful ADB Reverse Commands
- **List active port forwards:**
  ```bash
  adb reverse --list
  ```
- **Remove a specific forward:**
  ```bash
  adb reverse --remove tcp:55555
  ```
- **Remove all reverse forwards:**
  ```bash
  adb reverse --remove-all
  ```

---

### Solution 2: Android Emulator (Host Loopback Alias)

If you are using the official Android Studio Emulator, ADB reverse is not strictly required. The emulator runs behind a virtual router that provides special network aliases.

#### Step 1: Use `10.0.2.2` as the Host IP
The Android Emulator uses `10.0.2.2` to access the host machine's loopback interface (`127.0.0.1`).

In your Android app configuration:
- **Host:** `10.0.2.2`
- **Port:** `55555` (or your configured Databroker port)

*(Optional)* You can also use `adb reverse tcp:55555 tcp:55555` with an emulator, in which case `localhost:55555` will work just like on a physical device.

---

### Solution 3: Physical Device over Local Wi-Fi (No USB)

If you cannot connect your device via USB and want to connect over your local Wi-Fi network:

1. **Bind Databroker to all interfaces:** Ensure the Databroker is bound to `0.0.0.0` (all network interfaces) and not only `127.0.0.1`.
2. **Find your PC's IP address on the local network:**
   - **macOS / Linux:** Run `ifconfig` or `ip a` (e.g., `192.168.1.50`).
   - **Windows:** Run `ipconfig` in Command Prompt / PowerShell.
3. **Configure Firewall:** Ensure your PC's firewall allows incoming TCP traffic on the Databroker port (e.g., `55555`).
4. **Connect device to same Wi-Fi:** Ensure your Android device is connected to the same Wi-Fi network / subnet.
5. **Set Host in App:** Enter your PC's local IP address (e.g., `192.168.1.50`) and Databroker port in the Android app.

---

## Connection Checklist

If you are still unable to connect after following the steps above, check the following:

- [ ] **Is the Databroker actually running?** Check your terminal or Docker container to verify the Databroker process is active.
- [ ] **Are the ports matching?** Ensure the port specified in your app matches the Databroker port (default Databroker is `55555`; testapp sample is `55556`).
- [ ] **macOS Port 55556 limitation:** On macOS, the Databroker cannot run on port `55556`. If running on a Mac, use port `55555` and ensure the app is also set to port `55555`.
- [ ] **Insecure vs. Secure (TLS):** If your Databroker requires TLS certificates, connecting via `connectInsecure()` / plaintext will fail. Ensure TLS settings match your Databroker configuration.
- [ ] **Authentication (JWT):** If the Databroker has authentication enabled, ensure you pass a valid JSON Web Token.
