## Proxmox variables
variable "px_endpoint" { type = string }
variable "px_user" { type = string }
variable "px_password" { type = string }
variable "pxTargetNode" { type = string }
variable "px_tls" { type = bool }

## Clone variables
variable "clone" { type = string }
variable "defaultStorage" { type = string }
variable "agent" { type = number }
variable "osType" { type = string }
variable "scsihw" { type = string }
variable "prefix" { type = string }
variable "diskSize" { type = number }

## Network configuration
variable "subnet" { type = string }
variable "gateway" { type = string }
variable "cidr" { type = number }
variable "tag" { type = number }
variable "sshkeys" { type = string }
variable "ciuser" { type = string }
variable "k8sStartIP" { type = number }
variable "k8sProxyIP" { type = number }
variable "k8sStorageIP" { type = number }
variable "dbStartIP" { type = number }
variable "dnsStartIP" { type = number }
variable "proxyIP" { type = number }
variable "templatesSrvIP" { type = number }
variable "backupSrvIP" { type = number }

## Kubernetes cluster configuration
variable "masterCount" { type = number }
variable "masterMem" { type = number }
variable "masterCores" { type = number }
# variable "masterDSize" { type = number }

variable "workersCount" { type = number }
variable "workerMem" { type = number }
variable "workerCores" { type = number }
# variable "workerDSize" { type = number }

variable "k8sproxyMem" { type = number }
variable "k8sproxyCores" { type = number }


variable "k8storageMem" { type = number }
variable "k8storageCores" { type = number }
