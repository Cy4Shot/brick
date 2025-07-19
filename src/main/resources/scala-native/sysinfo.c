#include <string.h>
#include <stdlib.h>
#include <stdio.h>

#ifdef _WIN32

#include <windows.h>
#include <sysinfoapi.h>

static char cached_os[64];
static char cached_kernel[128];
static char cached_arch[64];
static char cached_node[128];

const char* sys_os() {
    strcpy_s(cached_os, sizeof(cached_os), "Windows");
    return cached_os;
}

const char* sys_kernel() {
    OSVERSIONINFOEX info;
    ZeroMemory(&info, sizeof(OSVERSIONINFOEX));
    info.dwOSVersionInfoSize = sizeof(OSVERSIONINFOEX);
#pragma warning(push)
#pragma warning(disable:4996) // GetVersionEx is deprecated, but okay for basic use
    GetVersionEx((LPOSVERSIONINFO)&info);
#pragma warning(pop)
    snprintf(cached_kernel, sizeof(cached_kernel), "%lu.%lu.%lu",
             info.dwMajorVersion, info.dwMinorVersion, info.dwBuildNumber);
    return cached_kernel;
}

const char* sys_kernel_version() {
    return sys_kernel(); // No extra info on Windows
}

const char* sys_arch() {
    SYSTEM_INFO sysInfo;
    GetNativeSystemInfo(&sysInfo);
    switch (sysInfo.wProcessorArchitecture) {
        case PROCESSOR_ARCHITECTURE_AMD64:
            strcpy_s(cached_arch, sizeof(cached_arch), "x86_64");
            break;
        case PROCESSOR_ARCHITECTURE_INTEL:
            strcpy_s(cached_arch, sizeof(cached_arch), "x86");
            break;
        case PROCESSOR_ARCHITECTURE_ARM64:
            strcpy_s(cached_arch, sizeof(cached_arch), "arm64");
            break;
        default:
            strcpy_s(cached_arch, sizeof(cached_arch), "unknown");
    }
    return cached_arch;
}

const char* sys_node() {
    DWORD size = sizeof(cached_node);
    GetComputerNameExA(ComputerNamePhysicalDnsHostname, cached_node, &size);
    return cached_node;
}

#else // POSIX (Linux/macOS)

#include <sys/utsname.h>
#include <stdio.h>

static struct utsname cached_uname;
static int uname_loaded = 0;

static void load_uname_once() {
    if (!uname_loaded) {
        if (uname(&cached_uname) == 0) {
            uname_loaded = 1;
        }
    }
}

const char* sys_os() {
    load_uname_once();
    return cached_uname.sysname;
}

const char* sys_kernel() {
    load_uname_once();
    return cached_uname.release;
}

const char* sys_kernel_version() {
    load_uname_once();
    return cached_uname.version;
}

const char* sys_arch() {
    load_uname_once();
    return cached_uname.machine;
}

const char* sys_node() {
    load_uname_once();
    return cached_uname.nodename;
}

#endif
