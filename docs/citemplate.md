# How to Create a Cloudinit template

- Clone one of VPSie templates

> you can use the public cloudimages as well

- Add Cloudinit drive to it: id0

- Start the VM.

- Install Cloudinit on it.

```sh
apt install cloud-init -y
systemctl enable cloud-init
```

- Reboot the VM and watch for Cloudinit configuration at booting and login screen.

```sh
reboot
```

- Fix grub-pc issue.

```sh
dpkg --configure -a
# Select OK >> No >> Select the main disk instead of dm-o
```

![alt text](../img/image.png)

> dm-o is the root mapper for LVM.

- Remove any network settings if exists

- shutdown the VM and convert it to Template.
