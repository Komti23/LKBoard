#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import sys
import urllib.request
import paramiko

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

SERVER_IP = "164.37.224.219"
SERVER_USER = "root"
SERVER_PASS = "G90OlL5Tgnvw9vkogLAa1!"
REMOTE_WEB_DIR = "/var/www/html"
LOCAL_DIR = os.path.dirname(os.path.abspath(__file__))

def main():
    print("=" * 60)
    print("       Deploying Animated Heart Sub-Page")
    print("=" * 60)
    print(f"Target: {SERVER_USER}@{SERVER_IP}")
    
    # 1. Connect SSH
    print("[1/3] Connecting to server via SSH/SFTP...")
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(SERVER_IP, username=SERVER_USER, password=SERVER_PASS, timeout=10)
    sftp = ssh.open_sftp()
    print("      [OK] Connected successfully.")

    # 2. Upload heart.html and heart/index.html
    print("[2/3] Uploading heart pages...")
    local_heart = os.path.join(LOCAL_DIR, "web", "heart.html")
    remote_heart = f"{REMOTE_WEB_DIR}/heart.html"
    sftp.put(local_heart, remote_heart)
    print("      [OK] Uploaded: /var/www/html/heart.html")

    try:
        sftp.mkdir(f"{REMOTE_WEB_DIR}/heart")
    except Exception:
        pass

    remote_heart_idx = f"{REMOTE_WEB_DIR}/heart/index.html"
    sftp.put(local_heart, remote_heart_idx)
    print("      [OK] Uploaded: /var/www/html/heart/index.html")

    sftp.close()
    ssh.close()

    # 3. Verify Public Endpoints
    print("[3/3] Verifying HTTP endpoints...")
    test_urls = [
        f"http://{SERVER_IP}/heart",
        f"http://{SERVER_IP}/heart.html",
        f"http://{SERVER_IP}/heart/"
    ]
    for url in test_urls:
        try:
            req = urllib.request.urlopen(url, timeout=5)
            print(f"      [OK] {url} -> Status {req.status} OK")
        except Exception as e:
            print(f"      [!] Check warning for {url}: {e}")

    print("=" * 60)
    print(f"  [SUCCESS] Heart page live at: http://{SERVER_IP}/heart")
    print("=" * 60)

if __name__ == "__main__":
    main()
