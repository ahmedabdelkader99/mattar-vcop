output "databaseIPs" {
    value = [for instance in proxmox_vm_qemu.db : instance.default_ipv4_address ]
}