#!/usr/bin/env python3

"""
* Run this script on your host machine (the computer running VirtualBox). 
* It will prompt you for the VM's IP address and execute the sshfs command.
"""
import os
import subprocess
import sys

def main():
    print("=== Mount CentOS VM Directory via SSHFS ===")
    
    # Check if sshfs is installed (basic check for Linux/Mac)
    if subprocess.call("command -v sshfs", shell=True, stdout=subprocess.DEVNULL) != 0:
        print("[ERROR] 'sshfs' is not installed on your host machine.")
        print("Please install it (e.g., 'sudo apt install sshfs' or 'brew install macfuse sshfs') and try again.")
        sys.exit(1)

    # Get VM IP from user
    centos_ip = input("Enter the IP address of your CentOS VM: ").strip()
    if not centos_ip:
        print("[ERROR] IP address cannot be empty.")
        sys.exit(1)

    # Define paths
    remote_path = f"admin@{centos_ip}:/home/admin/opennms-infrastructure"
    local_path = "./opennms-infrastructure"

    # Create local directory if it doesn't exist
    if not os.path.exists(local_path):
        os.makedirs(local_path)
        print(f"[INFO] Created local mount point at: {local_path}")

    # Mount command
    # Note: '-o allow_other' might require configuration in /etc/fuse.conf on the host
    mount_cmd = f"sudo sshfs -o allow_other {remote_path} {local_path}"
    
    print(f"\n[RUNNING] {mount_cmd}")
    print("You may be prompted for your host sudo password, followed by the VM admin password.")
    
    try:
        subprocess.run(mount_cmd, shell=True, check=True)
        print(f"\n[SUCCESS] Successfully mounted VM repository to '{local_path}'.")
        print("You can now open this folder in your local IDE (VS Code, IntelliJ, etc.).")
        print(f"To unmount later, run: sudo umount {local_path}")
    except subprocess.CalledProcessError:
        print("\n[ERROR] Failed to mount the directory. Ensure SSH is running on the VM and the IP is correct.")

if __name__ == "__main__":
    main()