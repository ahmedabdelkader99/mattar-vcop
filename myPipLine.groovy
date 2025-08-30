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
        string(name: 'K8S_START_IP', defaultValue: '190', description: 'Starting IP for Kubernetes nodes')

        string(name: 'DB_COUNT', defaultValue: '3', description: 'Number of DB VMs')
        string(name: 'DB_MEM', defaultValue: '4096', description: 'Memory (MB) for each DB node')
        string(name: 'DB_CORES', defaultValue: '2', description: 'CPU cores for each DB node')
        string(name: 'DB_START_IP', defaultValue: '201', description: 'Starting IP for DB nodes')

        string(name: 'DNS_COUNT', defaultValue: '3', description: 'Number of DNS VMs')
        string(name: 'DNS_MEM', defaultValue: '4096', description: 'Memory (MB) for each DNS node')
        string(name: 'DNS_CORES', defaultValue: '2', description: 'CPU cores for each DNS node')
        string(name: 'DNS_START_IP', defaultValue: '204', description: 'Starting IP for DNS nodes')

        string(name: 'k8sproxy_Mem', defaultValue: '2048', description: 'Memory for K8s Proxy/load balancer')
        string(name: 'k8sproxy_Cores', defaultValue: '2', description: 'CPU cores for K8s Proxy/load balancer')
        string(name: 'K8S_PROXY_IP', defaultValue: '169', description: 'IP for K8s Proxy/load balancer')  

        string(name: 'Haproxy_Mem', defaultValue: '2048', description: 'Memory for HAProxy ')
        string(name: 'Haproxy_Cores', defaultValue: '2', description: 'CPU cores for HAProxy ')
        string(name: 'PROXY_IP', defaultValue: '198', description: 'IP for HAProxy ')

        string(name: 'k8storageMem', defaultValue: '2048', description: 'Memory for K8s Storage')
        string(name: 'k8storageCores', defaultValue: '2', description: 'CPU cores for K8s Storage')
        string(name: 'K8S_STORAGE_IP', defaultValue: '199', description: 'IP for K8s Storage/load balancer')

        string(name: 'TEMPLATES_SRV_IP', defaultValue: '111', description: 'IP for Templates Server')
        string(name: 'BACKUP_SRV_IP', defaultValue: '112', description: 'IP for Backup Server')

        string(name: 'CLONE_TEMPLATE', defaultValue: 'ci001', description: 'Base VM/template to clone')
        string(name: 'Temp_Mem', defaultValue: '2048', description: 'Memory (MB) for the template VM')
        string(name: 'Temp_Cores', defaultValue: '2', description: 'CPU cores for the template VM')
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

        stage('Generate tfvars') {
            steps {
                script {
                    def tfvarsContent = """
px_endpoint   = "${params.PX_ENDPOINT}"
pxTargetNode  = "${params.PX_NODE}"
px_tls        = ${params.PX_TLS}

clone          = "${params.CLONE_TEMPLATE}"
defaultStorage = "${params.DEFAULT_STORAGE}"
agent          = ${params.AGENT}
osType         = "${params.OSTYPE}"
scsihw         = "${params.SCSI_HW}"
prefix         = "${params.PREFIX}"
diskSize       = ${params.DISK_SIZE}

subnet         = "${params.SUBNET}"
gateway        = "${params.GATEWAY}"
cidr           = 24
tag            = 1
k8sStartIP     = ${params.K8S_START_IP}
dbStartIP      = ${params.DB_START_IP}
dnsStartIP     = ${params.DNS_START_IP}
templatesSrvIP = ${params.TEMPLATES_SRV_IP}
backupSrvIP    = ${params.BACKUP_SRV_IP}
sshkeys        = ""
ciuser         = "vpsie"

masterCount    = ${params.MASTER_COUNT}
masterMem      = ${params.MASTER_MEM}
masterCores    = ${params.MASTER_CORES}

workersCount   = ${params.WORKER_COUNT}
workerMem      = ${params.WORKER_MEM}
workerCores    = ${params.WORKER_CORES}

dbCount        = ${params.DB_COUNT}
dbMem          = ${params.DB_MEM}
dbCores        = ${params.DB_CORES}

dnsCount       = ${params.DNS_COUNT}
dnsMem         = ${params.DNS_MEM}
dnsCores       = ${params.DNS_CORES}

k8sproxyMem   = ${params.k8sproxy_Mem}
k8sproxyCores = ${params.k8sproxy_Cores}
k8sProxyIP     = ${params.K8S_PROXY_IP}

k8storageMem  = ${params.k8storage_Mem}
k8storageCores= ${params.k8storage_Cores}
k8sStorageIP   = ${params.K8S_STORAGE_IP}

haproxyMem    = ${params.Haproxy_Mem}
haproxyCores  = ${params.Haproxy_Cores}
proxyIP       = ${params.PROXY_IP}

tempMem        = ${params.Temp_Mem}
tempCores      = ${params.Temp_Cores}
"""
                    writeFile file: 'terraform.tfvars', text: tfvarsContent
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

        stage('Check VM Availability') {
            steps {
                script {
                    def vmIPs = []

                    // DB VMs
                    for (int i = 0; i < params.DB_COUNT.toInteger(); i++) {
                        vmIPs.add("${params.SUBNET}.${params.DB_START_IP.toInteger() + i}")
                    }

                    // K8s master nodes
                    for (int i = 0; i < params.MASTER_COUNT.toInteger(); i++) {
                        vmIPs.add("${params.SUBNET}.${params.K8S_START_IP.toInteger() + i}")
                    }

                    // K8s worker nodes
                    for (int i = 0; i < params.WORKER_COUNT.toInteger(); i++) {
                        vmIPs.add("${params.SUBNET}.${params.K8S_START_IP.toInteger() + params.MASTER_COUNT.toInteger() + i}")
                    }

                    // DNS nodes
                    for (int i = 0; i < params.DNS_COUNT.toInteger(); i++) {
                        vmIPs.add("${params.SUBNET}.${params.DNS_START_IP.toInteger() + i}")
                    }

                    // Proxy, Templates, Backup
                    vmIPs.add("${params.SUBNET}.${params.PROXY_IP.toInteger()}")
                    vmIPs.add("${params.SUBNET}.${params.TEMPLATES_SRV_IP.toInteger()}")
                    vmIPs.add("${params.SUBNET}.${params.BACKUP_SRV_IP.toInteger()}")

                    // Ping check
                    vmIPs.each { ip ->
                        def pingResult = sh(script: "ping -c 4 ${ip}", returnStatus: true)
                        if (pingResult != 0) {
                            error "❌ VM with IP ${ip} is not reachable!"
                        } else {
                            echo "✅ VM ${ip} is reachable."
                        }
                    }
                }
            }
        }
    }
}
