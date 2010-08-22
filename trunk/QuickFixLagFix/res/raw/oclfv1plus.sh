/data/oclf/busybox mknod /dev/loop0 b 7 0;
/data/oclf/busybox losetup /dev/loop0 /data/linux.ex2;
/data/oclf/busybox mkdir /data/ext2data;
/data/oclf/e2fsck -p /dev/loop0;
mount -t ext2 -o noatime,nodiratime,errors=continue /dev/loop0 /data/ext2data;
/data/oclf/busybox rm -rf /data/data.bak
/data/oclf/busybox rm -rf /data/system.bak
/data/oclf/busybox rm -rf /data/dalvik-cache.bak
/data/oclf/busybox rm -rf /data/app.bak
/data/oclf/busybox rm -rf /data/app-private.bak
