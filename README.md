Goal:
1. To understand OpenNMS from a Software Architect and Software Engineer point of view.
2. It's building blocks.
3. The underlying tecnologies, tools and infrastures.
4. Why OpenNMS rely on those technologies
5. The underlying tecnologies, tools and infrastures in other domains.

Steps:
  1. Install VirtualBox or VMware. This guide uses Oracle VM VirtualBox Manager.
  2. Install CentOS, this guide uses CentOS-Stream-9-20260723.0-x86_64-boot.iso: https://odcs.stream.centos.org/production/latest-CentOS-Stream/compose/BaseOS/x86_64/iso/?utm_source=chatgpt.com
  3. Create user and add to sudo group.
    a. useradd admin
    b. passwd admin
    c. usermod -aG wheel admin
    d. id admin
  5. sudo dnf update -y && sudo dnf install -y java-17-openjdk-devel maven git podman wget curl nano

Clone Repo: 
  1. git clone https://github.com/ishtiaq2/opennms-infrastructure.git

  The following is for my personal use:
  3. ssh-keygen
  4. ssh-add ~/.ssh/id_ed25519
  5. cat ~/.ssh/id_ed25519.pub and copy the entire output.
  6. Add it to GitHub
     a Sign in to GitHub and Go to Settings → SSH and GPG keys.
     b. Click New SSH key and Paste the key and save.
    
  7. Change the repository to use SSH
    Check your current remote:
    git remote -v
    Change it:
    git remote set-url origin git@github.com:ishtiaq2/opennms-infrastructure.git
    Test the connection:
    ssh -T git@github.com
  8. [admin@localhost opennms-infrastructure]$ git config --global user.name "Muhammad Ishtiaq Hussain"
     [admin@localhost opennms-infrastructure]$ git config --global user.email "muis0121@gmail.com"
  
  
Copy the repo to your development machine:
  sudo sshfs -o allow_other admin@<centos-ip>://home/admin/apache/opennms-infrastructure ./
  
