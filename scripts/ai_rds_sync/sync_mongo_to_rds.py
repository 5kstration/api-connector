import argparse
import json
import logging
import os
import sys
from datetime import datetime, timezone
from urllib.parse import quote_plus

from pymongo import MongoClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from entity import Base
from service import SyncService

logging.basicConfig(
    level=os.getenv("AI_RDS_SYNC_LOG_LEVEL", "INFO"),
    format="%(asctime)s %(levelname)s %(message)s",
)
logger = logging.getLogger(__name__)

DEFAULT_MONGO_DATABASE = "api_connector"
DEFAULT_MONGO_COLLECTION = "raw_externals"
SYNC_CATEGORIES = ("POLICY", "CARD", "INSURANCE")


def parse_args():
    parser = argparse.ArgumentParser(description="Sync API-Connector MongoDB raw data to AI-Service RDS tables.")
    parser.add_argument("--category", choices=["ALL", *SYNC_CATEGORIES], default=os.getenv("AI_RDS_SYNC_CATEGORY", "ALL"))
    parser.add_argument("--limit", type=int, default=int(os.getenv("AI_RDS_SYNC_LIMIT", "1000")))
    parser.add_argument("--create-tables", action="store_true", default=os.getenv("AI_RDS_SYNC_CREATE_TABLES", "false").lower() == "true")
    return parser.parse_args()


def mongo_database(client: MongoClient):
    configured_name = os.getenv("AI_RDS_SYNC_MONGO_DATABASE")
    if configured_name:
        return client[configured_name]

    default_database = client.get_default_database(default=None)
    if default_database is not None:
        return default_database

    return client[DEFAULT_MONGO_DATABASE]


def normalize_document(document: dict) -> dict:
    document["id"] = str(document.pop("_id"))
    return document


def load_items(collection, category: str, limit: int) -> list[dict]:
    query = {"status": "SUCCESS", "category": category}
    cursor = collection.find(query).sort("fetchedAt", -1).limit(limit)
    return [normalize_document(document) for document in cursor]


def rds_database_url() -> str:
    required_keys = ("DB_HOST", "DB_PORT", "DB_NAME", "DB_USER", "DB_PASSWORD")
    missing_keys = [key for key in required_keys if not os.getenv(key)]
    if missing_keys:
        raise RuntimeError(f"RDS connection environment variables are missing: {', '.join(missing_keys)}")

    host = os.getenv("DB_HOST")
    port = os.getenv("DB_PORT")
    name = os.getenv("DB_NAME")
    user = os.getenv("DB_USER")
    password = os.getenv("DB_PASSWORD")

    encoded_user = quote_plus(user)
    encoded_password = quote_plus(password)
    return f"postgresql+psycopg2://{encoded_user}:{encoded_password}@{host}:{port}/{name}"


def main() -> int:
    args = parse_args()
    mongo_uri = os.getenv("MONGODB_URI") or os.getenv("SPRING_DATA_MONGODB_URI")
    rds_url = rds_database_url()

    if not mongo_uri:
        raise RuntimeError("MONGODB_URI or SPRING_DATA_MONGODB_URI is required.")
    if args.limit < 1:
        raise RuntimeError("AI_RDS_SYNC_LIMIT must be greater than 0.")

    mongo_client = MongoClient(mongo_uri)
    mongo_db = mongo_database(mongo_client)
    collection_name = os.getenv("AI_RDS_SYNC_MONGO_COLLECTION", DEFAULT_MONGO_COLLECTION)
    collection = mongo_db[collection_name]

    engine = create_engine(rds_url, pool_pre_ping=True)
    if args.create_tables:
        Base.metadata.create_all(engine)

    session_factory = sessionmaker(bind=engine)
    categories = SYNC_CATEGORIES if args.category == "ALL" else (args.category,)
    result = {
        "startedAt": datetime.now(timezone.utc).isoformat(),
        "category": args.category,
        "mongoDatabase": mongo_db.name,
        "mongoCollection": collection_name,
        "limit": args.limit,
        "results": {},
    }

    with session_factory() as session:
        sync_service = SyncService(session)
        for category in categories:
            items = load_items(collection, category, args.limit)
            logger.info("Loaded raw items. category=%s count=%s", category, len(items))
            result["results"][category] = sync_service.sync_products(items, category)

    result["endedAt"] = datetime.now(timezone.utc).isoformat()
    print(json.dumps(result, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        logger.exception("AI RDS sync failed. error=%s", exc)
        print(json.dumps({"success": False, "error": str(exc)}, ensure_ascii=False), file=sys.stderr)
        sys.exit(1)
