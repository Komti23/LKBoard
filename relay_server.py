#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
LKBoard Real-time Cloud Collaboration Relay Server.
Provides real-time room-based synchronization via SSE, Delta Sync, and Web Canvas.
"""

import os
import sys
import json
import time
import threading
from urllib.parse import urlparse, parse_qs
from http.server import HTTPServer, BaseHTTPRequestHandler
from socketserver import ThreadingMixIn

PORT = 8765
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CLIENT_HTML_PATH = os.path.join(BASE_DIR, "client.html")

ROOMS = {}
GLOBAL_LOCK = threading.Lock()

def get_room(room_id):
    with GLOBAL_LOCK:
        if room_id not in ROOMS:
            ROOMS[room_id] = {
                'title': f'LKBoard {room_id}',
                'elements': {},
                'actions': [],
                'version': 0,
                'clients': set(),
                'lock': threading.Lock()
            }
        return ROOMS[room_id]

class ThreadingHTTPServer(ThreadingMixIn, HTTPServer):
    daemon_threads = True

class RelayHandler(BaseHTTPRequestHandler):

    def send_cors_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_cors_headers()
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path
        qs = parse_qs(parsed.query)
        room_id = qs.get('room', ['LK-MAIN'])[0]
        room = get_room(room_id)

        if path == "/" or path == "/index.html":
            self.serve_web_client(room_id)
        elif path == "/api/snapshot":
            self.serve_snapshot(room, room_id)
        elif path == "/api/status":
            self.serve_status()
        elif path == "/api/sync":
            since = int(qs.get('since', [0])[0])
            self.serve_sync(room, since)
        elif path == "/api/events":
            self.serve_sse(room, room_id)
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Not Found")

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path
        qs = parse_qs(parsed.query)
        room_id = qs.get('room', ['LK-MAIN'])[0]
        room = get_room(room_id)

        if path == "/api/action":
            content_length = int(self.headers.get('Content-Length', 0))
            body = self.rfile.read(content_length).decode('utf-8')
            try:
                action = json.loads(body)
                self.process_action(room, action)
                resp = json.dumps({"status": "ok"}).encode('utf-8')
                self.send_response(200)
                self.send_cors_headers()
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(resp)))
                self.end_headers()
                self.wfile.write(resp)
            except Exception as e:
                resp = json.dumps({"status": "error", "message": str(e)}).encode('utf-8')
                self.send_response(400)
                self.send_cors_headers()
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(resp)))
                self.end_headers()
                self.wfile.write(resp)
        else:
            self.send_response(404)
            self.end_headers()

    def process_action(self, room, action):
        with room['lock']:
            room['version'] += 1
            action_id = room['version']
            
            atype = action.get('type')
            if atype == 'snapshot':
                doc = action.get('document', {})
                room['elements'] = {}
                for el in doc.get('elements', []):
                    if isinstance(el, dict) and 'id' in el:
                        room['elements'][el['id']] = el
                if 'title' in doc and doc['title']:
                    room['title'] = doc['title']
            elif atype == 'clear_board':
                room['elements'] = {}
            elif atype == 'upsert_element':
                el = action.get('element')
                if el and 'id' in el:
                    room['elements'][el['id']] = el
            elif atype == 'delete_elements':
                for el_id in action.get('elementIds', []):
                    room['elements'].pop(el_id, None)
            elif atype == 'toggle_sticky_todo':
                note_id = action.get('noteId')
                idx = action.get('lineIndex', 0)
                el = room['elements'].get(note_id)
                if el and 'text' in el:
                    lines = el['text'].split('\n')
                    if 0 <= idx < len(lines):
                        if lines[idx].startswith('- [ ] '):
                            lines[idx] = '- [x] ' + lines[idx][6:]
                        elif lines[idx].startswith('- [x] ') or lines[idx].startswith('- [X] '):
                            lines[idx] = '- [ ] ' + lines[idx][6:]
                        el['text'] = '\n'.join(lines)

            room['actions'].append((action_id, action))
            if len(room['actions']) > 500:
                room['actions'].pop(0)

            sse_msg = f"data: {json.dumps(action)}\n\n".encode('utf-8')
            dead_clients = []
            for client_wfile in room['clients']:
                try:
                    client_wfile.write(sse_msg)
                    client_wfile.flush()
                except Exception:
                    dead_clients.append(client_wfile)
            for dc in dead_clients:
                room['clients'].discard(dc)

    def serve_snapshot(self, room, room_id):
        with room['lock']:
            elements_list = list(room['elements'].values())
            data = {
                "title": room.get('title', f"LKBoard {room_id}"),
                "elements": elements_list,
                "version": room['version']
            }
        resp = json.dumps(data).encode('utf-8')
        self.send_response(200)
        self.send_cors_headers()
        self.send_header("Content-Type", "application/json; charset=UTF-8")
        self.send_header("Content-Length", str(len(resp)))
        self.end_headers()
        self.wfile.write(resp)

    def serve_sync(self, room, since):
        with room['lock']:
            new_actions = [{"id": aid, "action": act} for aid, act in room['actions'] if aid > since]
            data = {
                "currentVersion": room['version'],
                "actions": new_actions
            }
        resp = json.dumps(data).encode('utf-8')
        self.send_response(200)
        self.send_cors_headers()
        self.send_header("Content-Type", "application/json; charset=UTF-8")
        self.send_header("Content-Length", str(len(resp)))
        self.end_headers()
        self.wfile.write(resp)

    def serve_sse(self, room, room_id):
        self.send_response(200)
        self.send_cors_headers()
        self.send_header("Content-Type", "text/event-stream; charset=UTF-8")
        self.send_header("Cache-Control", "no-cache, no-transform")
        self.send_header("Connection", "keep-alive")
        self.send_header("X-Accel-Buffering", "no")
        self.end_headers()

        with room['lock']:
            snapshot_action = {
                "type": "snapshot",
                "document": {
                    "title": f"LKBoard {room_id}",
                    "elements": list(room['elements'].values())
                },
                "senderId": "server",
                "senderName": "Cloud Relay"
            }
            initial_data = f"data: {json.dumps(snapshot_action)}\n\n".encode('utf-8')
            try:
                self.wfile.write(initial_data)
                self.wfile.flush()
                room['clients'].add(self.wfile)
            except Exception:
                return

        try:
            while True:
                time.sleep(15)
                self.wfile.write(b": ping\n\n")
                self.wfile.flush()
        except Exception:
            pass
        finally:
            with room['lock']:
                room['clients'].discard(self.wfile)

    def serve_status(self):
        with GLOBAL_LOCK:
            total_rooms = len(ROOMS)
            total_clients = sum(len(r['clients']) for r in ROOMS.values())
        resp = json.dumps({
            "status": "online",
            "service": "LKBoard Cloud Relay",
            "port": PORT,
            "roomsCount": total_rooms,
            "clientsCount": total_clients
        }).encode('utf-8')
        self.send_response(200)
        self.send_cors_headers()
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(resp)))
        self.end_headers()
        self.wfile.write(resp)

    def serve_web_client(self, room_id):
        try:
            with open(CLIENT_HTML_PATH, "r", encoding="utf-8") as f:
                html = f.read()
            html = html.replace("LK-ROOM-PLACEHOLDER", room_id)
        except Exception as e:
            html = f"<html><body>Error loading client.html: {e}</body></html>"
            
        encoded = html.encode('utf-8')
        self.send_response(200)
        self.send_cors_headers()
        self.send_header("Content-Type", "text/html; charset=UTF-8")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def log_message(self, format, *args):
        sys.stderr.write(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] {args[0]} {args[1]}\n")

def run():
    server_address = ('0.0.0.0', PORT)
    httpd = ThreadingHTTPServer(server_address, RelayHandler)
    print(f"[*] LKBoard Collaboration Relay Server running on port {PORT}")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        httpd.server_close()

if __name__ == '__main__':
    run()
