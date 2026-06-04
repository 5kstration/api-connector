from sqlalchemy import Column, DateTime, Integer, String, Text
from sqlalchemy.orm import DeclarativeBase
from sqlalchemy.sql import func


class Base(DeclarativeBase):
    pass


class PolicyProduct(Base):
    __tablename__ = "policy_product"

    key = Column(String(26), primary_key=True, nullable=False)
    policy_name = Column(String(255), nullable=True)
    org = Column(String(255), nullable=True)
    category = Column(String(100), nullable=True)
    category_color = Column(String(20), nullable=True)
    deadline = Column(String(50), nullable=True)
    dday = Column(Integer, nullable=True)
    tags = Column(Text, nullable=True)
    core_benefit = Column(String(255), nullable=True)
    description = Column(Text, nullable=True)
    age_min = Column(Integer, nullable=True)
    age_max = Column(Integer, nullable=True)
    income_condition = Column(String(255), nullable=True)
    employment_condition = Column(String(255), nullable=True)
    education_condition = Column(String(255), nullable=True)
    application_period = Column(String(255), nullable=True)
    apply_url = Column(Text, nullable=True)
    external_id = Column(String(255), nullable=True, unique=True)
    conflict_policy_ids = Column(Text, nullable=True, default="[]")
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())


class InsuranceProduct(Base):
    __tablename__ = "insurance_product"

    key = Column(String(26), primary_key=True, nullable=False)
    insurer = Column(String(255), nullable=True)
    insurance_name = Column(String(255), nullable=True)
    top_benefit = Column(String(255), nullable=True)
    benefits = Column(Text, nullable=True)
    apply_url = Column(Text, nullable=True)
    accent_color = Column(String(20), nullable=True)
    external_id = Column(String(255), nullable=True, unique=True)
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())


class CardProduct(Base):
    __tablename__ = "card_product"

    key = Column(String(26), primary_key=True, nullable=False)
    company = Column(String(255), nullable=True)
    card_name = Column(String(255), nullable=True)
    top_benefit = Column(String(255), nullable=True)
    benefits = Column(Text, nullable=True)
    apply_url = Column(Text, nullable=True)
    accent_color = Column(String(20), nullable=True)
    external_id = Column(String(255), nullable=True, unique=True)
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
