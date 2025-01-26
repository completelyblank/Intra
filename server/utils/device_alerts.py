def detect_new_devices(scanned_devices, known_devices):
    known_macs = {device["mac"] for device in known_devices}
    new_devices = [device for device in scanned_devices if device["mac"] not in known_macs]
    return new_devices
