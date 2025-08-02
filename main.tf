terraform {
  required_providers {
    proxmox = {
      source  = "telmate/proxmox"
      version = "3.0.2-rc03"
    }
  }
}


############# Varables #############
# Proxmox variables
variable "px_endpoint" { type = string }
variable "px_user" { type = string }
variable "px_password" { type = string }
variable "px_target_node" { type = string }
variable "px_tls" { type = bool }

# Cluster variables
variable "cluster_name" { type = string }
variable "cluster_desc" { type = string }
variable "cluster_clone" { type = string }
variable "cluster_agent" { type = number }
variable "cluster_os_type" { type = string }
variable "cluster_scsihw" { type = string }
variable "cluster_storage" { type = string }

variable "cluster_ssh" { type = string }
variable "cluster_subnet" { type = string }
variable "cluster_gw" { type = string }
variable "cluster_start_ip" { type = number }
variable "cluster_cidr" { type = number }
variable "cluster_tag" { type = number }

variable "cluster_master_count" { type = number }
variable "cluster_master_memory" { type = number }
variable "cluster_master_cores" { type = number }
variable "cluster_master_dsize" { type = number }

variable "cluster_worker_count" { type = number }
variable "cluster_worker_memory" { type = number }
variable "cluster_worker_cores" { type = number }
variable "cluster_worker_dsize" { type = number }
####################################

provider "proxmox" {

      pm_tls_insecure = var.px_tls
      pm_api_url = "${var.px_endpoint}"
      pm_user = "${var.px_user}"
      pm_password = "${var.px_password}"
}

module "srv" {
    source = "./modules/srv"
    
    srv_name = "haproxy"
    srv_desc = "This is a test server"
    px_target_node = var.px_target_node
    clone = var.cluster_clone

    storage = var.cluster_storage
    srv_dsize = var.cluster_master_dsize
    srv_memory = var.cluster_master_memory
    srv_cores = var.cluster_master_cores

    srv_ip = "11.1.1.99"
    srv_cidr = 24
    srv_gw = var.cluster_gw
    srv_tag = var.cluster_tag

    ciuser = "m4tt4r"
    sshkeys = var.cluster_ssh

}