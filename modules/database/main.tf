terraform {
  required_providers {
    proxmox = {
      source  = "telmate/proxmox"
      version = "3.0.2-rc03"
    }
  }
}



resource "proxmox_vm_qemu" "database" {
    count = var.dbCount
    name = "${var.prefix}-${var.clusterName}0${count.index + 1}"
    tags = "VPSie_VCOP_${var.prefix}_${var.clusterName}_0${count.index + 1}"
    target_node = "${var.pxTargetNode}"
    clone = "${var.clone}"
    agent = var.agent
    os_type = "${var.osType}"
    memory = var.dbMem
    scsihw = "${var.scsihw}"

    cpu {
        cores = var.dbCores
        sockets = 1
        type = "x86-64-v2-AES"
    }

    disks {
            ide {
                ide0 {
                    cloudinit {
                        storage = "${var.storage}"
                    }
                }
            }
            virtio {
                virtio0 {
                    disk {
                        size            = var.diskSize
                        storage         = "${var.storage}"
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
    ipconfig0 = "ip=${var.subnet}.${var.dbStartIP + count.index}/${var.cidr},gw=${var.gateway}"
    ciuser = var.ciuser
    sshkeys = var.sshkeys
}