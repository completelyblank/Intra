from scapy.all import ARP, Ether, srp

def scan_network(target_ip="192.168.18.7"):
    devices = []
    arp = ARP(pdst=target_ip)
    ether = Ether(dst="ff:ff:ff:ff:ff:ff")
    packet = ether / arp
    result = srp(packet, timeout=3, verbose=0)[0]
    for sent, received in result:
        devices.append({"ip": received.psrc, "mac": received.hwsrc})

    for dev in devices:
        print(dev)
    return devices
