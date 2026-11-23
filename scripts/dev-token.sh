#!/usr/bin/env bash
# Prints a dev JWT (HS256) for one Barkito role, or for all of them.
# Usage, from the repository root: scripts/dev-token.sh <END_USER|OPERATOR|TECHNICIAN|CLIENT_MANAGER|ADMIN|ALL>
# The secret is $BARKITO_DEV_JWT_SECRET, or else barkito.dev.jwt-secret from application-dev.properties.
set -eu

ROLES="END_USER OPERATOR TECHNICIAN CLIENT_MANAGER ADMIN"
PROPERTIES="backend/src/main/resources/application-dev.properties"
ISSUER="barkito-dev"
VALID_SECONDS=$((365 * 24 * 60 * 60))

role="${1:-}"
if [ "$role" = "ALL" ]; then
  roles_json='"END_USER","OPERATOR","TECHNICIAN","CLIENT_MANAGER","ADMIN"'
else
  case " $ROLES " in
    *" $role "*) roles_json="\"$role\"" ;;
    *)
      echo "Usage: $0 <END_USER|OPERATOR|TECHNICIAN|CLIENT_MANAGER|ADMIN|ALL>" >&2
      exit 1
      ;;
  esac
fi

secret="${BARKITO_DEV_JWT_SECRET:-}"
if [ -z "$secret" ] && [ -f "$PROPERTIES" ]; then
  secret=$(grep '^barkito\.dev\.jwt-secret=' "$PROPERTIES" | head -n 1 | cut -d= -f2-)
fi
if [ -z "$secret" ]; then
  echo "No dev secret: set BARKITO_DEV_JWT_SECRET or run from the repository root." >&2
  exit 1
fi

base64url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

now=$(date +%s)
subject="dev-$(echo "$role" | tr '[:upper:]' '[:lower:]')"
header=$(printf '%s' '{"alg":"HS256","typ":"JWT"}' | base64url)
claims=$(printf '{"sub":"%s","iss":"%s","iat":%s,"exp":%s,"realm_access":{"roles":[%s]}}' \
  "$subject" "$ISSUER" "$now" "$((now + VALID_SECONDS))" "$roles_json" | base64url)
signature=$(printf '%s' "$header.$claims" | openssl dgst -sha256 -hmac "$secret" -binary | base64url)

echo "$header.$claims.$signature"
