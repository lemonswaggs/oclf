/data/oclf/busybox mkdir /dbdata/ext2data;
/data/oclf/busybox mkdir /dbdata/rfsdata;
mount -t rfs -o nosuid,nodev,check=no,noatime,nodiratime /dev/block/mmcblk0p2 /dbdata/rfsdata
/data/oclf/busybox mknod /dev/loop0 b 7 0;
/data/oclf/busybox losetup /dev/loop0 /dbdata/rfsdata/ext2/linux.ex2;
/data/oclf/e2fsck -p /dev/loop0;
mount -t ext2 -o noatime,nodiratime,errors=continue /dev/loop0 /dbdata/ext2data &&
/data/oclf/busybox rm -rf /data/data &&
/data/oclf/busybox rm -rf /data/system &&
/data/oclf/busybox rm -rf /data/dalvik-cache &&
/data/oclf/busybox rm -rf /data/app &&
/data/oclf/busybox rm -rf /data/app-private &&
/data/oclf/busybox mkdir /data/data &&
/data/oclf/busybox mkdir /data/system &&
/data/oclf/busybox mkdir /data/dalvik-cache &&
/data/oclf/busybox mkdir /data/app &&
/data/oclf/busybox mkdir /data/app-private &&
mount -o bind /dbdata/ext2data/data /data &&
mount -o bind /dbdata/rfsdata/gps /data/gps &&
mount -o bind /dbdata/rfsdata/misc /data/misc &&
mount -o bind /dbdata/rfsdata/wifi /data/wifi &&
mount -o bind /dbdata/rfsdata/dontpanic /data/dontpanic &&
mount -o bind /dbdata/rfsdata/local /data/local &&
/data/oclf/busybox rm -rf /dbdata/rfsdata/data &&
/data/oclf/busybox rm -rf /dbdata/rfsdata/system &&
/data/oclf/busybox rm -rf /dbdata/rfsdata/dalvik-cache &&
/data/oclf/busybox rm -rf /dbdata/rfsdata/app &&
/data/oclf/busybox rm -rf /dbdata/rfsdata/app-private &&
echo "stump" > /dbdata/rfsdata/data &&
echo "stump" > /dbdata/rfsdata/system &&
echo "stump" > /dbdata/rfsdata/dalvik-cache &&
echo "stump" > /dbdata/rfsdata/app &&
echo "stump" > /dbdata/rfsdata/app-private;
sync;
