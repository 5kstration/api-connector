import json
import logging
import re
import time

from sqlalchemy.exc import IntegrityError, SQLAlchemyError
from sqlalchemy.orm import Session

from entity import CardProduct, InsuranceProduct, PolicyProduct

logger = logging.getLogger(__name__)


def create_tsid() -> str:
    millis = int(time.time() * 1000)
    nanos = time.time_ns() % 1_000_000
    return f"{millis:013d}{nanos:06d}"[:26]


def _get(raw: dict, *candidates, default=""):
    for key in candidates:
        val = raw.get(key)
        if val is not None and str(val).strip():
            return val
    return default


def _parse_date(raw: dict, *candidates) -> str:
    val = str(_get(raw, *candidates))
    val = re.sub(r"[-/]", "", val)
    if re.fullmatch(r"\d{8}", val):
        return f"{val[:4]}.{val[4:6]}.{val[6:]}"
    return val


def _parse_url(raw: dict, *candidates) -> str:
    for key in candidates:
        val = raw.get(key, "")
        if val and re.match(r"https?://", str(val)):
            return val
    return ""


def _parse_int(raw: dict, *candidates):
    val = _get(raw, *candidates, default="")
    if val == "":
        return None
    digits = re.sub(r"[^0-9]", "", str(val))
    return int(digits) if digits else None


def _json_text(value) -> str:
    return json.dumps(value, ensure_ascii=False)


def _map_card(source_code: str, raw: dict) -> dict:
    benefit_texts = raw.get("benefitTexts") or raw.get("benefits") or []
    if isinstance(benefit_texts, list) and benefit_texts and isinstance(benefit_texts[0], str):
        benefits = _json_text([
            {"label": f"혜택 {index + 1}", "value": benefit}
            for index, benefit in enumerate(benefit_texts[:8])
        ])
    elif isinstance(benefit_texts, list):
        benefits = _json_text(benefit_texts[:8])
    else:
        benefits = "[]"

    return dict(
        company=_get(raw, "cardCompany", "company", "issuer", "cmpyNm", "brandNm"),
        card_name=_get(raw, "cardName", "name", "productName", "prdNm", "goodsNm"),
        top_benefit=_get(raw, "summary", "topBenefit", "description", "mog", "brief"),
        benefits=benefits,
        apply_url=_parse_url(raw, "detailUrl", "applyUrl", "productUrl", "hpgeUrl", "url"),
        accent_color="#3182F6",
    )


def _map_insurance(source_code: str, raw: dict) -> dict:
    if source_code == "SAFE_INSURANCE":
        guarantee_items = raw.get("lcgvrGrnt", [])
        benefits = _json_text([
            {"label": item.get("grntItmNm", ""), "value": item.get("grntCnts", "")}
            for item in guarantee_items[:5]
        ])
        return dict(
            insurer=_get(raw, "orgNm", "upOrgNm", "ctrtrNm"),
            insurance_name=_get(raw, "insrncGdsNm", "prdNm", "productName"),
            top_benefit=_get(raw, "insrdNm", "mog", "summary"),
            benefits=benefits,
            apply_url=_parse_url(raw, "hpgeUrl", "applyUrl", "url"),
            accent_color="#8B5CF6",
        )

    if source_code == "INDEMNITY_INSURANCE":
        benefits = _json_text([
            {"label": "보험 유형", "value": _get(raw, "ptrn", "insrncType", "category")},
            {"label": "보장 구분", "value": _get(raw, "mog", "coverageType", "coverage")},
            {"label": "기준일", "value": _get(raw, "basDt", "baseDate", "stdDt")},
        ])
        return dict(
            insurer=_get(raw, "cmpyNm", "companyName", "issuer", "orgNm"),
            insurance_name=_get(raw, "prdNm", "productName", "insrncGdsNm", "goodsNm"),
            top_benefit=_get(raw, "mog", "summary", "topBenefit", "insrdNm"),
            benefits=benefits,
            apply_url=_parse_url(raw, "hpgeUrl", "applyUrl", "url"),
            accent_color="#8B5CF6",
        )

    guarantee_items = raw.get("lcgvrGrnt") or raw.get("coverages") or raw.get("grntItems") or []
    if guarantee_items:
        benefits = _json_text([
            {
                "label": item.get("grntItmNm") or item.get("title", ""),
                "value": item.get("grntCnts") or item.get("description", ""),
            }
            for item in guarantee_items[:5]
        ])
    else:
        benefits = _json_text([
            {"label": "보험 유형", "value": _get(raw, "ptrn", "insrncType", "productType", "gdsClsNm", "GDS_CLSF_NM")},
            {"label": "보장 구분", "value": _get(raw, "mog", "coverageType", "coverage", "insrncPrd", "GIVE_BNFI_NM")},
            {"label": "기준일", "value": _get(raw, "basDt", "baseDate", "stdDt", "aplcStrtDt", "APLCN_BGNG_YMD")},
        ])

    return dict(
        insurer=_get(raw, "cmpyNm", "companyName", "orgNm", "upOrgNm", "issuer"),
        insurance_name=_get(raw, "prdNm", "insrncGdsNm", "productName", "goodsNm", "insrncPrdNm", "GDS_NM", "INSU_NM", "INSU_GDS_NM"),
        top_benefit=_get(raw, "mog", "insrdNm", "summary", "topBenefit", "gdsClsNm", "GDS_CLSF_NM", "GIVE_BNFI_NM"),
        benefits=benefits,
        apply_url=_parse_url(raw, "hpgeUrl", "applyUrl", "url", "termsUrl"),
        accent_color="#8B5CF6",
    )


