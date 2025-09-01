# Required variables
variable "pxTargetNode" { type = string }

variable "clone" { type = string }

variable "prefix" { type = string }
variable "dbCount" { type = number }

variable "subnet" { type = string }
variable "storage" { type = string }
variable "diskSize" { type = number }
variable "dbMem" { type = number }
variable "dbCores" { type = number }

variable "dbStartIP" { type = string }
variable "cidr" { type = number }
variable "gateway" { type = string }
variable "tag" { type = number }

variable "ciuser" { type = string }
variable "sshkeys" { type = string }
variable "clusterName" { type = string }

# Default variables
variable "osType" {
  type    = string
  default = "cloud-init"
}

variable "agent" {
  type    = number
  default = 1
}

variable "scsihw" {
  type    = string
  default = "virtio-scsi-pci"
}
variable "apikey" { type = string }
