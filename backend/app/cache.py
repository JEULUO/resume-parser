import json
import sqlite3
from pathlib import Path
from typing import Any

from redis import Redis


class CacheStore:
    def __init__(self, db_path: str, redis_url: str | None = None):
        self.db_path = Path(db_path)
        self.redis: Redis | None = self._connect_redis(redis_url)
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self._init_db()

    def _connect_redis(self, redis_url: str | None) -> Redis | None:
        if not redis_url:
            return None
        try:
            client = Redis.from_url(redis_url, decode_responses=True)
            client.ping()
            return client
        except Exception:
            return None

    def _connect(self) -> sqlite3.Connection:
        return sqlite3.connect(self.db_path)

    def _init_db(self) -> None:
        with self._connect() as conn:
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS cache_entries (
                    cache_key TEXT PRIMARY KEY,
                    value TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
            )

    def get(self, cache_key: str) -> dict[str, Any] | None:
        if self.redis:
            value = self.redis.get(cache_key)
            if value:
                return json.loads(value)

        with self._connect() as conn:
            row = conn.execute("SELECT value FROM cache_entries WHERE cache_key = ?", (cache_key,)).fetchone()
        if not row:
            return None
        return json.loads(row[0])

    def set(self, cache_key: str, value: dict[str, Any]) -> None:
        payload = json.dumps(value, ensure_ascii=False)
        if self.redis:
            self.redis.set(cache_key, payload, ex=60 * 60 * 24 * 7)

        with self._connect() as conn:
            conn.execute(
                """
                INSERT INTO cache_entries(cache_key, value)
                VALUES(?, ?)
                ON CONFLICT(cache_key) DO UPDATE SET value = excluded.value
                """,
                (cache_key, payload),
            )