def _map_policy(source_code: str, raw: dict) -> dict:
    keyword_raw = _get(raw, "plcyKywdNm", "keywords", "keyword", "tags")
    if isinstance(keyword_raw, list):
        tags = _json_text(keyword_raw[:5])
    else:
        tags = _json_text([
            keyword.strip()
            for keyword in str(keyword_raw).split(",")[:5]
            if keyword.strip()
        ] if keyword_raw else [])

    end = str(_get(raw, "aplyYmd", "grntEnd", "endDate", "plcyApplyEndDt", "aplcEndDt", "applyEndDate"))
    deadline = _parse_date(raw, "grntEnd", "endDate", "plcyApplyEndDt", "aplcEndDt") if end and re.search(r"\d{8}", end) else end
    start = _get(raw, "grntFrom", "startDate", "plcyApplyStrtDt", "aplcStrtDt", "applyStartDate")

    return dict(
        policy_name=_get(raw, "plcyNm", "policyName", "polyBizNm", "name", "insrncGdsNm"),
        org=_get(raw, "sprvsnInstCdNm", "operInstCdNm", "orgNm", "upOrgNm", "organNm", "jrsdInsttNm", "institution"),
        category=_get(raw, "lclsfNm", "bizTycdNm", "polyBizSecd", "category", "policyCategory") or "기타",
        category_color="#3182F6",
        deadline=deadline,
        dday=None,
        tags=tags,
        core_benefit=str(_get(raw, "plcySprtCn", "plcyExplnCn", "coreBenefit", "summary", "polyBizNm") or "")[:255],
        description=_get(raw, "plcyExplnCn", "description", "applyMethod", "aplcMthd", "clmMthd"),
        age_min=_parse_int(raw, "sprtTrgtMinAge", "ageMin", "minAge"),
        age_max=_parse_int(raw, "sprtTrgtMaxAge", "ageMax", "maxAge"),
        income_condition=_get(raw, "earnEtcCn", "incomeCondition", "income"),
        employment_condition=_get(raw, "jobCd", "employmentCondition", "employment"),
        education_condition=_get(raw, "schoolCd", "educationCondition", "education"),
        apply_url=_parse_url(raw, "aplyUrlAddr", "hpgeUrl", "applyUrl", "plcyHomeUrl", "url"),
        application_period=f"{start} ~ {end}" if start or end else _get(raw, "aplyYmd", "applicationPeriod"),
    )


class SyncService:
    def __init__(self, db: Session):
        self.db = db

    def sync_products(self, items: list[dict], category: str) -> dict:
        saved, updated, skipped, failed = 0, 0, 0, 0

        for item in items:
            try:
                raw = item.get("rawPayload", {}) or {}
                external_id = item.get("externalId")
                source_code = item.get("sourceCode", "")

                if not external_id:
                    logger.warning("[SyncService] externalId missing. sourceCode=%s", source_code)
                    failed += 1
                    continue

                if category == "CARD":
                    model = CardProduct
                    mapped = _map_card(source_code, raw)
                elif category == "INSURANCE":
                    model = InsuranceProduct
                    mapped = _map_insurance(source_code, raw)
                elif category == "POLICY":
                    model = PolicyProduct
                    mapped = _map_policy(source_code, raw)
                    mapped.setdefault("conflict_policy_ids", "[]")
                else:
                    skipped += 1
                    continue

                existing = self.db.query(model).filter(model.external_id == external_id).first()
                if existing:
                    for key, value in mapped.items():
                        setattr(existing, key, value)
                    updated += 1
                else:
                    self.db.add(model(key=create_tsid(), external_id=external_id, **mapped))
                    saved += 1

            except IntegrityError:
                self.db.rollback()
                skipped += 1
                logger.warning("[SyncService] duplicated external_id skipped. external_id=%s", item.get("externalId"))
            except (TypeError, ValueError, AttributeError) as exc:
                failed += 1
                logger.exception("[SyncService] payload mapping failed. external_id=%s error=%s", item.get("externalId"), exc)
            except SQLAlchemyError as exc:
                self.db.rollback()
                failed += 1
                logger.exception("[SyncService] database save failed. external_id=%s error=%s", item.get("externalId"), exc)

        self._commit(category)
        logger.info("[SyncService] %s sync completed. saved=%s updated=%s skipped=%s failed=%s", category, saved, updated, skipped, failed)
        return {"saved": saved, "updated": updated, "skipped": skipped, "failed": failed}

    def _commit(self, label: str) -> None:
        try:
            self.db.commit()
        except SQLAlchemyError as exc:
            self.db.rollback()
            logger.exception("[SyncService] %s commit failed. error=%s", label, exc)
            raise
