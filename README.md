
# OpenNMS Architecture & Infrastructure Exploration

## 🎯 Project Objectives
  This repository serves as a hands-on learning environment to deconstruct OpenNMS from a Software Engineering and Architectural perspective. The primary goals are to understand:

* Architectural Breakdown: The core building blocks and subsystems of OpenNMS.
* Technology Stack: The underlying technologies, tools, and infrastructure (e.g., Apache Karaf, OSGi, Apache Felix, Maven).
* Design Justification: Why OpenNMS relies on this specific tech stack for enterprise-grade network management (e.g., zero-downtime 
  hot-swapping, modularity).
* Cross-Domain Application: How these underlying technologies and architectural patterns are utilized in other software domains outside 
  of OpenNMS.
* Alternative to OSGI and Karaf   

## 🚀 Environment Setup

### Virtualization & Operating System    
This environment is built to run on a local Virtual Machine.

* Hypervisor: Install VirtualBox (or VMware). This guide assumes Oracle VM VirtualBox Manager.
* Operating System: Download and install CentOS Stream 9.
      ISO Used: CentOS-Stream-9-20260723.0-x86_64-boot.iso 
      (https://odcs.stream.centos.org/production/latest-CentOS-Stream/compose/BaseOS/x86_64/iso/?utm_source=chatgpt.com)

#### System Provisioning:
  Once CentOS is installed, log in as root (or a user with sudo privileges) and set up the dedicated admin user:

##### Create the user and set a password
  * useradd admin
  * passwd admin

* Add the user to the sudoers (wheel) group
  usermod -aG wheel admin

* Verify the user groups
  id admin
    
#### Log in as the new admin user and install the required development tools:
* sudo dnf update -y    
* sudo dnf install -y java-17-openjdk-devel maven git podman wget curl nano

### Cloning the Repository
  Clone this repository into your VM:
  * git clone https://github.com/ishtiaq2/opennms-infrastructure.git
  * cd opennms-infrastructure

### 💻 Developer Configuration (Personal Setup)
  The following steps are for the repository maintainer to configure SSH access and Git identity for pushing commits.

  * Generate and Add SSH Keys
    #### Generate a new Ed25519 SSH key
    * ssh-keygen -t ed25519 -C "muis0121@gmail.com"

   #### Start the ssh-agent and add the key
   * eval "$(ssh-agent -s)"
   * ssh-add ~/.ssh/id_ed25519

  * Output the public key to the terminal
    * cat ~/.ssh/id_ed25519.pub

  Copy the output, sign in to GitHub, navigate to Settings → SSH and GPG keys → New SSH key, paste the key, and save.

### Update Git Remote & Test Connection
  Test the GitHub SSH connection
  * ssh -T git@github.com

  Change the repository remote URL from HTTPS to SSH
  * git remote set-url origin git@github.com:ishtiaq2/opennms-infrastructure.git

  Verify the change
  * git remote -v

  Configure Git Identity  
  * git config --global user.name "Muhammad Ishtiaq Hussain"
  * git config --global user.email "muis0121@gmail.com"  

### 🔗 Host Machine Integration
  To write code comfortably from your local development machine (host) while executing it on the CentOS VM, mount the VM's project directory locally using sshfs.

  Run this command on your host machine terminal:
  Replace <centos-ip> with the actual IP address of your VM
  * sudo sshfs -o allow_other admin@<centos-ip>:/home/admin/opennms-infrastructure ./opennms-infrastructure

You can now open the ./opennms-infrastructure folder in VS Code, IntelliJ, or your preferred local IDE.
