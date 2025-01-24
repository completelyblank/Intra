![WhatsApp Image 2025-01-24 at 18 53 10_ceaacc70](https://github.com/user-attachments/assets/35bbcd21-3510-44ca-8a9c-f46e29a71772)

# Intra

A network monitoring app that provides real-time scanning and tagging of devices on your network. Designed with an emphasis on security and user interactivity, **Intra Scan** helps you keep track of connected devices, receive alerts for unknown devices, and customize your network management experience.

## Suggested Color Scheme

### Primary Colors
- **Teal (#008080)**: For headings, buttons, and highlights (tech-savvy feel).
- **Light Blue (#ADD8E6)**: For background gradients or accents.

### Secondary Colors
- **Dark Gray (#2E2E2E)**: For the background (dark mode aesthetic).
- **White (#FFFFFF)**: For text and icons.

### Accent Colors
- **Yellow (#FFD700)**: For alerts or notifications (new/unknown devices).
- **Green (#32CD32)**: To show trusted/known devices.

### Text Colors
- **Off-White (#F0F0F0)**: For general text.
- **Light Gray (#C0C0C0)**: For secondary details.

## Development Roadmap

### Month 1: Foundation & Setup
#### Research & Design
- Understand ARP, ping sweeps, and tools like `arp-scan` or `scapy`.
- Sketch wireframes for:
  - Home screen
  - Alerts screen
  - Device tagging popup
- Decide on backend technology (Flask or Node.js).
- Set up the development environment.

#### Backend Development
- Implement basic network scanning functionality using:
  - Python's `scapy` or Node.js with `arp-scan`.
- Create APIs for:
  - Scanning the network.
  - Saving tagged devices locally.
  - Sending alerts for new devices.
- Test scanning on a local network.

#### Frontend Development
- Set up a React Native project.
- Create navigation using `react-navigation`.
- Build a basic UI with `react-native-paper` (lists and buttons).

### Month 2: Core Features
#### Backend Enhancements
- Store tagged devices in a local database (SQLite or JSON file).
- Add logic to detect new/unknown devices compared to the saved list.
- Implement an endpoint to notify the frontend of alerts.

#### Frontend Features
- **Home Screen**:
  - Display a list of scanned devices with IP, MAC, and hostname.
  - Add options to tag devices (e.g., "My Phone").
- **Alerts Screen**:
  - Display recent alerts (e.g., "Unknown device joined the network").
- Add basic styling and apply the chosen color scheme.

#### Integration & Testing
- Connect the React Native app to the backend using fetch requests.
- Test scanning and alerting functionality end-to-end.
- Ensure smooth data flow between the backend and frontend.

### Month 3: Polishing & Enhancements
#### UI/UX Improvements
- Add animations (e.g., device list slide-in, alert popup fade-in).
- Create polished color gradients for the background.
- Use icons for tagging and trusted/unknown devices.

#### Advanced Features
- Save scan logs locally using React Native’s AsyncStorage.
- Implement a "Block Device" feature using router APIs (if supported).
- Enable customizable alerts (e.g., threshold for unrecognized devices).

#### Testing & Deployment
- Test the app across different devices and networks.
- Perform user testing to gather feedback on usability.
- Deploy the backend to a cloud service (e.g., AWS, Heroku).

## System Design

### Backend Design
#### API Endpoints
- **`/scan`**:
  - **Input**: Trigger a scan request.
  - **Output**: List of connected devices with IP, MAC, and hostname.
- **`/tag-device`**:
  - **Input**: Device details and tag (e.g., “My Laptop”).
  - **Output**: Success message.
- **`/alerts`**:
  - **Input**: None.
  - **Output**: List of new/unknown device alerts.

#### Backend Flow
1. Perform a network scan.
2. Compare results with a stored list of tagged devices.
3. Send the scan results or alerts to the frontend.

### Frontend Design
#### Screens
- **Home Screen**:
  - List of scanned devices.
  - Button to trigger a new scan.
  - Option to tag devices.
- **Alerts Screen**:
  - List of unrecognized devices and timestamps.
  - Options to tag or ignore.

#### Frontend Flow
1. App requests a scan from the backend.
2. Displays the results on the home screen.
3. Alerts for new devices are shown on the alerts screen.

### Database Design
#### Tables
- **Tagged Devices**:
  - `device_id` (Primary Key).
  - `ip_address`.
  - `mac_address`.
  - `hostname`.
  - `tag_name`.
- **Alerts**:
  - `alert_id` (Primary Key).
  - `timestamp`.
  - `device_id`.

## Tech Stack
- **Frontend**: React Native
- **Backend**: Flask or Node.js
- **Database**: SQLite (or JSON for simplicity)
- **Tools**: `arp-scan`, `scapy`

## Core Features (High Priority, Must-Have)
1. **Device Type Identification**:
   - Use heuristics or lightweight ML models to guess device types (e.g., OUI lookup from MAC addresses).
2. **Device History**:
   - Store a log of previously connected devices with timestamps.
3. **Customizable Alerts**:
   - Allow users to define alert rules (e.g., notify for unknown devices).
4. **Customizable Dashboard**:
   - Let users personalize their view with widgets for device lists, history, or charts.
5. **Multi-Network Support**:
   - Save and switch between profiles for multiple networks.

## Enhanced Features (Medium Priority, Interactive)
1. **Network Traffic Monitoring**:
   - Track data usage per device with real-time updates or graphs.
2. **Network Security Scanning**:
   - Provide lightweight security scans using tools like Nmap.
3. **Machine Learning-Based Anomaly Detection**:
   - Detect unusual behaviors like connection times or unauthorized devices.

## Advanced Features (Low Priority, Optional)
1. **Geolocation-Based Alerts**:
   - Use APIs to notify users of suspicious connection origins.
2. **Integration with Smart Home Devices**:
   - Voice-based alerts or actions via Alexa/Google Home.

## Recommended Development Plan
1. **Version 1**:
   - Build core features with device identification, alerts, and a customizable dashboard.
2. **Version 2**:
   - Add medium-priority features like traffic monitoring and security scanning.
3. **Version 3**:
   - Enhance with advanced features like geolocation and smart home integration.

## Interactive Features for Version 1
- Device tagging with live updates.
- Real-time progress for network scans.
- Alerts for new/suspicious devices.
- Simple graphs for network activity.
- Manual security scans with progress indicators.

This approach balances functionality, interactivity, and scalability, creating an engaging user experience with room for future growth.

