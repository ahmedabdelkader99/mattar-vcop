output "masters" {
    value = [for instance in proxmox_vm_qemu.master : instance.default_ipv4_address ]
}

output "workers" {
    value = [for instance in proxmox_vm_qemu.worker : instance.default_ipv4_address ]
}

output "k8sproxy" {
    value = proxmox_vm_qemu.k8sproxy.default_ipv4_address
}

output "k8storage" {
    value = proxmox_vm_qemu.k8storage.default_ipv4_address
}