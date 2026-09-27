"""
Master Test Runner for Personnel Wellness Backend & ML Pipeline (SIH26186)
=============================================================================
Runs all test modules and outputs exact test statistics.
"""

import sys
import os

# Add backend root to sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.database import Base, engine, SessionLocal
from app.auth.service import ensure_seed_data

import tests.test_database as td
import tests.test_prediction as tp
import tests.test_auth_rbac as tar
import tests.test_privacy_security as tps
import tests.test_comprehensive_flows as tcf
import tests.test_daily_checkin_rule as tdcr


def run_master_test_suite():
    # Re-initialize fresh test database
    if os.path.exists("wellness.db"):
        try:
            os.remove("wellness.db")
        except Exception:
            pass

    Base.metadata.create_all(bind=engine)
    with SessionLocal() as db:
        ensure_seed_data(db)

    test_modules = [
        ("Database Layer (test_database.py)", [
            td.test_required_tables_exist,
            td.test_seed_users_for_all_four_roles,
            td.test_orm_relationships_and_cascade,
            td.test_health_endpoint,
        ]),
        ("ML Risk Model Pipeline (test_prediction.py)", [
            tp.test_predict_endpoint_high_risk,
            tp.test_predict_endpoint_low_risk,
        ]),
        ("Authentication & RBAC (test_auth_rbac.py)", [
            tar.test_login_and_jwt_generation,
            tar.test_authenticated_get_me,
            tar.test_personnel_role_rules,
            tar.test_welfare_officer_role_rules_and_audit_logging,
            tar.test_commander_role_rules_and_aggregation,
            tar.test_admin_role_rules,
        ]),
        ("Privacy, Consent & Security (test_privacy_security.py)", [
            tps.test_security_headers,
            tps.test_consent_grant_and_revoke_flow,
            tps.test_consent_enforcement_on_welfare_officer_access,
            tps.test_sensitive_data_redaction,
            tps.test_minimum_necessary_data_exposure_unit_summary,
        ]),
        ("One Check-in Per Day Rule (test_daily_checkin_rule.py)", [
            tdcr.test_one_checkin_per_day_rule,
        ]),
        ("Comprehensive User Flows & Edge Cases (test_comprehensive_flows.py)", [
            tcf.test_flow_1_and_8_personnel_and_officer_login,
            tcf.test_flow_2_wellness_checkin,
            tcf.test_flow_3_assessment_submission,
            tcf.test_flow_4_5_6_7_ml_prediction_explanation_recommendations_history,
            tcf.test_flow_9_10_12_officer_dashboard_rbac_consent,
            tcf.test_flow_11_interventions,
            tcf.test_flow_13_jwt_invalidation_and_invalid_tokens,
            tcf.test_edge_cases_and_negative_inputs,
        ]),
    ]

    total_tests = 0
    passed_tests = 0
    failed_tests = 0
    failure_details = []

    print("=" * 80)
    print("      SIH26186 PERSONNEL WELLNESS BACKEND & ML PIPELINE MASTER TEST SUITE")
    print("=" * 80)

    for module_name, test_funcs in test_modules:
        print(f"\n--- {module_name} ---")
        for fn in test_funcs:
            total_tests += 1
            try:
                fn()
                passed_tests += 1
                print(f"  [PASS] {fn.__name__}")
            except Exception as e:
                failed_tests += 1
                err_msg = f"{fn.__name__}: {type(e).__name__} - {str(e)}"
                failure_details.append(err_msg)
                print(f"  [FAIL] {err_msg}")

    print("\n" + "=" * 80)
    print("                           SUMMARY OF RESULTS")
    print("=" * 80)
    print(f"  Total Tests Executed : {total_tests}")
    print(f"  Passed Tests         : {passed_tests}")
    print(f"  Failed Tests         : {failed_tests}")

    if failure_details:
        print("\nFailure Details:")
        for fd in failure_details:
            print(f"  - {fd}")
    else:
        print("  All tests completed successfully with 0 failures!")
    print("=" * 80)


if __name__ == "__main__":
    run_master_test_suite()
