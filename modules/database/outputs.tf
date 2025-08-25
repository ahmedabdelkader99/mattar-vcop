output "databaseIPs" {
    value = [for instance in proxmox_vm_qemu.database : instance.default_ipv4_address ]
}