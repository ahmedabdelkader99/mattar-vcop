# Required variables
variable "px_target_node" { type = string }

variable "srv_name" { type = string }
variable "srv_desc" { type = string }
variable "clone" { type = string }

variable "storage" { type = string }
variable "srv_dsize" { type = number }
variable "srv_memory" { type = number }
variable "srv_cores" { type = number }

variable "srv_ip" { type = string }
variable "srv_cidr" { type = number }
variable "srv_gw" { type = string }
variable "srv_tag" { type = number }

variable "ciuser" { type = string }
variable "sshkeys" { type = string }


# Default variables
variable "os_type" { 
    type = string 
    default = "cloud-init"
    }

variable "srv_agent" { 
    type = number 
    default = 1
    }

variable "scsihw" { 
    type = string 
    default = "virtio-scsi-pci"
    }
