#!/usr/bin/env python3

"""
* Run this script inside your CentOS VM as the admin user. 
* It will ask for your sudo password when installing packages.
"""


import os
import subprocess
import sys

def run_cmd(command, cwd=None, check=True):
    """Utility to run shell commands and print them."""
    print(f"\n[RUNNING] {command}")
    try:
        subprocess.run(command, shell=True, check=check, cwd=cwd, executable='/bin/bash')
    except subprocess.CalledProcessError as e:
        print(f"[ERROR] Command failed with exit code {e.returncode}")
        sys.exit(1)

def main():
    print("=== OpenNMS Infrastructure VM Setup ===")
    
    # 1. Install dependencies (requires sudo)
    print("\n--- Installing System Dependencies ---")
    run_cmd("sudo dnf update -y")
    run_cmd("sudo dnf install -y java-17-openjdk-devel maven git podman wget curl nano")
    
    # 2. Clone the repository
    repo_url = "https://github.com/ishtiaq2/opennms-infrastructure.git"
    repo_dir = "opennms-infrastructure"
    
    print("\n--- Cloning Repository ---")
    if not os.path.exists(repo_dir):
        run_cmd(f"git clone {repo_url}")
    else:
        print(f"Directory '{repo_dir}' already exists. Skipping clone.")

    # 3. Generate SSH Keys
    print("\n--- Setting up SSH Keys ---")
    ssh_key_path = os.path.expanduser("~/.ssh/id_ed25519")
    if not os.path.exists(ssh_key_path):
        # Generates key without prompting for a passphrase (-N "")
        run_cmd(f'ssh-keygen -t ed25519 -N "" -f {ssh_key_path}')
        run_cmd("eval \"$(ssh-agent -s)\" && ssh-add ~/.ssh/id_ed25519")
    else:
        print("SSH key already exists. Skipping generation.")

    # 4. Git Configuration
    print("\n--- Configuring Git ---")
    run_cmd('git config --global user.name "Muhammad Ishtiaq Hussain"')
    run_cmd('git config --global user.email "muis0121@gmail.com"')

    # Change remote to SSH
    run_cmd("git remote set-url origin git@github.com:ishtiaq2/opennms-infrastructure.git", cwd=repo_dir)
    print("[SUCCESS] Git remote changed to SSH.")

    # 5. Output instructions for GitHub
    print("\n=======================================================")
    print("SETUP ALMOST COMPLETE! Manual action required:")
    print("=======================================================")
    print("1. Copy the SSH public key below:")
    print("-------------------------------------------------------")
    
    with open(f"{ssh_key_path}.pub", "r") as pub_key_file:
        print(pub_key_file.read().strip())
        
    print("-------------------------------------------------------")
    print("2. Go to GitHub -> Settings -> SSH and GPG keys -> New SSH key")
    print("3. Paste the key and save.")
    print(f"4. Test the connection by running: ssh -T git@github.com")
    print("=======================================================\n")

if __name__ == "__main__":
    main()