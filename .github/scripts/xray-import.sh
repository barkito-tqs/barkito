#!/usr/bin/env bash
# Imports the backend test results into Xray as one Test Execution (PM5, QA Manual 2.3).
#   1. Cucumber JSON: Xray creates or updates one Test per scenario and links it to the
#      story through the feature tag (for example @BRK-42).
#   2. JUnit XML of the other tests is added to the same Test Execution as extra evidence.
# Run from the backend folder after `mvn verify`. Needs XRAY_CLIENT_ID and XRAY_CLIENT_SECRET.
set -euo pipefail

XRAY_API=https://xray.cloud.getxray.app/api/v2
PROJECT_KEY=BRK
CUCUMBER_REPORT=target/cucumber.json
JUNIT_REPORT=target/xray-junit.xml

code=$(curl --silent --show-error -o target/xray-auth.txt -w '%{http_code}' -X POST \
  -H "Content-Type: application/json" \
  --data "{\"client_id\":\"$XRAY_CLIENT_ID\",\"client_secret\":\"$XRAY_CLIENT_SECRET\"}" \
  "$XRAY_API/authenticate")
if [ "$code" != "200" ]; then
  echo "::error::Xray authenticate failed: HTTP $code"; cat target/xray-auth.txt; exit 1
fi
token=$(tr -d '"' < target/xray-auth.txt)

test_exec_key=""
if [ -s "$CUCUMBER_REPORT" ]; then
  cat > target/xray-info.json <<JSON
{
  "fields": {
    "project": { "key": "$PROJECT_KEY" },
    "summary": "CI ${GITHUB_REF_NAME:-local} ${GITHUB_SHA:0:7}",
    "issuetype": { "name": "Test Execution" }
  }
}
JSON
  code=$(curl --silent --show-error -o target/xray-cucumber.txt -w '%{http_code}' -X POST \
    -H "Authorization: Bearer $token" \
    -F "info=@target/xray-info.json;type=application/json" \
    -F "results=@$CUCUMBER_REPORT;type=application/json" \
    "$XRAY_API/import/execution/cucumber/multipart")
  cat target/xray-cucumber.txt; echo
  if [ "$code" != "200" ]; then
    echo "::error::Xray Cucumber import failed: HTTP $code"; exit 1
  fi
  test_exec_key=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["key"])' target/xray-cucumber.txt)
  echo "Test Execution: $test_exec_key"
fi

# The Cucumber suite is already imported above, so its JUnit report is left out.
python3 - "$JUNIT_REPORT" <<'PY'
import glob
import sys
import xml.etree.ElementTree as ET

merged = ET.Element("testsuites")
for path in glob.glob("target/surefire-reports/TEST-*.xml") + glob.glob("target/failsafe-reports/TEST-*.xml"):
    if ".bdd." in path:
        continue
    merged.append(ET.parse(path).getroot())
ET.ElementTree(merged).write(sys.argv[1])
PY

url="$XRAY_API/import/execution/junit?projectKey=$PROJECT_KEY"
if [ -n "$test_exec_key" ]; then
  url="$url&testExecKey=$test_exec_key"
fi
code=$(curl --silent --show-error -o target/xray-junit.txt -w '%{http_code}' -X POST \
  -H "Content-Type: text/xml" \
  -H "Authorization: Bearer $token" \
  --data @"$JUNIT_REPORT" \
  "$url")
cat target/xray-junit.txt; echo
if [ "$code" != "200" ]; then
  echo "::error::Xray JUnit import failed: HTTP $code"; exit 1
fi
