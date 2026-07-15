#!/bin/bash
set -euo pipefail

APP_DIR=/home/ec2-user/hard-click
REPO_RAW_BASE="https://raw.githubusercontent.com/Hard-Click/Hard-Click-BackEnd/main"
SSM_PATH="/hard-click/prod"
REGION="ap-northeast-2"

mkdir -p "$APP_DIR/monitoring" "$APP_DIR/logs"
cd "$APP_DIR"

curl -fsSL "$REPO_RAW_BASE/docker-compose.prod.yml" -o docker-compose.prod.yml
curl -fsSL "$REPO_RAW_BASE/monitoring/promtail-config.prod.yml" -o monitoring/promtail-config.prod.yml

: > .env
aws ssm get-parameters-by-path \
  --path "$SSM_PATH" \
  --with-decryption \
  --region "$REGION" \
  --query "Parameters[*].[Name,Value]" \
  --output text | while IFS=$'\t' read -r name value; do
    key=$(basename "$name")
    echo "${key}=${value}" >> .env
done

if ! docker compose version > /dev/null 2>&1; then
  mkdir -p ~/.docker/cli-plugins
  curl -SL https://github.com/docker/compose/releases/download/v2.27.0/docker-compose-linux-x86_64 -o ~/.docker/cli-plugins/docker-compose
  chmod +x ~/.docker/cli-plugins/docker-compose
fi

docker compose -f docker-compose.prod.yml pull
docker rm -f hard-click-app hard-click-promtail 2>/dev/null || true
docker compose -f docker-compose.prod.yml up -d --remove-orphans
docker image prune -f
