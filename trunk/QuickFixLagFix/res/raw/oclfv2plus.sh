/data/oclf/busybox mkdir /dbdata/ext2data;
/data/oclf/busybox mkdir /dbdata/rfsdata;
mount -t rfs -o nosuid,nodev,check=no,noatime,nodiratime /dev/block/mmcblk0p2 /dbdata/rfsdata
/data/oclf/busybox mknod /dev/loop0 b 7 0;
/data/oclf/busybox losetup /dev/loop0 /dbdata/rfsdata/ext2/linux.ex2;
/data/oclf/e2fsck -p /dev/loop0;
mount -t ext2 -o noatime,nodiratime,errors=continue /dev/loop0 /dbdata/ext2data;
mount -o bind /dbdata/ext2data/data /data
mount -o bind /dbdata/rfsdata/gps /data/gps
mount -o bind /dbdata/rfsdata/misc /data/misc
mount -o bind /dbdata/rfsdata/wifi /data/wifi
mount -o bind /dbdata/rfsdata/dontpanic /data/dontpanic
mount -o bind /dbdata/rfsdata/local /data/local

