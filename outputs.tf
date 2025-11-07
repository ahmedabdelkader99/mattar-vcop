output "k8s_master_ips" {
  value = module.kubernetes.masters
}

output "k8s_workers_ips" {
  value = module.kubernetes.workers
}

output "k8s_proxy_ip" {
  value = module.kubernetes.k8sproxy
}

output "k8s_storage_ip" {
  value = module.kubernetes.k8storage
}

output "database" {
  value = module.database.databaseIPs
}

output "dns" {
  value = module.dns.databaseIPs
}

output "haproxy_ip" {
  value = module.haproxy.srv_ip
}

/*output "template_srv_ip" {
  value = module.templateSrv.srv_ip
}*/
