/data/oclf/busybox mknod /dev/loop0 b 7 0;
/data/oclf/busybox losetup /dev/loop0 /data/linux.ex2;
/data/oclf/busybox mkdir /data/ext2data;
mount -t ext2 -o noatime,nodiratime,errors=continue /dev/loop0 /data/ext2data;
