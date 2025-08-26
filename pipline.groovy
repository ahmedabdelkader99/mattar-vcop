pipeline {
    agent any

    environment {
        TF_VAR_px_user     = credentials('proxmox-user')
        TF_VAR_px_password = credentials('proxmox-password')
    }

    parameters {
        string(name: 'PX_ENDPOINT', defaultValue: 'https://10.116.21.71:8006/api2/json', description: 'Proxmox API endpoint')
        string(name: 'PX_NODE', defaultValue: 's1vpsie01', description: 'Target Proxmox node')
        booleanParam(name: 'PX_TLS', defaultValue: true, description: 'Enable TLS for Proxmox API')

        string(name: 'MASTER_COUNT', defaultValue: '3', description: 'Number of Kubernetes master nodes')
        string(name: 'MASTER_MEM', defaultValue: '4096', description: 'Memory (MB) for each master node')
        string(name: 'MASTER_CORES', defaultValue: '2', description: 'CPU cores for each master node')

        string(name: 'WORKER_COUNT', defaultValue: '3', description: 'Number of Kubernetes worker nodes')
        string(name: 'WORKER_MEM', defaultValue: '4096', description: 'Memory (MB) for each worker node')
        string(name: 'WORKER_CORES', defaultValue: '2', description: 'CPU cores for each worker node')

        string(name: 'DB_COUNT', defaultValue: '3', description: 'Number of DB VMs')
        string(name: 'DB_MEM', defaultValue: '4096', description: 'Memory (MB) for each DB node')
        string(name: 'DB_CORES', defaultValue: '2', description: 'CPU cores for each DB node')
        string(name: 'DB_START_IP', defaultValue: '201', description: 'Starting IP for DB nodes')

        string(name: 'K8S_PROXY_IP', defaultValue: '196', description: 'IP for K8s Proxy/load balancer')
        string(name: 'K8S_STORAGE_IP', defaultValue: '199', description: 'IP for K8s Storage/load balancer')
        string(name: 'K8S_START_IP', defaultValue: '190', description: 'Starting IP for Kubernetes nodes')

        string(name: 'DNS_START_IP', defaultValue: '204', description: 'Starting IP for DNS nodes')
        string(name: 'PROXY_IP', defaultValue: '198', description: 'IP for Proxy/load balancer')
        string(name: 'TEMPLATES_SRV_IP', defaultValue: '111', description: 'IP for Templates Server')
        string(name: 'BACKUP_SRV_IP', defaultValue: '112', description: 'IP for Backup Server')

        string(name: 'CLONE_TEMPLATE', defaultValue: 'ci001', description: 'Base VM/template to clone')
        string(name: 'DEFAULT_STORAGE', defaultValue: 'local', description: 'Proxmox storage location for VM disks')
        string(name: 'AGENT', defaultValue: '1', description: 'Enable QEMU guest agent (1=enabled, 0=disabled)')
        string(name: 'OSTYPE', defaultValue: 'cloud-init', description: 'OS type for the VM')
        string(name: 'SCSI_HW', defaultValue: 'virtio-scsi-pci', description: 'SCSI controller type')
        string(name: 'PREFIX', defaultValue: 'test-', description: 'Prefix for VM names')
        string(name: 'DISK_SIZE', defaultValue: '40', description: 'Disk size (GB) per VM')
        string(name: 'SUBNET', defaultValue: '11.1.1', description: 'Subnet for VMs')
        string(name: 'GATEWAY', defaultValue: '11.1.1.1', description: 'Gateway IP for the subnet')
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/ahmedabdelkader99/mattar-vcop'
            }
        }

        stage('Check Proxmox Login') {
            steps {
                sh """
                    echo "🔑 Checking Proxmox login for user: \$TF_VAR_px_user"
                    LOGIN_RESPONSE=\$(curl -sk -d "username=\$TF_VAR_px_user&password=\$TF_VAR_px_password" "\$PX_ENDPOINT" || true)

                    if echo "\$LOGIN_RESPONSE"; then
                        echo "✅ Login succeeded to Proxmox API \$PX_ENDPOINT"
                    else
                        echo "❌ Login FAILED to Proxmox API"
                        echo "Response: \$LOGIN_RESPONSE"
                        exit 1
                    fi
                """
            }
        }

        stage('Update tfvars') {
            steps {
                script {
                    sh """
                        sed -i 's|^px_endpoint.*|px_endpoint    = "${PX_ENDPOINT}"|' terraform.tfvars
                        sed -i 's|^px_user.*|px_user        = "${TF_VAR_px_user}"|' terraform.tfvars
                        sed -i 's|^px_password.*|px_password    = "${TF_VAR_px_password}"|' terraform.tfvars
                        sed -i 's|^pxTargetNode.*|pxTargetNode   = "${PX_NODE}"|' terraform.tfvars
                        sed -i 's|^px_tls.*|px_tls         = ${PX_TLS}|' terraform.tfvars

                        sed -i 's|^masterCount.*|masterCount    = ${MASTER_COUNT}|' terraform.tfvars
                        sed -i 's|^workersCount.*|workersCount   = ${WORKER_COUNT}|' terraform.tfvars
                        sed -i 's|^masterMem.*|masterMem      = ${MASTER_MEM}|' terraform.tfvars
                        sed -i 's|^masterCores.*|masterCores    = ${MASTER_CORES}|' terraform.tfvars
                        sed -i 's|^workerMem.*|workerMem      = ${WORKER_MEM}|' terraform.tfvars
                        sed -i 's|^workerCores.*|workerCores    = ${WORKER_CORES}|' terraform.tfvars

                        sed -i 's|^dbCount.*|dbCount        = ${DB_COUNT}|' terraform.tfvars
                        sed -i 's|^dbMem.*|dbMem          = ${DB_MEM}|' terraform.tfvars
                        sed -i 's|^dbCores.*|dbCores        = ${DB_CORES}|' terraform.tfvars
                        sed -i 's|^dbStartIP.*|dbStartIP      = ${DB_START_IP}|' terraform.tfvars
                        sed -i 's|^k8sProxyIP.*|k8sProxyIP     = ${K8S_PROXY_IP}|' terraform.tfvars
                        sed -i 's|^k8sStorageIP.*|k8sStorageIP   = ${K8S_STORAGE_IP}|' terraform.tfvars
                        sed -i 's|^dnsStartIP.*|dnsStartIP     = ${DNS_START_IP}|' terraform.tfvars
                        sed -i 's|^proxyIP.*|proxyIP        = ${PROXY_IP}|' terraform.tfvars
                        sed -i 's|^templatesSrvIP.*|templatesSrvIP  = ${TEMPLATES_SRV_IP}|' terraform.tfvars
                        sed -i 's|^backupSrvIP.*|backupSrvIP     = ${BACKUP_SRV_IP}|' terraform.tfvars

                        sed -i 's|^clone.*|clone          = "${CLONE_TEMPLATE}"|' terraform.tfvars
                        sed -i 's|^defaultStorage.*|defaultStorage = "${DEFAULT_STORAGE}"|' terraform.tfvars
                        sed -i 's|^agent.*|agent          = ${AGENT}|' terraform.tfvars
                        sed -i 's|^osType.*|osType         = "${OSTYPE}"|' terraform.tfvars
                        sed -i 's|^scsihw.*|scsihw        = "${SCSI_HW}"|' terraform.tfvars
                        sed -i 's|^prefix.*|prefix         = "${PREFIX}"|' terraform.tfvars
                        sed -i 's|^diskSize.*|diskSize       = ${DISK_SIZE}|' terraform.tfvars

                        sed -i 's|^subnet.*|subnet         = "${SUBNET}"|' terraform.tfvars
                        sed -i 's|^gateway.*|gateway        = "${GATEWAY}"|' terraform.tfvars
                        sed -i 's|^k8sStartIP.*|k8sStartIP     = ${K8S_START_IP}|' terraform.tfvars

                        echo "Updated terraform.tfvars:"
                        cat terraform.tfvars
                    """
                }
            }
        }

        stage('Terraform Init') {
            steps { sh 'terraform init' }
        }

        stage('Terraform Plan') {
            steps { sh 'terraform plan' }
        }

        stage('Terraform Apply') {
            steps { sh 'terraform apply -auto-approve' }
        }
    }
}
