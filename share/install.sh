#!/bin/bash

set -e

REPO_URL="https://Cy4Shot.github.io/repo"
PACKAGE_NAME="brick"
APT_KEYRING_PATH="/usr/share/keyrings/${PACKAGE_NAME}.gpg"
APT_LIST_PATH="/etc/apt/sources.list.d/${PACKAGE_NAME}.list"
YUM_REPO_PATH="/etc/yum.repos.d/${PACKAGE_NAME}.repo"

echo "🧠 Detecting package manager..."

if command -v apt >/dev/null; then
    echo "📦 APT-based system detected."
    sudo rm -f "$APT_KEYRING_PATH" "$APT_LIST_PATH"

    echo "🔐 Adding GPG key..."
    curl -fsSL "$REPO_URL/public.key" | gpg --dearmor | sudo tee "$APT_KEYRING_PATH" > /dev/null

    echo "📝 Adding APT repo..."
    echo "deb [signed-by=$APT_KEYRING_PATH] $REPO_URL/debian stable main" | sudo tee "$APT_LIST_PATH" > /dev/null

    echo "🔄 Updating APT..."
    sudo apt update

    echo "⬇️ Installing $PACKAGE_NAME..."
    sudo apt install -y "$PACKAGE_NAME"

elif command -v dnf >/dev/null || command -v yum >/dev/null; then
    echo "📦 YUM-based system detected."

    echo "📝 Adding YUM repo..."
    sudo tee "$YUM_REPO_PATH" > /dev/null <<EOF
[${PACKAGE_NAME}]
name=${PACKAGE_NAME} Repository
baseurl=${REPO_URL}/rpm/
enabled=1
gpgcheck=0
EOF

    echo "🔄 Updating YUM..."
    if command -v dnf >/dev/null; then
        sudo dnf clean all
        sudo dnf makecache
        echo "⬇️ Installing $PACKAGE_NAME..."
        sudo dnf install -y "$PACKAGE_NAME"
    else
        sudo yum clean all
        sudo yum makecache
        echo "⬇️ Installing $PACKAGE_NAME..."
        sudo yum install -y "$PACKAGE_NAME"
    fi

else
    echo "❌ Unsupported package manager. Only apt, dnf, and yum are supported."
    exit 1
fi

echo "✅ Done! Run '$PACKAGE_NAME --help' to get started."
