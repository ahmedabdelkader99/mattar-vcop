pipeline {
    agent any

    environment {
        TF_VAR_px_user     = credentials('proxmox-user')
        TF_VAR_px_password = credentials('proxmox-password')
    }

    parameters {
        string(name: 'PX_ENDPOINT', defaultValue: 'https://10.116.21.71:8006/api2/json', description: 'Proxmox API endpoint')
        string(name: 'PX_NODE', defaultValue: 's1vpsie01', description: 'Target Proxmox node')
        string(name: 'TEMPLATES_SRV_IP', defaultValue: '111', description: 'IP for Templates Server')
        string(name: 'ciuser', defaultValue: 'vpsie', description: 'user name for ssh access to VMs')
        string(name: 'CLONE_TEMPLATE', defaultValue: 'ci001', description: 'Base VM/template to clone')
        string(name: 'DEFAULT_STORAGE', defaultValue: 'local', description: 'Proxmox storage location for VM disks')
        string(name: 'PREFIX', defaultValue: 'test-', description: 'Prefix for VM names')
        string(name: 'DISK_SIZE', defaultValue: '40', description: 'Disk size (GB) per VM')
        string(name: 'SUBNET', defaultValue: '10.x.x', description: 'Subnet for VMs')
        string(name: 'GATEWAY', defaultValue: '10.x.x.x', description: 'Gateway IP for the subnet')
        string(name: 'apikey', defaultValue: 'Generate your API key using API Key Generator ', description: 'https://codepen.io/corenominal/pen/rxOmMJ')
        password(name: 'DB_PASSWORD', defaultValue: '', description: 'Root password for the database cluster')
        string(name: 'DB_USERNAME', defaultValue: 'DB_userName', description: 'username for the database cluster')
        file(name: 'DB_FILE', description: 'Upload your DB Dump file')
        file(name: 'MONGO_DB_FILE', description: 'Upload your Mongo_DB Dump file')
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/ahmedabdelkader99/mattar-vcop'
            }
        }

        stage('Check Proxmox Login') {
            steps {
                sh '''
                    echo "🔑 Checking Proxmox login for user: $TF_VAR_px_user"

                    # Perform login and capture response
                    LOGIN_RESPONSE=$(curl -sk \
                        -d "username=$TF_VAR_px_user&password=$TF_VAR_px_password" \
                        "$PX_ENDPOINT")

                    # Check if login returned a valid ticket
                    if echo "$LOGIN_RESPONSE" | jq -e '.data.ticket' >/dev/null; then
                        echo "✅ Login succeeded to Proxmox API $PX_ENDPOINT"
                    else
                        echo "❌ Login FAILED to Proxmox API $PX_ENDPOINT"
                        echo "Response from server:"
                        echo "$LOGIN_RESPONSE"
                        exit 1
                    fi
                '''
            }
            post {
                success {
                    echo '✅ Proxmox login check completed successfully.'
                }
                failure {
                    echo '❌ Proxmox login check failed. Check credentials and endpoint.'
                }
            }
        }

        stage('Generate terraform.tfvars') {
            steps {
                script {
                    // Update only the parameters inside terraform.tfvars.tmp
                    sh """
                    sed -i '/^px_user/d' terraform.tfvars.tmp
                    sed -i '/^px_password/d' terraform.tfvars.tmp
                    sed -i "s|^px_endpoint *=.*|px_endpoint    = \\"${params.PX_ENDPOINT}\\"|g" terraform.tfvars.tmp
                    sed -i "s|^pxTargetNode *=.*|pxTargetNode   = \\"${params.PX_NODE}\\"|g" terraform.tfvars.tmp
                    sed -i "s|^clone *=.*|clone          = \\"${params.CLONE_TEMPLATE}\\"|g" terraform.tfvars.tmp
                    sed -i "s|^defaultStorage *=.*|defaultStorage = \\"${params.DEFAULT_STORAGE}\\"|g" terraform.tfvars.tmp
                    sed -i "s|^prefix *=.*|prefix         = \\"${params.PREFIX}\\"|g" terraform.tfvars.tmp
                    sed -i "s|^diskSize *=.*|diskSize       = ${params.DISK_SIZE}|g" terraform.tfvars.tmp
                    sed -i "s|^subnet *=.*|subnet         = \\"${params.SUBNET}\\"|g" terraform.tfvars.tmp
                    sed -i "s|^gateway *=.*|gateway        = \\"${params.GATEWAY}\\"|g" terraform.tfvars.tmp
                    sed -i "s|^templatesSrvIP *=.*|templatesSrvIP = ${params.TEMPLATES_SRV_IP}|g" terraform.tfvars.tmp
                    sed -i "s|^ciuser *=.*|ciuser         = \\"${params.ciuser}\\"|g" terraform.tfvars.tmp
                    sed -i "s|^apiKey *=.*| apikey         = \\"${params.apikey}\\"|g" terraform.tfvars.tmp
                    """
                    sh '''
                    PUBKEY=$(cat /var/jenkins_home/.ssh/id_rsa.pub | tr -d '\n')
                    sed -i "s|^sshkeys *=.*|sshkeys        = \\\"${PUBKEY}\\\"|g" terraform.tfvars.tmp
                    '''

                    // Save the final version
                    sh 'cp terraform.tfvars.tmp terraform.tfvars'
                    sh "echo '✅ Generated terraform.tfvars:' && cat terraform.tfvars"
                }
            }
            post {
                success {
                    echo '✅ Generated terraform.tfvars successfully.'
                }
                failure {
                    echo '❌ Generating terraform.tfvars failed! Check the logs above for details.'
                }
            }
        }

        stage('Terraform Init') {
            steps {
                sh 'terraform init'
            }
            post {
                success {
                    echo '✅ Terraform initialized successfully.'
                }
                failure {
                    echo '❌ Terraform init failed! Check the logs above for details.'
                }
            }
        }

        stage('Terraform Plan') {
            steps {
                sh 'terraform plan'
            }
            post {
                success {
                    echo '✅ Terraform plan completed successfully.'
                }
                failure {
                    echo '❌ Terraform plan failed! Check the logs above for details.'
                }
            }
        }

        stage('Terraform Apply') {
            steps {
                sh 'terraform apply -auto-approve'
            }
            post {
                success {
                    echo '✅ Terraform applied successfully.'
                }
                failure {
                    echo '❌ Terraform apply failed!'
                }
            }
        }

        stage('Genrate sshkey-pairs inside jenkins vm') {
            steps {
                sh '''
                if [ ! -f /var/jenkins_home/.ssh/id_rsa ]; then
                    echo "🔐 Generating SSH key pair..."
                    ssh-keygen -t rsa -b 2048 -f /var/jenkins_home/.ssh/id_rsa -q -N ""
                    echo "✅ SSH key pair generated."
                    echo "Public Key:"
                    echo "#################################"
                    cat /var/jenkins_home/.ssh/id_rsa.pub
                    echo "#################################"
                    echo "Private Key:"
                    echo "#################################"
                    cat /var/jenkins_home/.ssh/id_rsa
                    echo "#################################"
                else
                    echo "ℹ️ SSH key pair already exists, skipping generation."
                    echo "Public Key:"
                    echo "#################################"
                    cat /var/jenkins_home/.ssh/id_rsa.pub
                    echo "#################################"
                    echo "Private Key:"
                    echo "#################################"
                    cat /var/jenkins_home/.ssh/id_rsa
                    echo "#################################"
                fi
                '''
            }
            post {
                success {
                    echo '✅ Generated ssh key pairs successfully.'
                }
                failure {
                    echo '❌ Generating ssh key pairs failed!, Check if VMs are reachable.'
                }
            }
        }
        stage('Refresh known_hosts') {
            steps {
                sh '''
                SSH_DIR="/var/jenkins_home/.ssh"
                HOSTS_FILE="hosts"

                mkdir -p $SSH_DIR
                chmod 700 $SSH_DIR

                # Remove old file completely (clean slate)
                rm -f $SSH_DIR/known_hosts
                touch $SSH_DIR/known_hosts

                for HOST in $(grep -Eo '^[a-zA-Z0-9._-]+' $HOSTS_FILE); do
                    echo "➡️ Refreshing SSH key for $HOST..."

                    # Remove any old fingerprints for good measure
                    ssh-keygen -R "$HOST" 2>/dev/null || true

                    # Re-add without hashing (-H disabled by default)
                    ssh-keyscan "$HOST" >> $SSH_DIR/known_hosts 2>/dev/null || true
                done

                chmod 600 $SSH_DIR/known_hosts
                cat $SSH_DIR/known_hosts   # debug: show plain entries
                '''
            }
            post {
                success {
                    echo '✅ known_hosts refreshed successfully.'
                }
                failure {
                    echo '❌ Failed to refresh known_hosts. '
                }
            }
        }

        stage('List Created VMs') {
            steps {
                    sh '''
                        echo "📋 Getting Terraform outputs in JSON..."
                        terraform output -json > terraform_output.json
                        cat terraform_output.json
                        echo "terraform_output.json created in workspace ✅"
                        echo "******************************************************"

                        echo "📋 Parsing terraform_output.json and creating hosts file..."
                        > hosts

                        # Extract all values (arrays or single IPs) and filter IPv4 addresses
                        jq -r '
                        .[]
                        | .value
                        | if type=="array" then .[] else . end
                        ' terraform_output.json | grep -Eo '^[0-9.]+$' >> hosts

                        echo "******************************************************"
                        echo "✅ Generated hosts file content:"
                        cat hosts
                        echo "******************************************************"
                    '''
            }
            post {
                success {
                        echo '✅ Successfully generated hosts file from terraform_output.json.'
                }
                failure {
                        echo '❌ Failed to generate hosts file. Check Terraform outputs or jq parsing.'
                }
            }
        }
        stage('Distribute Terraform Output') {
            steps {
                script {
                    echo '📤 Distributing terraform_output.json to all subdirectories...'
                    sh '''
                    # Ensure terraform_output.json exists
                    if [ ! -f terraform_output.json ]; then
                        echo "❌ terraform_output.json not found in workspace root!"
                        exit 1
                    fi

                    # Loop over all subdirectories dynamically
                    for dir in */; do
                        [ -d "$dir" ] || continue
                        echo "➡️ Copying terraform_output.json to $dir"
                        cp terraform_output.json "$dir/"
                    done

                    echo "✅ terraform_output.json distributed successfully."
                    '''
                }
            }
        }

        stage('Check VM Availability') {
            steps {
                script {
                    def privateKey = '/var/jenkins_home/.ssh/id_rsa'
                    def hostsFile = 'hosts'
                    def maxRetries = 3         // number of attempts
                    def waitSeconds = 60       // wait time between retries

                    def vmIPs = readFile(hostsFile).split('\n').findAll { it?.trim() }

                    vmIPs.each { ip ->
                        echo "🔹 Checking VM ${ip} ..."
                        boolean reachable = false

                        for (int attempt = 1; attempt <= maxRetries; attempt++) {
                            echo "Attempt ${attempt} to ping ${ip} ..."
                            def pingStatus = sh(script: "ping -c 3 ${ip}", returnStatus: true)

                            if (pingStatus == 0) {
                                echo "✅ ${ip} is reachable via ping"
                                reachable = true

                                // Optional: check SSH key login
                                def sshStatus = sh(script: "ssh -i ${privateKey} -o BatchMode=yes -o ConnectTimeout=5 -o StrictHostKeyChecking=no ${params.ciuser}@${ip} 'echo SSH OK'", returnStatus: true)
                                if (sshStatus == 0) {
                                    echo "✅ SSH key works for ${ip}"
                        } else {
                                    echo "⚠️ SSH key login failed for ${ip}"
                                }

                                break  // exit retry loop if ping successful
                    } else {
                                echo "⚠️ ${ip} is not reachable on attempt ${attempt}"
                                if (attempt < maxRetries) {
                                    echo "⏳ Waiting ${waitSeconds} seconds before retry..."
                                    sleep(waitSeconds)
                                }
                            }
                        }

                        if (!reachable) {
                            echo "❌ ${ip} is not reachable after ${maxRetries} attempts"
                        }

                        echo '------------------------------------'
                    }
                }
            }
            post {
                success {
                    echo '✅ Checked VM availability successfully.'
                }
                failure {
                    echo '❌ Checking VM availability encountered issues.'
                }
            }
        }

        stage('Deploy the K8s Cluster') {
            steps {
                script {
                    sh '''
                        echo "📋 Getting Terraform outputs in JSON..."
                        terraform output -json > terraform_output.json
                        echo "✅ terraform_output.json created"
                        echo "******************************************************"
                        cat terraform_output.json
                        echo "******************************************************"

                        echo "📋 Parsing terraform_output.json and creating grouped Ansible hosts file..."
                        > hosts

                        # Build [all] group with everything except dns/template/haproxy/database/k8s_storage
                        echo "[all]" >> hosts
                        jq -r '
                        to_entries[]
                        | select(.key != "dns" and .key != "template_srv_ip" and .key != "haproxy_ip" and .key != "database" and .key != "k8s_storage_ip")
                        | .value.value
                        | if type=="array" then .[] else . end
                        ' terraform_output.json | grep -Eo '^[0-9.]+$' | while read ip; do
                            echo "${ip} ansible_host=${ip} ansible_user=vpsie ansible_ssh_private_key_file=/var/jenkins_home/.ssh/id_rsa" >> hosts
                        done
                        echo "" >> hosts

                        # Masters
                        echo "[masters]" >> hosts
                        jq -r '.k8s_master_ips.value[]' terraform_output.json | while read ip; do
                            echo "${ip} ansible_host=${ip} ansible_user=vpsie ansible_ssh_private_key_file=/var/jenkins_home/.ssh/id_rsa" >> hosts
                        done
                        echo "" >> hosts

                        # Workers
                        echo "[workers]" >> hosts
                        jq -r '.k8s_workers_ips.value[]' terraform_output.json | while read ip; do
                            echo "${ip} ansible_host=${ip} ansible_user=vpsie ansible_ssh_private_key_file=/var/jenkins_home/.ssh/id_rsa" >> hosts
                        done
                        echo "" >> hosts

                        # Load balancer
                        echo "[lb]" >> hosts
                        jq -r '.k8s_proxy_ip.value' terraform_output.json | while read ip; do
                            echo "${ip} ansible_host=${ip} ansible_user=vpsie ansible_ssh_private_key_file=/var/jenkins_home/.ssh/id_rsa" >> hosts
                        done
                        echo "" >> hosts

                        echo "******************************************************"
                        echo "✅ Generated Ansible hosts inventory with groups:"
                        cat hosts
                        echo "******************************************************"

                        echo "🔧 Creating ansible.cfg..."
                        cat > ansible.cfg <<EOF
        [defaults]
        host_key_checking = False
        inventory = hosts
        remote_user = root
        private_key_file = /var/jenkins_home/.ssh/id_rsa
        EOF
                        echo "✅ ansible.cfg created"
                    '''

                    // Jenkins can still access the file content if needed
                    def hostsContent = readFile('hosts')
                    writeFile file: 'hosts', text: hostsContent

                    dir('k8s-repo-setup') {
                        echo '📥 Cloning k8s repo...'
                        try {
                            checkout([$class: 'GitSCM',
                                branches: [[name: 'main']],
                                userRemoteConfigs: [[
                                    url: 'https://code.k9.ms/ahmad.abd-alkadir/k8s-new-setup.git',
                                    credentialsId: 'codek9'
                                ]]
                            ])
                            echo '✅ Git k8s repo clone successful.'
                        } catch (Exception e) {
                            error "❌ Git clone failed: ${e.message}"
                        }

                        echo '⚙️ Running K8S repo Ansible playbook...'
                        sh 'ansible-playbook -i ../hosts k8s.yml'
                        if (currentBuild.result == 'FAILURE') {
                            error '❌ K8s Ansible playbook failed. Check the logs above for details.'
                        } else {
                            echo '✅ K8s Ansible playbook completed successfully.'
                        }
                    }
                        echo '🛠️ Verifying Kubernetes cluster status...'
                    //     sh '''
                    //     echo "🛠️ Getting first master node IP..."
                    //     MASTER1=$(jq -r '.k8s_master_ips.value[0]' terraform_output.json)
                    //     echo "Master1 IP: $MASTER1"
                    //     ssh -o StrictHostKeyChecking=no vpsie@$MASTER1 "sudo kubectl get nodes --kubeconfig /etc/kubernetes/admin.conf"
                    // '''
                    sh '''#!/bin/bash
                set -euo pipefail

                echo "🛠️ Getting first master node IP..."
                MASTER1=$(jq -r '.k8s_master_ips.value[0]' terraform_output.json)
                echo "Master1 IP: $MASTER1"

                echo "📋 Checking node statuses..."
                NODE_INFO=$(ssh -o StrictHostKeyChecking=no vpsie@$MASTER1 \
                    "sudo kubectl get nodes --no-headers --kubeconfig /etc/kubernetes/admin.conf")

                echo "$NODE_INFO"

                ALL_READY=true
                while read -r name status rest; do
                    if [ "$status" != "Ready" ]; then
                        echo "❌ Node $name is NOT Ready (status=$status)"
                        ALL_READY=false
                    else
                        echo "✅ Node $name is Ready"
                    fi
                done <<< "$NODE_INFO"

                if [ "$ALL_READY" = false ]; then
                    echo "❌ Some nodes are not Ready. Failing pipeline..."
                    exit 1
                else
                    echo "✅ All nodes are Ready."
                fi
                '''
                }
            }
            post {
                success {
                    echo '🎉 Cluster is healthy. All nodes are Ready.'
                }
                failure {
                    echo '🚨 Cluster check failed. Some nodes are not Ready.'
                }
            }
        }
        stage('Configure NFS-Shared-Storage') {
            steps {
                dir('NFS-shared-storage-setup') {
                    script {
                        def k8s_storage_ip = sh(
                            script: "jq -r '.k8s_storage_ip.value' ../terraform_output.json",
                            returnStdout: true
                        ).trim()
                            sh """
                            ssh -o StrictHostKeyChecking=no vpsie@${k8s_storage_ip} '
                                echo "📁 Creating shared directory..." &&
                                sudo mkdir -p /mnt/vpsie1 &&

                                echo "📂 Changing directory ownership..." &&
                                sudo chown nobody:nogroup /mnt/vpsie1 &&

                                echo "📥 Installing NFS server packages..." &&
                                sudo apt-get update -y && sudo apt-get install -y nfs-kernel-server &&
                                echo "✅ NFS server packages installed." &&

                                echo "🔎 Checking if NFS service is running..." &&
                                if sudo systemctl is-active --quiet nfs-kernel-server; then
                                    echo "✅ NFS service is already running."
                                else
                                    echo "⚙️ Starting NFS service..." &&
                                    sudo systemctl start nfs-kernel-server &&
                                    sudo systemctl enable nfs-kernel-server &&
                                    echo "✅ NFS service started and enabled."
                                fi &&

                                echo "⚙️ Configuring NFS exports..." &&
                                LINE="/mnt/vpsie1 ${params.SUBNET}.0/24(rw,sync,no_subtree_check,no_root_squash)" &&
                                if ! grep -qxF "\$LINE" /etc/exports; then
                                    echo "\$LINE" | sudo tee -a /etc/exports > /dev/null
                                    echo "✅ Added new export entry."
                                else
                                    echo "ℹ️ Export entry already exists, skipping..."
                                fi &&

                                echo "################ /etc/exports ################" &&
                                sudo cat /etc/exports &&
                                echo "##############################################" &&

                                echo "🔄 Reloading NFS exports..." &&
                                sudo exportfs -ra &&
                                sudo systemctl restart nfs-kernel-server &&
                                echo "✅ NFS exports configured and reloaded." &&

                                echo "🔎 Verifying NFS service after reload..." &&
                                if sudo systemctl is-active --quiet nfs-kernel-server; then
                                    echo "✅ NFS service is active and running."
                                else
                                    echo "❌ NFS service is NOT running!"
                                    sudo systemctl status nfs-kernel-server
                                    exit 1
                                fi
                            '
                            """
                    }
                }
            }
            post {
                success {
                    echo '✅ Deployed NFS-Shared-Storage successfully.'
                }
                failure {
                    echo '❌ Deploy NFS-Shared-Storage failed. Check if VMs are reachable.'
                }
            }
        }
        stage('Build a Database Cluster for the platform and Restore DB Dump DB , MONGODB.') {
            steps {
                dir('db-cluster') {
                    script {
                        echo '📥 Cloning Git repo...'
                        try {
                            checkout([$class: 'GitSCM',
                                branches: [[name: 'main']],
                                userRemoteConfigs: [[
                                    url: 'https://code.k9.ms/vpsie/xtradb.git',
                                    credentialsId: 'codek9'
                                ]]
                            ])
                            echo '✅ Git Database Cluster clone successful.'
                        } catch (Exception e) {
                            error "❌ Git clone failed: ${e.message}"
                        }

                        sh """
                        set +x
                        sed -i "s|^username:.*|username: ${params.DB_USERNAME}|g" group_vars/all
                        sed -i "s|^password:.*|password: ${params.DB_PASSWORD}|g" group_vars/all
                        set -x
                        """

                        echo '📋 Generating DB hosts file...'
                        sh """
                        DB_NODES=\$(jq -r '.database.value[]' ../terraform_output.json)
                        : > hosts
                        for ip in \$DB_NODES; do
                            echo "\$ip ansible_user=${params.ciuser} ansible_ssh_private_key_file=/var/jenkins_home/.ssh/id_rsa ansible_port=22" >> hosts
                        done
                        echo "✅ Generated hosts file content:"
                        cat hosts
                        """

                        echo '⚙️ Checking MySQL status...'
                        def dbHosts = readJSON file: '../terraform_output.json'
                        def DB_HOST1 = dbHosts.database.value[0]
                        def DB_HOST2 = dbHosts.database.value[1]

                        def mysqlClientInstalled = sh(script: """
                            ssh -o StrictHostKeyChecking=no ${params.ciuser}@${DB_HOST1} "command -v mysql >/dev/null 2>&1"
                        """, returnStatus: true)

                        def mysqlStatus = sh(script: """
                            ssh -o StrictHostKeyChecking=no ${params.ciuser}@${DB_HOST2} "sudo systemctl is-active --quiet mysql"
                        """, returnStatus: true)

                        if (mysqlClientInstalled != 0 || mysqlStatus != 0) {
                            echo '⚙️ MySQL not running. Running DB Ansible playbook...'
                            sh """ ansible-playbook -i hosts play-xtraDB.yml -u ${params.ciuser} --private-key /var/jenkins_home/.ssh/id_rsa"""
                        } else {
                            echo '✅ MySQL is already running. Skipping Ansible playbook.'
                        }

                        echo '🛠️ Verifying XtraDB Cluster status...'
                        sh """
                            ssh -o StrictHostKeyChecking=no ${params.ciuser}@${DB_HOST1} "sudo mysql -N -s -e 'SHOW STATUS LIKE \\"wsrep_cluster_size\\";'"
                        """

                        echo '⚙️ Installing MongoDB...'
                        sh """ ansible-playbook -i hosts play-mongo.yml -u ${params.ciuser} --private-key /var/jenkins_home/.ssh/id_rsa"""

                        if (currentBuild.result == 'FAILURE') {
                            error '❌ MongoDB Ansible playbook failed. Check the logs above for details.'
                        } else {
                            echo '✅ MongoDB Ansible playbook completed successfully.'
                        }
                    }
                }
            }
            post {
                success {
                    echo '✅ Build a Database Cluster for the platform completed successfully.'
                }
                failure {
                    echo '❌ Build a Database Cluster for the platform failed. Check if VMs are reachable or files exist.'
                }
            }
        }
        stage('Prepare DB Dumps') {
            steps {
                script {
                    // Create folder for DB dumps
                    def dbFolder_dumps = "${env.WORKSPACE}/db"
                    sh "mkdir -p ${dbFolder_dumps}"

                    // MySQL dump
                    withFileParameter(name: 'DB_FILE', allowNoFile: true) { uploadedMySQLFile ->
                        if (uploadedMySQLFile && fileExists(uploadedMySQLFile)) {
                            sh "mv ${uploadedMySQLFile} ${dbFolder_dumps}/DB_FILE.sql"
                            echo "✅ MySQL dump moved to ${dbFolder_dumps}/DB_FILE.sql"
                        } else {
                            echo 'ℹ️ No MySQL dump file provided. Skipping.'
                        }
                    }

                    // MongoDB dump
                    withFileParameter(name: 'MONGO_DB_FILE', allowNoFile: true) { uploadedMongoFile ->
                        if (uploadedMongoFile && fileExists(uploadedMongoFile)) {
                            sh "mv ${uploadedMongoFile} ${dbFolder_dumps}/vpsie-file.archive"
                            echo "✅ MongoDB dump moved to ${dbFolder_dumps}/vpsie-file.archive"
                        } else {
                            echo 'ℹ️ No MongoDB dump file provided. Skipping.'
                        }
                    }

                    sh "ls -lh ${dbFolder_dumps}"
                }
            }
        }

        stage('Restoring default DBs (MySQL, MongoDB)') {
            steps {
                script {
                    // Read DB host(s) from Terraform output
                    def dbHosts = readJSON file: 'terraform_output.json'
                    def DB_HOST1 = dbHosts.database.value[0]

                    // --- MySQL Restore ---
                    withFileParameter(name: 'DB_FILE', allowNoFile: true) { uploadedMySQLFile ->
                        if (uploadedMySQLFile && fileExists(uploadedMySQLFile)) {
                            def dbFileName = uploadedMySQLFile.tokenize('/').last()
                            echo "📦 Found MySQL dump file: ${uploadedMySQLFile}"

                            echo '📤 Uploading MySQL dump to DB Host1...'
                            sh """
                                scp -o StrictHostKeyChecking=no ${uploadedMySQLFile} ${params.ciuser}@${DB_HOST1}:/home/${params.ciuser}/
                                ls -lh ${uploadedMySQLFile}
                            """

                            echo '⏳ Waiting 15 seconds to ensure file is fully uploaded...'
                            sleep time: 15, unit: 'SECONDS'

                            echo '📥 Creating database vpsie if it does not exist...'
                            sh """
                                ssh -o StrictHostKeyChecking=no ${params.ciuser}@${DB_HOST1} \\
                                "sudo mysql -e \\"CREATE DATABASE IF NOT EXISTS vpsie;\\""
                            """

                            echo '📥 Restoring MySQL dump on DB Host1...'
                            sh """
                                ssh -o StrictHostKeyChecking=no ${params.ciuser}@${DB_HOST1} \\
                                "mysql -u ${params.DB_USERNAME.split(',')[0] ?: params.DB_USERNAME} -p'${params.DB_PASSWORD.split(',')[0] ?: params.DB_PASSWORD}' vpsie < /home/${params.ciuser}/${dbFileName}"
                            """
                            echo "✅ MySQL dump file ${dbFileName} restored successfully."
                        } else {
                            echo 'ℹ️ No MySQL dump file provided or file not found. Skipping restore step.'
                        }
                    }

                    // --- MongoDB Restore ---
                    withFileParameter(name: 'MONGO_DB_FILE', allowNoFile: true) { uploadedMongoFile ->
                        if (uploadedMongoFile && fileExists(uploadedMongoFile)) {
                            def mongoFileName = uploadedMongoFile.tokenize('/').last()
                            echo "📦 Found MongoDB dump file: ${uploadedMongoFile}"

                            echo '📤 Uploading MongoDB dump to DB Host1...'
                            sh """
                                scp -o StrictHostKeyChecking=no ${uploadedMongoFile} ${params.ciuser}@${DB_HOST1}:/home/${params.ciuser}/
                                ls -lh ${uploadedMongoFile}
                            """

                            echo '⏳ Waiting 15 seconds to ensure MongoDB dump is fully uploaded...'
                            sleep time: 15, unit: 'SECONDS'

                            echo '📥 Restoring MongoDB dump on DB Host1...'
                            sh """
                                ssh -o StrictHostKeyChecking=no ${params.ciuser}@${DB_HOST1} \\
                                "mongorestore --username ${params.MONGO_DB_USERNAME} --password '${params.MONGO_DB_PASSWORD}' --authenticationDatabase admin --db vpsie /home/${params.ciuser}/${mongoFileName}"
                            """
                            echo "✅ MongoDB dump file ${mongoFileName} restored successfully."
                        } else {
                            echo 'ℹ️ No MongoDB dump file provided or file not found. Skipping restore step.'
                        }
                    }
                }
            }
        }

        stage('Build and Configure Database On DNS Servers for the platform.') {
            steps {
                dir('db-cluster-for-dns-vms') {
                    script {
                        echo '📥 Cloning Git repo...'
                        try {
                            checkout([
                                $class: 'GitSCM',
                                branches: [[name: 'main']],
                                userRemoteConfigs: [[
                                    url: 'https://code.k9.ms/vpsie/xtradb.git',
                                    credentialsId: 'codek9'
                                ]]
                            ])
                            echo '✅ Git clone successful.'
                        } catch (Exception e) {
                            error "❌ Git clone failed: ${e.message}"
                        }
                        sh """
                        DNS_NODES=\$(jq -r '.dns.value[]' ../terraform_output.json)
                        : > hosts
                        for ip in \$DNS_NODES; do
                            echo "\$ip ansible_user=${params.ciuser} ansible_ssh_private_key_file=/var/jenkins_home/.ssh/id_rsa ansible_port=22" >> hosts
                        done
                        echo "✅ Generated hosts file content:"
                        cat hosts
                        """
                        echo '#################### DB REPO FOR DNS VMS  #############################'

                        // def DnsHosts     = readJSON file: '../terraform_output.json'
                        // def DNS_HOST1_IP = DnsHosts.dns.value[0]
                        // def DNS_HOST2_IP = DnsHosts.dns.value[1]
                        // def DNS_HOST3_IP = DnsHosts.dns.value[2]

                        echo '⚙️ Running DB Ansible playbook For DNS VMS...'
                        sh """ansible-playbook -i hosts play-xtraDB.yml -u ${params.ciuser} --private-key /var/jenkins_home/.ssh/id_rsa"""
                    }
                }
            }
            post {
                success {
                    echo '✅ DB On DNS Servers for the platform completed successfully.'
                }
                failure {
                    echo '❌ DB On DNS Servers for the platform failed. Check if VMs are reachable.'
                }
            }
        }

        stage('Build and Configure DNS Servers for the platform.') {
            steps {
                dir('Dns-setup') {
                    script {
                        echo '📥 Cloning Git repo...'
                        try {
                            checkout([$class: 'GitSCM',
                                branches: [[name: 'main']],
                                userRemoteConfigs: [[
                                    url: 'https://code.k9.ms/vpsie/xtradb.git',
                                    credentialsId: 'codek9'
                                ]]
                            ])
                            echo '✅ Git clone successful.'
                        } catch (Exception e) {
                            error "❌ Git clone failed: ${e.message}"
                        }

                        echo '#################### DNS REPO FOR DNS VMS #############################'
                        echo '📋 Generating DNS hosts file...'

                        // ✅ Use bash explicitly to avoid "Bad substitution" and ensure loop works
                        sh """
                        # Extract DNS IPs from terraform output
                        DNS_NODES=\$(jq -r '.dns.value[]' ../terraform_output.json)

                        # Start fresh hosts file
                        : > hosts

                        i=1
                        for ip in \$DNS_NODES; do
                            echo "\$ip ansible_user=${params.ciuser} ansible_port=22" >> hosts
                            i=\$(expr \$i + 1)
                        done

                        echo "✅ Generated hosts file content:"
                        cat hosts
                        """

                        echo '#################### DB REPO FOR DNS VMS #############################'
                        def DnsHosts = readJSON file: '../terraform_output.json'
                        def DNS_HOST2_IP = DnsHosts.dns.value[1]
                        def DNS_HOST3_IP = DnsHosts.dns.value[2]

                        echo '⚙️ Running DNS Ansible playbook...'
                        sh "ansible-playbook -i hosts play-pdns.yml -e apikey=${params.apikey} -u ${params.ciuser} --private-key /var/jenkins_home/.ssh/id_rsa"

                        if (currentBuild.result == 'FAILURE') {
                            error '❌ DNS Ansible playbook failed. Check the logs above for details.'
                        } else {
                            echo '✅ DNS Ansible playbook completed successfully.'
                        }

                        echo '🛠️ Waiting for DNS service on port 8081...'
                        sleep 90

                        sh """
                            ssh -o StrictHostKeyChecking=no ${params.ciuser}@${DNS_HOST3_IP} bash -c '
                            status=\$(sudo curl -o /dev/null -s -w "%{http_code}" http://${DNS_HOST2_IP}:8081)
                            echo "✅ DNS service check completed on ${DNS_HOST2_IP} with status code = \$status"
                            '
                        """

                        echo '✅ DNS Servers for the platform are ready.'
                    }
                }
            }
            post {
                success {
                    echo '✅ Build and Configure DNS Servers for the platform completed successfully.'
                }
                failure {
                    echo '❌ Build and Configure DNS Servers for the platform failed. Check if VMs are reachable.'
                }
            }
        }

        stage('Deploy NFS Dynamic Provisioner') {
            steps {
                dir('Nfs-dynamic-provisioner') {
                    script {
                        // Extract storage IP
                        def k8s_storage_ip = sh(
                            script: "jq -r '.k8s_storage_ip.value' ../terraform_output.json",
                            returnStdout: true
                        ).trim()

                        // Extract Master1 IP
                        def k8sHosts = readJSON file: '../terraform_output.json'
                        def MASTER1_IP = k8sHosts.k8s_master_ips.value[0]

                        echo "🚀 Installing NFS Dynamic Provisioner on Master1: ${MASTER1_IP}, Storage IP: ${k8s_storage_ip}"

                        sh """
                        ssh -o StrictHostKeyChecking=no ${params.ciuser}@${MASTER1_IP} /bin/bash <<EOF
                            set -euo pipefail

                            # Ensure kubectl can access the cluster
                            sudo mkdir -p /home/${params.ciuser}/.kube
                            sudo cp /etc/kubernetes/admin.conf /home/${params.ciuser}/.kube/config
                            sudo chown ${params.ciuser}:${params.ciuser} /home/${params.ciuser}/.kube/config

                            echo "🧹 Checking for existing NFS Provisioner..."
                            if helm status nfs -n kube-system >/dev/null 2>&1; then
                                if kubectl get pods -n kube-system -l app=nfs-subdir-external-provisioner \\
                                    -o jsonpath='{.items[*].status.phase}' | grep -q "Running"; then
                                    echo "✅ NFS Provisioner is already running — skipping reinstall."
                                    exit 0
                                else
                                    echo "⚠️ NFS release exists but pods are not running — reinstalling..."
                                    helm uninstall nfs -n kube-system || true
                                    sleep 60
                                    echo "🧹 Cleanup completed in kube-system namespace."
                                fi
                            else
                                echo "✅ No old NFS Provisioner release found — proceeding with fresh install."
                            fi

                            echo "⚙️ Installing Helm..."
                            curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
                            chmod 700 get_helm.sh
                            ./get_helm.sh

                            echo "📦 Adding NFS Provisioner repo..."
                            helm repo add nfs-provisioner https://kubernetes-sigs.github.io/nfs-subdir-external-provisioner/
                            helm repo update

                            echo "📦 Fetching Helm values..."
                            helm show values nfs-provisioner/nfs-subdir-external-provisioner > values.yml

                            echo "📝 Updating Helm values with sed............................"
                            sed -i 's|path:.*|path: /mnt/vpsie1|' values.yml
                            sed -i 's|server:.*|server: ${k8s_storage_ip}|' values.yml
                            sed -i 's|defaultClass:.*|defaultClass: true|' values.yml
                            sed -i 's|accessModes:.*|accessModes: ReadWriteMany|' values.yml
                            sed -i 's|name:.*|name: nfs|' values.yml
                            sed -i 's|pathPattern:.*|pathPattern: \\\${.PVC.namespace}/\\\${.PVC.name}|' values.yml

                            echo "🚀 Deploying fresh NFS Provisioner..."
                            helm install nfs nfs-provisioner/nfs-subdir-external-provisioner -f values.yml -n kube-system

                            echo "🔎 Waiting for NFS provisioner pod..."
                            kubectl rollout status deployment -n kube-system -l app=nfs-subdir-external-provisioner --timeout=300s
                        EOF
                        """
                    }
                }
            }
            post {
                success {
                    echo '✅ Deployed NFS Dynamic Provisioner successfully.'
                }
                failure {
                    echo '❌ Failed to deploy NFS Dynamic Provisioner.'
                }
            }
        }
        }
    }
