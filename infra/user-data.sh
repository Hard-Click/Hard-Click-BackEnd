#!/bin/bash
dnf install -y docker
systemctl enable --now docker
usermod -aG docker ec2-user

curl -fsSL https://raw.githubusercontent.com/Hard-Click/Hard-Click-BackEnd/main/infra/deploy.sh -o /tmp/deploy.sh
chmod +x /tmp/deploy.sh
/tmp/deploy.sh
