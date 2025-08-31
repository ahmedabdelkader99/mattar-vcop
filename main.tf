terraform {
  required_providers {
    proxmox = {
      source  = "telmate/proxmox"
      version = "3.0.2-rc03"
    }
  }
}

provider "proxmox" {

  pm_tls_insecure = var.px_tls
  pm_api_url      = var.px_endpoint
  pm_user         = var.px_user
  pm_password     = var.px_password
}



module "kubernetes" {
  source = "./modules/kubernetes"

  pxTargetNode   = var.pxTargetNode
  clone          = var.clone
  defaultStorage = var.defaultStorage
  diskSize       = var.diskSize
  agent          = var.agent
  osType         = var.osType
  scsihw         = var.scsihw
  prefix         = var.prefix

  subnet  = var.subnet
  gateway = var.gateway
  cidr    = var.cidr
  tag     = var.tag
  sshkeys = var.sshkeys
  ciuser  = var.ciuser

  k8sStartIP   = var.k8sStartIP
  k8sProxyIP   = var.k8sProxyIP
  k8sStorageIP = var.k8sStorageIP

  masterCount = var.masterCount
  masterMem   = var.masterMem
  masterCores = var.masterCores
  # masterDSize = var.masterDSize

  workersCount = var.workersCount
  workerMem    = var.workerMem
  workerCores  = var.workerCores
  # workerDSize = var.workerDSize

  k8sproxyMem   = var.k8sproxyMem
  k8sproxyCores = var.k8sproxyCores

  k8storageMem   = var.k8storageMem
  k8storageCores = var.k8storageCores
}

module "database" {
  source = "./modules/database"

  pxTargetNode = var.pxTargetNode
  clone        = var.clone
  prefix       = var.prefix

  clusterName = "k8s-database"
  storage     = var.defaultStorage
  diskSize    = var.diskSize
  dbMem       = var.dbMem
  dbCores     = var.dbCores

  dbStartIP = var.dbStartIP
  subnet    = var.subnet
  cidr      = var.cidr
  gateway   = var.gateway
  tag       = var.tag

  ciuser  = var.ciuser
  sshkeys = var.sshkeys

  dbCount = var.dbCount
}

module "dns" {
  source = "./modules/database"

  pxTargetNode = var.pxTargetNode
  clone        = var.clone
  prefix       = var.prefix

  clusterName = "dns"
  storage     = var.defaultStorage
  diskSize    = var.diskSize
  dbMem       = var.dnsMem
  dbCores     = var.dnsCores

  dbStartIP = var.dnsStartIP
  subnet    = var.subnet
  cidr      = var.cidr
  gateway   = var.gateway
  tag       = var.tag

  ciuser  = var.ciuser
  sshkeys = var.sshkeys

  dbCount = var.dnsCount
}

module "haproxy" {
  source = "./modules/srv"

  srv_name       = "${var.prefix}-haproxy"
  px_target_node = var.pxTargetNode
  clone          = var.clone

  storage    = var.defaultStorage
  srv_dsize  = var.diskSize
  srv_memory = var.haproxyMem
  srv_cores  = var.haproxyCores

  srv_subnet = var.subnet
  srv_ip     = var.proxyIP
  srv_cidr   = var.cidr
  srv_gw     = var.gateway
  srv_tag    = var.tag

  ciuser  = var.ciuser
  sshkeys = var.sshkeys

}

module "templateSrv" {
  source = "./modules/srv"

  srv_name       = "${var.prefix}-templateSrv"
  px_target_node = var.pxTargetNode
  clone          = var.clone

  storage    = var.defaultStorage
  srv_dsize  = var.diskSize
  srv_memory = var.tempMem
  srv_cores  = var.tempCores

  srv_subnet = var.subnet
  srv_ip     = var.templatesSrvIP
  srv_cidr   = var.cidr
  srv_gw     = var.gateway
  srv_tag    = var.tag

  ciuser  = var.ciuser
  sshkeys = var.sshkeys

}

