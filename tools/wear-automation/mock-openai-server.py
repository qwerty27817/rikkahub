#!/usr/bin/env python3
"""Minimal OpenAI-compatible mock server for acceptance tests.

Supports POST /v1/chat/completions (streaming SSE and non-streaming) and GET /v1/models.
Used with `adb reverse tcp:8099 tcp:8099` so an Android device can reach it at
http://127.0.0.1:8099/v1 without any external network or API key.
"""

import json
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

REPLY_TEXT = (
    "Hello from the mock model. This reply is streamed token by token over the "
    "local adb reverse tunnel, so the watch really renders a streaming answer."
)


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *args):
        pass

    def do_GET(self):
        data = json.dumps(
            {"object": "list", "data": [{"id": "mock-gpt", "object": "model", "owned_by": "mock"}]}
        ).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_POST(self):
        length = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(length) if length else b"{}"
        try:
            request = json.loads(raw or b"{}")
        except Exception:
            request = {}
        if request.get("stream"):
            self._stream()
        else:
            self._complete()

    def _chunk(self, payload):
        self.wfile.write(("data: " + json.dumps(payload, ensure_ascii=False) + "\n\n").encode())
        self.wfile.flush()

    def _stream(self):
        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream")
        self.send_header("Cache-Control", "no-cache")
        self.send_header("Connection", "close")
        self.end_headers()
        base = {
            "id": "mock-1",
            "object": "chat.completion.chunk",
            "created": int(time.time()),
            "model": "mock-gpt",
        }
        self._chunk({**base, "choices": [{"index": 0, "delta": {"role": "assistant"}, "finish_reason": None}]})
        for word in REPLY_TEXT.split():
            self._chunk({**base, "choices": [{"index": 0, "delta": {"content": word + " "}, "finish_reason": None}]})
            time.sleep(0.08)
        usage = {"prompt_tokens": 12, "completion_tokens": len(REPLY_TEXT.split()), "total_tokens": 40}
        self._chunk({**base, "choices": [{"index": 0, "delta": {}, "finish_reason": "stop"}], "usage": usage})
        self.wfile.write(b"data: [DONE]\n\n")
        self.wfile.flush()

    def _complete(self):
        data = json.dumps(
            {
                "id": "mock-1",
                "object": "chat.completion",
                "created": int(time.time()),
                "model": "mock-gpt",
                "choices": [
                    {"index": 0, "message": {"role": "assistant", "content": REPLY_TEXT}, "finish_reason": "stop"}
                ],
                "usage": {"prompt_tokens": 12, "completion_tokens": 20, "total_tokens": 32},
            }
        ).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", 8099), Handler).serve_forever()
