package com.sms.nexus.common.util;

import com.sms.nexus.dto.response.SystemLoadVO;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.io.File;

public class SystemInfoUtil {

    public static SystemLoadVO getSystemLoad() {
        SystemLoadVO vo = new SystemLoadVO();

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        vo.setTotalMemory(totalMemory);
        vo.setUsedMemory(usedMemory);
        vo.setFreeMemory(freeMemory);
        vo.setMemoryUsage(maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0);

        // Disk info
        File root = new File("/");
        long diskTotal = root.getTotalSpace();
        long diskFree = root.getUsableSpace();
        long diskUsed = diskTotal - diskFree;
        vo.setDiskTotal(diskTotal);
        vo.setDiskUsed(diskUsed);
        vo.setDiskFree(diskFree);
        vo.setDiskUsage(diskTotal > 0 ? (double) diskUsed / diskTotal * 100 : 0);

        // CPU - best effort
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            vo.setCpuUsage(sunOsBean.getCpuLoad() * 100);
        } else {
            vo.setCpuUsage(osBean.getSystemLoadAverage());
        }

        return vo;
    }

    public static long getUptime() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        return runtimeBean.getUptime();
    }

    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }

    public static String getOsName() {
        return System.getProperty("os.name");
    }

    public static String getOsArch() {
        return System.getProperty("os.arch");
    }
}
