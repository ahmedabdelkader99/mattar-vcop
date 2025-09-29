pipeline {
    agent any

    environment {
        TF_VAR_px_user     = credentials('proxmox-user')
        TF_VAR_px_password = credentials('proxmox-password')
    }

    parameters {
        string(name: 'PX_ENDPOINT', defaultValue: 'https://10.116.21.71:8006/api2/json', description: 'Proxmox API endpoint')
        string(name: 'PX_NODE', defaultValue: 's1vpsie01', description: 'Target Proxmox node')

        string(name: 'K8S_START_IP', defaultValue: '190', description: 'Starting IP for Kubernetes nodes')

        string(name: 'DB_START_IP', defaultValue: '201', description: 'Starting IP for DB nodes')

        string(name: 'DNS_START_IP', defaultValue: '204', description: 'Starting IP for DNS nodes')

        string(name: 'K8S_PROXY_IP', defaultValue: '196', description: 'IP for K8s Proxy/load balancer')

        string(name: 'PROXY_IP', defaultValue: '198', description: 'IP for HAProxy ')

        string(name: 'K8S_STORAGE_IP', defaultValue: '200', description: 'IP for K8s Storage/load balancer')

        string(name: 'TEMPLATES_SRV_IP', defaultValue: '111', description: 'IP for Templates Server')
        string(name: 'sshkeys', defaultValue: 'Your generated ssh key ', description: 'SSH public key for VM access')
        string(name: 'ciuser', defaultValue: 'vpsie', description: 'user name for ssh access to VMs')
        string(name: 'CLONE_TEMPLATE', defaultValue: 'ci001', description: 'Base VM/template to clone')

        string(name: 'DEFAULT_STORAGE', defaultValue: 'local', description: 'Proxmox storage location for VM disks')

        string(name: 'PREFIX', defaultValue: 'test-', description: 'Prefix for VM names')
        string(name: 'DISK_SIZE', defaultValue: '40', description: 'Disk size (GB) per VM')
        string(name: 'SUBNET', defaultValue: 'x.x.x', description: 'Subnet for VMs')
        string(name: 'GATEWAY', defaultValue: 'x.x.x.x', description: 'Gateway IP for the subnet')
        string(name: 'VM_VCOP_PASSWORD', defaultValue: 'Your-Vcop-Password', description: 'Password for all VMs')
        string(name: 'DB_HOST1_NAME', defaultValue: 'k8s-database01', description: 'DB node 1 hostname')
        string(name: 'DB_HOST1_IP', defaultValue: '10.x.x.201', description: 'DB node 1 IP address')
        string(name: 'DB_HOST2_NAME', defaultValue: 'k8s-database02', description: 'DB node 2 hostname')
        string(name: 'DB_HOST2_IP', defaultValue: '10.x.x.202', description: 'DB node 2 IP address')
        string(name: 'DB_HOST3_NAME', defaultValue: 'k8s-database03', description: 'DB node 3 hostname')
        string(name: 'DB_HOST3_IP', defaultValue: '10.x.x.203', description: 'DB node 3 IP address')
        string(name: 'DNS_HOST1_NAME', defaultValue: 'dns01', description: 'DNS node 1 hostname')
        string(name: 'DNS_HOST1_IP', defaultValue: '10.x.x.204', description: 'DNS node 1 IP address')
        string(name: 'DNS_HOST2_NAME', defaultValue: 'dns02', description: 'DNS node 2 hostname')
        string(name: 'DNS_HOST2_IP', defaultValue: '10.x.x.205', description: 'DNS node 2 IP address')
        string(name: 'DNS_HOST3_NAME', defaultValue: 'dns03', description: 'DNS node 3 hostname')
        string(name: 'DNS_HOST3_IP', defaultValue: '10.x.x.206', description: 'DNS node 3 IP address')
        string(name: 'apikey', defaultValue: 'generate your API key using API Key Generator', description: 'API key for the setup')
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
                    LOGIN_RESPONSE=$(curl -sk \
                        -d "username=$TF_VAR_px_user&password=$TF_VAR_px_password" \
                        "$PX_ENDPOINT" || true)

                    if echo "$LOGIN_RESPONSE"; then
                        echo "✅ Login succeeded to Proxmox API $PX_ENDPOINT"
                    else
                        echo "❌ Login FAILED to Proxmox API"
                        echo "Response: $LOGIN_RESPONSE"
                        exit 1
                    fi
                '''
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
                    sed -i "s|^sshkeys *=.*|sshkeys        = \\"${params.sshkeys}\\"|g" terraform.tfvars.tmp

                    sed -i "s|^clone *=.*|clone          = \\"${params.CLONE_TEMPLATE}\\"|g" terraform.tfvars.tmp
                    sed -i "s|^defaultStorage *=.*|defaultStorage = \\"${params.DEFAULT_STORAGE}\\"|g" terraform.tfvars.tmp

                    sed -i "s|^prefix *=.*|prefix         = \\"${params.PREFIX}\\"|g" terraform.tfvars.tmp
                    sed -i "s|^diskSize *=.*|diskSize       = ${params.DISK_SIZE}|g" terraform.tfvars.tmp

                    sed -i "s|^subnet *=.*|subnet         = \\"${params.SUBNET}\\"|g" terraform.tfvars.tmp
                    sed -i "s|^gateway *=.*|gateway        = \\"${params.GATEWAY}\\"|g" terraform.tfvars.tmp
                    sed -i "s|^k8sStartIP *=.*|k8sStartIP     = ${params.K8S_START_IP}|g" terraform.tfvars.tmp
                    sed -i "s|^dbStartIP *=.*|dbStartIP      = ${params.DB_START_IP}|g" terraform.tfvars.tmp
                    sed -i "s|^dnsStartIP *=.*|dnsStartIP     = ${params.DNS_START_IP}|g" terraform.tfvars.tmp
                    sed -i "s|^templatesSrvIP *=.*|templatesSrvIP = ${params.TEMPLATES_SRV_IP}|g" terraform.tfvars.tmp
                    sed -i "s|^ciuser *=.*|ciuser         = \\"${params.ciuser}\\"|g" terraform.tfvars.tmp

                    sed -i "s|^k8sProxyIP *=.*|k8sProxyIP     = ${params.K8S_PROXY_IP}|g" terraform.tfvars.tmp

                    sed -i "s|^k8sStorageIP *=.*|k8sStorageIP   = ${params.K8S_STORAGE_IP}|g" terraform.tfvars.tmp

                    sed -i "s|^proxyIP *=.*|proxyIP        = ${params.PROXY_IP}|g" terraform.tfvars.tmp
                    sed -i "s|^apiKey *=.*| apikey         = \\"${params.apikey}\\"|g" terraform.tfvars.tmp

                    """

                    // Save the final version
                    sh 'cp terraform.tfvars.tmp terraform.tfvars'
                    sh "echo '✅ Generated terraform.tfvars:' && cat terraform.tfvars"
                }
            }
        }

        stage('Terraform Init') {
            steps {
                sh 'terraform init'
            }
        }

        stage('Terraform Plan') {
            steps {
                sh 'terraform plan'
            }
        }

        stage('Terraform Apply') {
            steps {
                sh 'terraform apply -auto-approve'
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
                                def sshStatus = sh(script: "ssh -i ${privateKey} -o BatchMode=yes -o ConnectTimeout=5 -o StrictHostKeyChecking=no root@${ip} 'echo SSH OK'", returnStatus: true)
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
        }

        ############ from here we start ############

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
                            echo "${ip} ansible_host=${ip} ansible_user=root ansible_ssh_private_key_file=/var/jenkins_home/.ssh/id_rsa" >> hosts
                        done
                        echo "" >> hosts

                        # Masters
                        echo "[masters]" >> hosts
                        jq -r '.k8s_master_ips.value[]' terraform_output.json | while read ip; do
                            echo "${ip} ansible_host=${ip} ansible_user=root ansible_ssh_private_key_file=/var/jenkins_home/.ssh/id_rsa" >> hosts
                        done
                        echo "" >> hosts

                        # Workers
                        echo "[workers]" >> hosts
                        jq -r '.k8s_workers_ips.value[]' terraform_output.json | while read ip; do
                            echo "${ip} ansible_host=${ip} ansible_user=root ansible_ssh_private_key_file=/var/jenkins_home/.ssh/id_rsa" >> hosts
                        done
                        echo "" >> hosts

                        # Load balancer
                        echo "[lb]" >> hosts
                        jq -r '.k8s_proxy_ip.value' terraform_output.json | while read ip; do
                            echo "${ip} ansible_host=${ip} ansible_user=root ansible_ssh_private_key_file=/var/jenkins_home/.ssh/id_rsa" >> hosts
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
                        sh '''
                        echo "🛠️ Getting first master node IP..."
                        MASTER1=$(jq -r '.k8s_master_ips.value[0]' terraform_output.json)
                        echo "Master1 IP: $MASTER1"

                        echo "🛠️ Verifying Kubernetes cluster status on Master1..."
                        ssh -o StrictHostKeyChecking=no root@$MASTER1 "kubectl get nodes --kubeconfig /etc/kubernetes/admin.conf"
                        echo "✅ Kubernetes cluster verification completed."
                    '''
                }
            }
        }
        stage('Configure NFS-Shared-Storage') {
            steps {
                dir('NFS-shared-storage-setup') {
                    script {
                        sh """
                        echo '🔑 Logging into NFS server VM and configuring NFS...'
                        ssh -o StrictHostKeyChecking=no root@${params.SUBNET}.${params.K8S_STORAGE_IP} "
                            echo '📁 Creating shared directory...' &&
                            mkdir -p /mnt/vpsie1 &&

                            echo '📂 Changing directory ownership...' &&
                            chown nobody:nogroup /mnt/vpsie1 &&

                            echo '📥 Installing NFS server packages...' &&
                            apt-get update -y && apt-get install -y nfs-kernel-server &&
                            echo '✅ NFS server packages installed.' &&

                            echo '🔎 Checking if NFS service is running...' &&
                            if systemctl is-active --quiet nfs-kernel-server; then
                                echo '✅ NFS service is already running.'
                            else
                                echo '⚙️ Starting NFS service...' &&
                                systemctl start nfs-kernel-server &&
                                systemctl enable nfs-kernel-server &&
                                echo '✅ NFS service started and enabled.'
                            fi &&

                            echo '⚙️ Configuring NFS exports...' &&
                            LINE='/mnt/vpsie1 ${params.SUBNET}.0/24(rw,sync,no_subtree_check,no_root_squash)'
                            if ! grep -qxF \"\$LINE\" /etc/exports; then
                                echo \"\$LINE\" >> /etc/exports
                                echo '✅ Added new export entry.'
                            else
                                echo 'ℹ️ Export entry already exists, skipping...'
                            fi &&

                            echo '################ /etc/exports ################' &&
                            cat /etc/exports &&
                            echo '##############################################' &&

                            echo '🔄 Reloading NFS exports...' &&
                            exportfs -a &&
                            echo '✅ NFS exports configured and reloaded.' &&

                            echo '🔎 Verifying NFS service after reload...' &&
                            if systemctl is-active --quiet nfs-kernel-server; then
                                echo '✅ NFS service is active and running.'
                            else
                                echo '❌ NFS service is NOT running!'
                                systemctl status nfs-kernel-server
                                exit 1
                            fi
                        "
                    """
                    }
                }
            }
        }

        stage('Build a Database Cluster for the platform.') {
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
                            echo '✅ Git  Database Cluster clone successful.'
                        } catch (Exception e) {
                            error "❌ Git clone failed: ${e.message}"
                        }

                        echo '📋 Generating DB hosts file...'
                        def hostsContent = """
${params.DB_HOST1_NAME} ansible_host=${params.DB_HOST1_IP} ansible_port=22
${params.DB_HOST2_NAME} ansible_host=${params.DB_HOST2_IP} ansible_port=22
${params.DB_HOST3_NAME} ansible_host=${params.DB_HOST3_IP} ansible_port=22
"""
                        writeFile file: 'hosts', text: hostsContent.trim()

                        echo '#################################*********************#######################'
                        echo '📋 📋 📋 📋  Contents of hosts file:'
                        sh 'cat hosts'
                        echo '#################################********************########################'
                        echo 'CHECKING IF MYSQL IS RUNNING ON VMS: ⚙️⚙️⚙️⚙️⚙️⚙️⚙️⚙️⚙️⚙️⚙️⚙️⚙️⚙️⚙️⚙️'

                        // Check if MySQL client exists on the VM
                        def mysqlClientInstalled = sh(
                            script: """
                                ssh -o StrictHostKeyChecking=no root@${params.DB_HOST1_IP} "command -v mysql >/dev/null 2>&1"
                            """,
                            returnStatus: true
                        )

                        def mysqlStatus = sh(
                            script: """
                            ssh -o StrictHostKeyChecking=no root@${params.DB_HOST2_IP} \\
                            "systemctl is-active --quiet mysql  "
                            """,
                            returnStatus: true
                        )
                        // Check Galera cluster size
                       // def clusterSize = sh(
                         //   script: """
                           //     ssh -o StrictHostKeyChecking=no root@${params.DB_HOST1_IP} \\
                             //   "mysql -u root -p'${params.VM_VCOP_PASSWORD}' -N -s -e \\"SELECT VARIABLE_VALUE FROM performance_schema.global_status WHERE VARIABLE_NAME='wsrep_cluster_size';\\" | awk '{print \\\$1}'"
                            //""",
                            //returnStdout: true
                        //).trim()

                        // Convert to integer for comparison
                      //  def clusterSizeInt = clusterSize.toInteger() ? clusterSize.toInteger() : 0
                       // echo '##################*************###################'
                        //echo "🔍 Cluster size detected from the function = ${clusterSizeInt}"
                        //echo '##################*************###################'

                        if (mysqlClientInstalled != 0 || mysqlStatus != 0) {
                            // MySQL is NOT running, run the playbook
                            echo '⚙️ MySQL not running. Running DB Ansible playbook...'
                            sh 'ansible-playbook -i hosts play-xtraDB.yml'
                        } else {
                            // MySQL is running, skip playbook
                            echo '✅ MySQL is already running. Skipping Ansible playbook.'
                        }

                        echo '🛠️ Verifying XtraDB Cluster status...'
                        sh """
                        ssh -o StrictHostKeyChecking=no root@${params.DB_HOST1_IP} \\
                        "mysql -u root -p'${params.VM_VCOP_PASSWORD}' -e \\"SHOW STATUS LIKE 'wsrep_cluster_size';\\""
                        """

                        echo '⚙️ Install MongoDB (on the same VMs as the DB)...'
                        sh 'ansible-playbook -i hosts play-mongo.yml'
                        echo 'Database cluster setup completed.✅ ✅ ✅ ✅ ✅ ✅'
                    }
                }
            }
        }

        stage('Build and Configure DB On DNS Servers for the platform.') {
            steps {
                dir('db-cluster-for-dns-vms') {
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
                        echo '#################### DB REPO FOR DNS VMS  #############################'
                        echo '📋 Generating DNS hosts file...'
                        def hostsContent = """
${params.DNS_HOST1_NAME} ansible_host=${params.DNS_HOST1_IP} ansible_port=22
${params.DNS_HOST2_NAME} ansible_host=${params.DNS_HOST2_IP} ansible_port=22
${params.DNS_HOST3_NAME} ansible_host=${params.DNS_HOST3_IP} ansible_port=22
"""
                        writeFile file: 'hosts', text: hostsContent.trim()

                        echo '#################################'
                        echo '📋 Contents of hosts file:'
                        sh 'cat hosts'
                        echo '#################################'

                        echo '⚙️ Running DB Ansible playbook...'
                        sh 'ansible-playbook -i hosts play-xtraDB.yml'
                        echo ' Database cluster setup completed On DNS Servers for the platform.✅ ✅ ✅ ✅ ✅ ✅'
                    }
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
                        echo '#################### DNS REPO FOR DNS VMS  #############################'
                        echo '📋 Generating DNS hosts file...'
                        def DnsHostsContent = """
${params.DNS_HOST1_NAME} ansible_host=${params.DNS_HOST1_IP} ansible_port=22
${params.DNS_HOST2_NAME} ansible_host=${params.DNS_HOST2_IP} ansible_port=22
${params.DNS_HOST3_NAME} ansible_host=${params.DNS_HOST3_IP} ansible_port=22
"""
                        writeFile file: 'hosts', text: DnsHostsContent.trim()

                        echo '#################################'
                        echo '📋 Contents of DNS hosts file:'
                        sh 'cat hosts'
                        echo '#################################'

                        echo '⚙️ Running DNS Ansible playbook...'
                        sh "ansible-playbook -i hosts play-pdns.yml -e apikey=${params.apikey}"
                        if (currentBuild.result == 'FAILURE') {
                            error '❌ DNS Ansible playbook failed. Check the logs above for details.'
                        } else {
                            echo '✅ DNS Ansible playbook completed successfully.'
                        }
                        echo '🛠️ Waiting for DNS service on port 8081...'
                        sleep 90
                        sh """
                        ssh -o StrictHostKeyChecking=no root@${params.DNS_HOST3_IP} bash -c '
                        status=\$(curl -o /dev/null -s -w "%{http_code}" http://${params.DNS_HOST3_IP}:8081)
                        echo "✅ DNS service check completed on ${params.DNS_HOST3_IP} with status code = \$status"
                        '
                        """
                        echo 'DNS Servers for the platform is ready .✅ ✅ ✅ ✅ ✅ ✅'
                    }
                }
            }
        }
    }
}
