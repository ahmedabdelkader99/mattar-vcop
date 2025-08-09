output "srv_ip" {
    value = proxmox_vm_qemu.srv.default_ipv4_address
}