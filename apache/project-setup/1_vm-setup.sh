#!/bin/bash

# ==============================================================================
# Script Name: vm-setup.sh
# Description: Automates the provisioning of a CentOS Virtual Machine for 
#              OpenNMS infrastructure development.
#
# Actions Performed:
#   1. System Update: Updates the CentOS package manager (dnf).
#   2. Toolchain Install: Installs Java 17, Maven, Git, Podman, and basic CLI tools.
#   3. Repository Setup: Clones the opennms-infrastructure GitHub repository.
#   4. SSH Key Generation: Creates an ED25519 SSH key for secure GitHub access.
#   5. Git Configuration: Sets global Git user credentials and configures the 
#                         cloned repository to use SSH instead of HTTPS.
#
# Execution Context: 
#   - Run this script INSIDE your CentOS VM.
#   - Run as your standard user (e.g., 'admin'). It will prompt for your sudo 
#     password to install the system packages.
#
# Post-Execution:
#   - The script will output a public SSH key to the console. You must manually
#     copy this key and add it to your GitHub account settings to enable push access.
# ==============================================================================

# Exit immediately if a command exits with a non-zero status
set -e

echo "=== OpenNMS Infrastructure VM Setup ==="

# 1. Install dependencies (requires sudo)
echo -e "\n--- Installing System Dependencies ---"
echo "[RUNNING] sudo dnf update -y"
sudo dnf update -y

echo "[RUNNING] sudo dnf install -y java-17-openjdk-devel maven git podman wget curl nano"
sudo dnf install -y java-17-openjdk-devel maven git podman wget curl nano

# 2. Clone the repository
REPO_URL="https://github.com/ishtiaq2/opennms-infrastructure.git"
REPO_DIR="opennms-infrastructure"

echo -e "\n--- Cloning Repository ---"
if [ ! -d "$REPO_DIR" ]; then
    echo "[RUNNING] git clone $REPO_URL"
    git clone "$REPO_URL"
else
    echo "Directory '$REPO_DIR' already exists. Skipping clone."
fi

# 3. Generate SSH Keys
echo -e "\n--- Setting up SSH Keys ---"
SSH_KEY_PATH="$HOME/.ssh/id_ed25519"

if [ ! -f "$SSH_KEY_PATH" ]; then
    echo "[RUNNING] ssh-keygen -t ed25519 -N \"\" -f $SSH_KEY_PATH"
    ssh-keygen -t ed25519 -N "" -f "$SSH_KEY_PATH"
    
    echo "[RUNNING] eval \"\$(ssh-agent -s)\" && ssh-add $SSH_KEY_PATH"
    eval "$(ssh-agent -s)" && ssh-add "$SSH_KEY_PATH"
else
    echo "SSH key already exists. Skipping generation."
fi

# 4. Git Configuration
echo -e "\n--- Configuring Git ---"
echo "[RUNNING] git config --global user.name \"Muhammad Ishtiaq Hussain\""
git config --global user.name "Muhammad Ishtiaq Hussain"

echo "[RUNNING] git config --global user.email \"muis0121@gmail.com\""
git config --global user.email "muis0121@gmail.com"

echo "[RUNNING] Changing remote to SSH..."
cd "$REPO_DIR"
git remote set-url origin git@github.com:ishtiaq2/opennms-infrastructure.git
cd ..
echo "[SUCCESS] Git remote changed to SSH."

# 5. Output instructions for GitHub
echo -e "\n======================================================="
echo "SETUP ALMOST COMPLETE! Manual action required:"
echo "======================================================="
echo "1. Copy the SSH public key below:"
echo "-------------------------------------------------------"

cat "${SSH_KEY_PATH}.pub"

echo "-------------------------------------------------------"
echo "2. Go to GitHub -> Settings -> SSH and GPG keys -> New SSH key"
echo "3. Paste the key and save."
echo "4. Test the connection by running: ssh -T git@github.com"
echo -e "=======================================================\n"