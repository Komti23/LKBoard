#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
LKBoard Release & Update Deployment Script.
Uploads landing page web assets, Windows .exe installer, and version manifest to 164.37.224.219.
"""

import os
import sys
import json
import time
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

VERSION = "1.0.0"
VERSION_CODE = 1
RELEASE_DATE = "2026-10-23"

CHANGELOG = [
    "Официальный релиз LKBoard v1.0.0",
    "Интерактивная математическая лаборатория Desmos",
    "Стикеры со встроенными To-Do чекбоксами и авто-масштабированием",
    "Канбан-доски с магнитной сменой статусов вех",
    "Стеклянный Pomodoro и воркшоп-таймер со звуковым гонгом",
    "Живая синхронизация через веб-браузер и Desktop в реальном времени",
    "Полноценный экспорт в векторный PDF, SVG и 4K PNG",
    "Встроенная система неблокирующих автообновлений"
]

NGINX_CONF = """server {
    listen 80 default_server;
    listen [::]:80 default_server;

    root /var/www/html;
    index index.html;
    server_name _;

    # Landing Page & Static Web Assets
    location / {
        try_files $uri $uri/ =404;
    }

    # Downloads for Windows .exe Installers
    location /downloads/ {
        alias /var/www/html/downloads/;
        sendfile on;
        tcp_nopush on;
        autoindex on;
    }

    # Auto-Update Manifest API for LKBoard Desktop App
    location = /api/version {
        default_type application/json;
        alias /var/www/html/api/version;
        add_header Access-Control-Allow-Origin *;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    # Web Canvas Proxy to Local Relay Server
    location /canvas {
        proxy_pass http://127.0.0.1:8765/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 86400s;
    }

    # Direct Web App fallback
    location = /app {
        alias /var/www/html/canvas.html;
    }

    # Real-Time Collaboration Events & Actions
    location ~ ^/api/(events|snapshot|sync|action|status) {
        proxy_pass http://127.0.0.1:8765;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 86400s;
    }
}
"""

def upload_dir_recursive(sftp, local_dir, remote_dir):
    try:
        sftp.mkdir(remote_dir)
    except Exception:
        pass

    for item in os.listdir(local_dir):
        local_path = os.path.join(local_dir, item)
        remote_path = f"{remote_dir}/{item}"
        if os.path.isdir(local_path):
            upload_dir_recursive(sftp, local_path, remote_path)
        else:
            print(f"  -> Uploading: {item}")
            sftp.put(local_path, remote_path)

def main():
    print("=" * 60)
    print("      LKBoard Deploy & Auto-Update Publisher")
    print("=" * 60)
    print(f"Target: {SERVER_USER}@{SERVER_IP}")
    print(f"Version: {VERSION} (Code: {VERSION_CODE})")
    print()

    # 1. Connect SSH
    print("[1/6] Connecting to server via SSH...")
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(SERVER_IP, username=SERVER_USER, password=SERVER_PASS, timeout=10)
    sftp = ssh.open_sftp()
    print("      ✓ Connected successfully.")

    # 2. Upload Web Assets & Web Client
    print("[2/6] Uploading website landing page assets and Full Web Studio...")
    local_web_dir = os.path.join(LOCAL_DIR, "web")
    upload_dir_recursive(sftp, local_web_dir, REMOTE_WEB_DIR)
    
    # Also upload client.html directly to relay server
    local_client_html = os.path.join(LOCAL_DIR, "client.html")
    if os.path.exists(local_client_html):
        sftp.put(local_client_html, "/opt/lkboard-relay/client.html")
        ssh.exec_command("systemctl restart lkboard-relay")
        print("      ✓ Full Web Client deployed to /opt/lkboard-relay/ and relay restarted.")
    print("      ✓ Web assets uploaded.")

    # 3. Create downloads directory & upload .exe
    print("[3/6] Uploading Windows installer (.exe)...")
    remote_downloads_dir = f"{REMOTE_WEB_DIR}/downloads"
    try:
        sftp.mkdir(remote_downloads_dir)
    except Exception:
        pass

    local_exe_path = os.path.join(LOCAL_DIR, "build", "compose", "binaries", "main", "exe", f"Lkboard-{VERSION}.exe")
    if os.path.exists(local_exe_path):
        remote_exe_name = f"Lkboard-{VERSION}.exe"
        remote_exe_path = f"{remote_downloads_dir}/{remote_exe_name}"
        file_size_mb = os.path.getsize(local_exe_path) / (1024 * 1024)
        print(f"      Uploading {local_exe_path} ({file_size_mb:.1f} MB)...")
        sftp.put(local_exe_path, remote_exe_path)
        print("      ✓ Installer uploaded.")

        # Create latest symlink or copy
        ssh.exec_command(f"ln -sf {remote_exe_path} {remote_downloads_dir}/Lkboard-latest.exe")
        ssh.exec_command(f"ln -sf {remote_exe_path} {remote_downloads_dir}/Lkboard-Setup.exe")
        print("      ✓ Latest download symlinks created.")
    else:
        print(f"      [WARNING] Installer not found at {local_exe_path}!")

    # 4. Generate & Upload Auto-Update Manifest
    print("[4/6] Generating and publishing update manifest (/api/version)...")
    manifest = {
        "version": VERSION,
        "versionCode": VERSION_CODE,
        "releaseDate": RELEASE_DATE,
        "title": f"Официальный релиз LKBoard v{VERSION}",
        "changelog": CHANGELOG,
        "downloadUrl": f"http://{SERVER_IP}/downloads/Lkboard-{VERSION}.exe",
        "mandatory": False
    }

    try:
        sftp.mkdir(f"{REMOTE_WEB_DIR}/api")
    except Exception:
        pass

    with sftp.open(f"{REMOTE_WEB_DIR}/api/version", "w") as f:
        f.write(json.dumps(manifest, ensure_ascii=False, indent=2))
    print("      ✓ Manifest written to /var/www/html/api/version.")

    # 5. Configure Nginx
    print("[5/6] Updating Nginx configuration on port 80...")
    with sftp.open("/etc/nginx/sites-available/default", "w") as f:
        f.write(NGINX_CONF)
    
    stdin, stdout, stderr = ssh.exec_command("nginx -t")
    nginx_test = stderr.read().decode()
    if "successful" in nginx_test:
        ssh.exec_command("systemctl reload nginx")
        print("      ✓ Nginx syntax ok and reloaded successfully.")
    else:
        print(f"      [ERROR] Nginx test failed:\n{nginx_test}")

    sftp.close()
    ssh.close()

    # 6. Verify Public Endpoints
    print("[6/6] Verifying public HTTP endpoints...")
    time.sleep(1)

    # Check Website
    try:
        req = urllib.request.urlopen(f"http://{SERVER_IP}/", timeout=5)
        print(f"      ✓ Website HTTP status: {req.status} OK")
    except Exception as e:
        print(f"      [!] Website check warning: {e}")

    # Check Update Manifest
    try:
        req = urllib.request.urlopen(f"http://{SERVER_IP}/api/version", timeout=5)
        data = json.loads(req.read().decode())
        print(f"      ✓ Version API status: {req.status} OK (Remote version: {data.get('version')})")
    except Exception as e:
        print(f"      [!] Version API warning: {e}")

    print()
    print("=" * 60)
    print("  [SUCCESS] LKBoard Website & Auto-Update Engine Live!")
    print(f"  • Website URL:     http://{SERVER_IP}/")
    print(f"  • Download URL:    http://{SERVER_IP}/downloads/Lkboard-{VERSION}.exe")
    print(f"  • Update API:      http://{SERVER_IP}/api/version")
    print(f"  • Web Canvas:      http://{SERVER_IP}/canvas")
    print("=" * 60)

if __name__ == "__main__":
    main()
