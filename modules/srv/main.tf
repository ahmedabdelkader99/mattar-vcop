terraform {
  required_providers {
    proxmox = {
      source  = "telmate/proxmox"
      version = "3.0.2-rc03"
    }
  }
}



resource "proxmox_vm_qemu" "srv" {
    name = "${var.srv_name}"
    tags = "VPSie_VCOP_${var.srv_name}"
    target_node = "${var.px_target_node}"
    clone = "${var.clone}"
    agent = var.srv_agent
    os_type = "${var.os_type}"
    memory = var.srv_memory
    scsihw = "${var.scsihw}"

    cpu {
        cores = var.srv_cores
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
                        size            = var.srv_dsize
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
        tag = var.srv_tag
    }

    # Setup the ip address using cloud-init.
    boot = "order=virtio0"
    # Keep in mind to use the CIDR notation for the ip.
    ipconfig0 = "ip=${var.srv_subnet}.${var.srv_ip}/${var.srv_cidr},gw=${var.srv_gw}"
    ciuser = var.ciuser
    sshkeys = var.sshkeys
}