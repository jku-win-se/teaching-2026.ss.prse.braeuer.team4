# Issue #105: Coverage Badge Alignment with mvn verify

**Status**: ✅ COMPLETED

## Problem
The code coverage badge in README.md was showing coverage for the entire codebase (including UI classes), while `mvn verify` calculates coverage only for non-UI service code. This mismatch caused confusing and inconsistent coverage reporting.

## Root Cause Analysis
- **OLD BEHAVIOR**: CI/CD workflow calculated coverage from all classes in `target/site/jacoco/jacoco.csv`
- **mvn verify**: Only checks coverage for specific service packages (defined in pom.xml JaCoCo check-service-coverage execution)
  - Includes: `service/mock/*`, `service/real/auth/*`, `service/real/schedule/*`
  - Excludes: `service/api/*`, anonymous classes (`$`)

## Solution Implemented

### Modified File
- **File**: `.github/workflows/Continuous Integration.yaml`
- **Step**: "Update README coverage badge"

### Changes
Updated the AWK script in the coverage badge calculation to:
1. **Filter jacoco.csv** to include only non-UI service classes:
   - Pattern: `service/(mock|real/(auth|schedule))/`
   - Exclude: `service/api/*` and anonymous classes (`$`)
2. **Calculate coverage** only for filtered classes
3. **Generate badge** based on filtered coverage percentage

### Implementation Details
```bash
# New coverage calculation filters:
# 1. Skip service/api/* (API layer classes)
# 2. Skip anonymous classes ($)
# 3. Include only service/mock/*, service/real/auth/*, service/real/schedule/*
awk -F, '
  NR==1 { next }  # Skip header
  /service\/api\// { next }  # Exclude API classes
  /\$/ { next }  # Exclude anonymous classes
  /service\/(mock|real\/(auth|schedule))/ {
    miss += $8
    cov += $9
  }
  END {
    # Calculate final percentage
  }
' target/site/jacoco/jacoco.csv
```

## Benefits
✅ Coverage badge now matches `mvn verify` output  
✅ Consistent coverage reporting across local and CI/CD environments  
✅ Accurate reflection of code quality for testable service code  
✅ UI code exclusion prevents misleading coverage metrics  

## Verification
- CI/CD workflow now calculates coverage from the same package set as `mvn verify`
- Badge percentage will align with `mvn verify` command output
- Future badge updates will maintain consistency

## Testing Notes
- Run `mvn verify` locally to confirm coverage matches README badge on next push
- CI/CD will update badge automatically on next push to main branch
