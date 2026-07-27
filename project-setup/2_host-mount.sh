#!/bin/bash

echo "=== Mount CentOS VM Directory via SSHFS ==="

# Check if sshfs is installed (basic check for Linux/Mac)
if ! command -v sshfs &> /dev/null; then
    echo "[ERROR] 'sshfs' is not installed on your host machine."
    echo "Please install it (e.g., 'sudo apt install sshfs' or 'brew install macfuse sshfs') and try again."
    exit 1
fi

# Get VM IP from user
read -p "Enter the IP address of your CentOS VM: " centos_ip

# Strip leading/trailing whitespace and check if empty
centos_ip=$(echo "$centos_ip" | xargs)
if [ -z "$centos_ip" ]; then
    echo "[ERROR] IP address cannot be empty."
    exit 1
fi

# Define paths
remote_path="admin@${centos_ip}:/home/admin/opennms-infrastructure"
local_path="./opennms-infrastructure"

# Create local directory if it doesn't exist
if [ ! -d "$local_path" ]; then
    mkdir -p "$local_path"
    echo "[INFO] Created local mount point at: $local_path"
fi

# Mount command
# Note: '-o allow_other' might require configuration in /etc/fuse.conf on the host
echo -e "\n[RUNNING] sudo sshfs -o allow_other $remote_path $local_path"
echo "You may be prompted for your host sudo password, followed by the VM admin password."

# Execute the mount command
if sudo sshfs -o allow_other "$remote_path" "$local_path"; then
    echo -e "\n[SUCCESS] Successfully mounted VM repository to '$local_path'."
    echo "You can now open this folder in your local IDE (VS Code, IntelliJ, etc.)."
    echo "To unmount later, run: sudo umount $local_path"
else
    echo -e "\n[ERROR] Failed to mount the directory. Ensure SSH is running on the VM and the IP is correct."
    exit 1
fi