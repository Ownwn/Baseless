import pathlib
import random
import socket
import subprocess
import tempfile
import time

# Ai generated file, a bit crap but should do the job
def available_port():
    for port in random.sample(range(20000, 31000), 100):
        with socket.socket() as sock:
            try:
                sock.bind(("127.0.0.1", port))
                return port
            except OSError:
                pass
    raise RuntimeError("No test port available")


port = available_port()
base = f"http://127.0.0.1:{port}"
checks = 0
with tempfile.TemporaryDirectory() as directory:
    work = pathlib.Path(directory)
    with (work / "server.log").open("w+") as log:
        server = subprocess.Popen(
            ["java", "--enable-native-access=ALL-UNNAMED", "-cp", "out:out/test",
             "com.ownwn.servertest.TestServer", str(port)], stdout=log, stderr=log)
        try:
            for _ in range(100):
                log.seek(0)
                if "Server started" in log.read():
                    break
                if server.poll() is not None:
                    raise AssertionError("Test server exited during startup")
                time.sleep(0.05)
            else:
                raise AssertionError("Test server did not start")

            def curl(path, status, *args, expected=None):
                global checks
                headers, body = work / "headers", work / "body"
                result = subprocess.run(
                    ["curl", "--silent", "--show-error", "--noproxy", "*", "--http1.1",
                     "--max-time", "10", "--dump-header", str(headers), "--output", str(body),
                     "--write-out", "%{http_code}", *args, base + path],
                    capture_output=True, check=True)
                assert result.stdout == str(status).encode(), (path, result.stdout, body.read_bytes())
                assert headers.read_bytes().count(b"HTTP/1.1") == 1, headers.read_text()
                assert b"Connection: close" in headers.read_bytes()
                if expected is not None:
                    assert body.read_bytes() == expected, (path, body.read_bytes())
                checks += 1
                return body.read_bytes()

            curl("/api/ok", 200, expected=b"hello")
            curl("/outside-api", 404)
            curl("/api/unknown", 404)
            curl("/api/private", 401, "-X", "POST")  # auth precedes multipart parsing
            curl("/api/upload", 405)
            curl("/api/rejected", 413)  # interceptor exception survives reflection
            assert b"private fixture failure" not in curl("/api/broken", 500)
            curl("/api/upload", 415, "-X", "POST", "-H", "Content-Length: 0")
            curl("/api/upload", 415, "-H", "Content-Type: application/json", "--data-binary", "{}")
            curl("/api/upload", 400, "-H", "Content-Type: multipart/form-data", "--data-binary", "bad")
            curl("/api/upload", 411, "-X", "POST", "-H", "Content-Type: multipart/form-data; boundary=test")
            curl("/api/upload", 400, "-X", "POST", "-H", "Content-Length: -1")
            curl("/api/upload", 413, "-X", "POST", "-H", "Content-Length: 999999999999999999")
            curl("/api/upload", 501, "-X", "POST", "-H", "Transfer-Encoding: chunked")
            curl("/api/upload", 400, "-H", "Content-Type: multipart/form-data; boundary=test",
                 "--data-binary", "--test\r\nContent-Disposition: form-data; name=x\r\n\r\ntruncated")
            curl("/api/upload", 400, "-H", "Content-Type: multipart/form-data; boundary=test",
                 "--data-binary", "--test\r\n\r\nvalue\r\n--test--\r\n")
            curl("/api/upload", 200, "-F", "text=hello", expected=b"hello")
            binary = bytes(range(256)) * 300 + b"\r\n--testX\r\n"
            (work / "payload.bin").write_bytes(binary)
            curl("/api/upload", 200, "-F", f"file=@{work / 'payload.bin'}", expected=binary)
            curl("/api/upload", 200, "-F", "text=one", "-F", "text=two", expected=b"onetwo")
            form = b"--test\r\nContent-Disposition: form-data; filename=\"x.bin\"; name=\"file\"\r\n\r\n--test\r\nfile bytes\r\n--test--\r\n"
            (work / "form").write_bytes(form)
            curl("/api/upload", 200, "-H", 'Content-Type: Multipart/Form-Data; charset=utf-8; boundary="test"',
                 "--data-binary", f"@{work / 'form'}", expected=b"--test\r\nfile bytes")
            curl("/api/upload", 400, "-H", 'Content-Type: multipart/form-data; boundary="test',
                 "--data-binary", "bad")
            curl("/api/ok", 200, expected=b"hello")  # server remains usable after failures
            log.flush()
            log.seek(0)
            messages = log.read()
            assert "Response already" not in messages, messages
            assert "Failed to write" not in messages, messages
            print(f"Passed {checks} curl HTTP checks")
        except Exception:
            log.seek(0)
            print(log.read())
            raise
        finally:
            server.terminate()
            try:
                server.wait(timeout=5)
            except subprocess.TimeoutExpired:
                server.kill()
                server.wait()
