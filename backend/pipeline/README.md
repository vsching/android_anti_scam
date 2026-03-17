# Safe Anot? Data Pipeline

Fetches, normalises, and publishes scam domain data to Cloudflare R2/KV/D1.

## Prerequisites

- Python 3.12+

## Setup

```bash
pip install -r requirements.txt
cp .env.example .env   # fill in Cloudflare credentials
```

## Usage

```bash
python seed_database.py
```

## Tests

```bash
pytest
```
