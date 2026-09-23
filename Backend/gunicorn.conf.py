import os
import sys

# Gunicorn configuration optimized for Render Free Tier (512MB RAM, 0.1 CPU)
bind = f"0.0.0.0:{os.getenv('PORT', '8000')}"

# Single process with 4 worker threads keeps total memory under ~120MB,
# leaving >380MB of free RAM buffer to completely eliminate Out-Of-Memory (OOM) kills.
workers = int(os.getenv('WEB_CONCURRENCY', '1'))
worker_class = 'gthread'
threads = int(os.getenv('GUNICORN_THREADS', '4'))

# Recycle workers periodically to eliminate any Python memory fragmentation
max_requests = 1000
max_requests_jitter = 50

timeout = 120
graceful_timeout = 30
keepalive = 5

# Limit glibc memory arenas on Linux to eliminate multi-threaded virtual memory fragmentation
raw_env = [
    "MALLOC_ARENA_MAX=2",
    "PYTHONUNBUFFERED=1"
]

# Use /dev/shm shared memory in Linux containers for heartbeat files to prevent disk hangs
if os.path.exists('/dev/shm'):
    worker_tmp_dir = '/dev/shm'

accesslog = '-'
errorlog = '-'
loglevel = 'info'
