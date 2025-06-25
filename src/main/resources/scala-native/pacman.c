#define _CRT_SECURE_NO_WARNINGS
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <windows.h>
#include <io.h>
#define PATH_SEPARATOR ";"
#define ACCESS_CHECK(path) (_access(path, 0) == 0)
#else
#include <unistd.h>
#include <sys/stat.h>
#define PATH_SEPARATOR ":"
#define ACCESS_CHECK(path) (access(path, X_OK) == 0)
#endif

// Windows package managers in order of preference
static const char* windows_pkgmgrs[] = {
    "scoop", "choco", "winget", NULL
};

// macOS package managers in order of preference
static const char* macos_pkgmgrs[] = {
    "brew", "port", "fink", NULL
};

// Linux package managers in order of preference
static const char* linux_pkgmgrs[] = {
    "apt", "apt-get", "yum", "dnf", "pacman", "zypper", "emerge", "xbps-install", "apk", NULL
};

char* find_executable_in_path(const char* exe_name) {
    char* path_env = getenv("PATH");
    if (!path_env) {
        return NULL;
    }
    
    char* path_copy = malloc(strlen(path_env) + 1);
    strcpy(path_copy, path_env);
    
    char* token = strtok(path_copy, PATH_SEPARATOR);
    static char full_path[4096];
    
    while (token != NULL) {
#ifdef _WIN32
        // Try with .exe extension
        snprintf(full_path, sizeof(full_path), "%s\\%s.exe", token, exe_name);
        if (ACCESS_CHECK(full_path)) {
            free(path_copy);
            return _strdup(exe_name);
        }
        
        // Try without extension
        snprintf(full_path, sizeof(full_path), "%s\\%s", token, exe_name);
        if (ACCESS_CHECK(full_path)) {
            free(path_copy);
            return _strdup(exe_name);
        }
#else
        snprintf(full_path, sizeof(full_path), "%s/%s", token, exe_name);
        if (ACCESS_CHECK(full_path)) {
            free(path_copy);
            return strdup(exe_name);
        }
#endif
        token = strtok(NULL, PATH_SEPARATOR);
    }
    
    free(path_copy);
    return NULL;
}

char* detect_package_manager() {
    const char** pkgmgrs = NULL;
    
#ifdef _WIN32
    pkgmgrs = windows_pkgmgrs;
#elif defined(__APPLE__)
    pkgmgrs = macos_pkgmgrs;
#else
    pkgmgrs = linux_pkgmgrs;
#endif
    
    // Check each package manager in order of preference
    for (int i = 0; pkgmgrs[i] != NULL; i++) {
        char* found = find_executable_in_path(pkgmgrs[i]);
        if (found) {
            return found;
        }
    }
    
    return NULL;
}

char* get_package_manager() {
    return detect_package_manager();
}
