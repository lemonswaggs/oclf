/data/oclf/busybox mknod /dev/loop0 b 7 1;
/data/oclf/busybox losetup /dev/loop0 /data/linux.ex2;
/data/oclf/busybox mkdir -p /data/ext2data;
/data/oclf/e2fsck -p /dev/loop0;
mount -t ext2 -o noatime,nodiratime,errors=continue /dev/loop0 /data/ext2data;
/data/oclf/busybox mount -o bind /data/ext2data/data /data/data
/data/oclf/busybox mount -o bind /data/ext2data/system /data/system
/data/oclf/busybox mount -o bind /data/ext2data/dalvik-cache /data/dalvik-cache
/data/oclf/busybox mount -o bind /data/ext2data/app /data/app
