from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Any

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse

APP_DIR = Path(__file__).resolve().parent
DEFAULT_CONFIG = APP_DIR.parent / "config" / "rules.json"
CONFIG_PATH = Path(os.getenv("MIVVI_RULES_FILE", str(DEFAULT_CONFIG)))
STATIC_INDEX = APP_DIR / "static" / "index.html"

app = FastAPI(
    title="MIVVI SECURE Rules API",
    version="1.0.0",
    description=(
        "API konfigurasi aturan Rule-Based Risk Scoring. API ini mendistribusikan "
        "keyword, bobot, threshold, domain whitelist, dan daftar shortlink; "
        "isi pesan pengguna tidak dikirim ke layanan ini."
    ),
)

# The Android app is a native client. CORS is enabled to support optional local admin tools.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["GET", "OPTIONS"],
    allow_headers=["*"],
)


def load_config() -> dict[str, Any]:
    try:
        with CONFIG_PATH.open("r", encoding="utf-8") as file:
            data = json.load(file)
    except FileNotFoundError as exc:
        raise HTTPException(status_code=500, detail="Rules configuration file was not found") from exc
    except json.JSONDecodeError as exc:
        raise HTTPException(status_code=500, detail="Rules configuration contains invalid JSON") from exc

    required = {"version", "weights", "thresholds", "keywords", "urgency_patterns", "sensitive_patterns", "official_domains", "shortlink_domains"}
    missing = required.difference(data)
    if missing:
        raise HTTPException(status_code=500, detail=f"Rules configuration missing keys: {sorted(missing)}")
    return data


@app.get("/", response_class=FileResponse, tags=["web"])
def root() -> FileResponse:
    """Serve the interactive web prototype simulator for MIVVI SECURE."""
    if not STATIC_INDEX.exists():
        raise HTTPException(status_code=404, detail="Simulator web page was not found")
    return FileResponse(STATIC_INDEX)


@app.get("/api/v1/health", tags=["system"])
def health() -> dict[str, str]:
    return {"status": "ok", "service": "mivvi-secure-rules-api", "version": "1.0.0"}


@app.get("/api/v1/rules", tags=["rules"])
def get_rules() -> dict[str, Any]:
    """Return the active scoring rules so Android can cache them locally."""
    return load_config()
