terraform {
  required_providers {
    proxmox = {
      source  = "telmate/proxmox"
      version = "3.0.2-rc03"
    }
  }
}



resource "proxmox_vm_qemu" "master" {
    count = var.masterCount
    name = "${var.prefix}-master0${count.index + 1}"
    tags = "VPSie_VCOP_${var.prefix}_kubernetes_master_node_0${count.index + 1}"
    target_node = "${var.pxTargetNode}"
    clone = "${var.clone}"
    agent = var.agent
    os_type = "${var.osType}"
    memory = var.masterMem
    scsihw = "${var.scsihw}"

    cpu {
        cores = var.masterCores
        sockets = 1
        type = "x86-64-v2-AES"
    }

    disks {
            ide {
                ide0 {
                    cloudinit {
                        storage = "${var.defaultStorage}"
                    }
                }
            }
            virtio {
                virtio0 {
                    disk {
                        size            = var.diskSize
                        storage         = "${var.defaultStorage}"
                        iothread        = true
                        discard         = true
                        format          = "qcow2"
                    }
                }
            }
        }

    # Setup the network interface and assign a vlan tag: 256
    network {
        id = 0
        model = "virtio"
        bridge = "vmbr10"
        tag = var.tag
    }

    # Setup the ip address using cloud-init.
    boot = "order=virtio0"
    # Keep in mind to use the CIDR notation for the ip.
    ipconfig0 = "ip=${var.subnet}.${var.k8sStartIP + count.index}/${var.cidr},gw=${var.gateway}"
    ciuser = var.ciuser
    sshkeys = var.sshkeys
}

resource "proxmox_vm_qemu" "worker" {
    count = var.workersCount
    name = "${var.prefix}-worker0${count.index + 1}"
    tags = "VPSie_VCOP_${var.prefix}_kubernetes_worker_node_0${count.index + 1}"
    target_node = "${var.pxTargetNode}"
    clone = "${var.clone}"
    agent = var.agent
    os_type = "${var.osType}"
    memory = var.workerMem
    scsihw = "${var.scsihw}"

    cpu {
        cores = var.workerCores
        sockets = 1
        type = "x86-64-v2-AES"
    }

    disks {
            ide {
                ide0 {
                    cloudinit {
                        storage = "${var.defaultStorage}"
                    }
                }
            }
            virtio {
                virtio0 {
                    disk {
                        size            = var.diskSize
                        storage         = "${var.defaultStorage}"
                        iothread        = true
                        discard         = true
                        format          = "qcow2"
                    }
                }
            }
        }

    # Setup the network interface and assign a vlan tag: 256
    network {
        id = 0
        model = "virtio"
        bridge = "vmbr10"
        tag = var.tag
    }

    # Setup the ip address using cloud-init.
    boot = "order=virtio0"
    # Keep in mind to use the CIDR notation for the ip. - if doesn't work add masterCount insteadf of worker count
    ipconfig0 = "ip=${var.subnet}.${var.k8sStartIP + count.index + var.workersCount}/${var.cidr},gw=${var.gateway}"
    ciuser = var.ciuser
    sshkeys = var.sshkeys
}

resource "proxmox_vm_qemu" "k8sproxy" {
    name = "${var.prefix}-k8sproxy"
    tags = "VPSie_VCOP_${var.prefix}_kubernetes_proxy"
    target_node = "${var.pxTargetNode}"
    clone = "${var.clone}"
    agent = var.agent
    os_type = "${var.osType}"
    memory = var.k8sproxyMem
    scsihw = "${var.scsihw}"

    cpu {
        cores = var.k8sproxyCores
        sockets = 1
        type = "x86-64-v2-AES"
    }

    disks {
            ide {
                ide0 {
                    cloudinit {
                        storage = "${var.defaultStorage}"
                    }
                }
            }
            virtio {
                virtio0 {
                    disk {
                        size            = var.diskSize
                        storage         = "${var.defaultStorage}"
                        iothread        = true
                        discard         = true
                        format          = "qcow2"
                    }
                }
            }
        }

    # Setup the network interface and assign a vlan tag: 256
    network {
        id = 0
        model = "virtio"
        bridge = "vmbr10"
        tag = var.tag
    }

    # Setup the ip address using cloud-init.
    boot = "order=virtio0"
    # Keep in mind to use the CIDR notation for the ip
    ipconfig0 = "ip=${var.subnet}.${var.k8sProxyIP}/${var.cidr},gw=${var.gateway}"
    ciuser = var.ciuser
    sshkeys = var.sshkeys
}

resource "proxmox_vm_qemu" "k8storage" {
    name = "${var.prefix}-k8storage"
    tags = "VPSie_VCOP_${var.prefix}_kubernetes_storage"
    target_node = "${var.pxTargetNode}"
    clone = "${var.clone}"
    agent = var.agent
    os_type = "${var.osType}"
    memory = var.k8storageMem
    scsihw = "${var.scsihw}"

    cpu {
        cores = var.k8storageCores
        sockets = 1
        type = "x86-64-v2-AES"
    }

    disks {
            ide {
                ide0 {
                    cloudinit {
                        storage = "${var.defaultStorage}"
                    }
                }
            }
            virtio {
                virtio0 {
                    disk {
                        size            = var.diskSize
                        storage         = "${var.defaultStorage}"
                        iothread        = true
                        discard         = true
                        format          = "qcow2"
                    }
                }
            }
        }

    # Setup the network interface and assign a vlan tag: 256
    network {
        id = 0
        model = "virtio"
        bridge = "vmbr10"
        tag = var.tag
    }

    # Setup the ip address using cloud-init.
    boot = "order=virtio0"
    # Keep in mind to use the CIDR notation for the ip
    ipconfig0 = "ip=${var.subnet}.${var.k8sStorageIP}/${var.cidr},gw=${var.gateway}"
    ciuser = var.ciuser
    sshkeys = var.sshkeys
}