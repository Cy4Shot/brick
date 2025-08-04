#define _CRT_SECURE_NO_WARNINGS
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "utils.h"

#ifdef _WIN32
#include <windows.h>
#include <io.h>
#define PATH_SEPARATOR ";"
#define ACCESS_CHECK(path) (_access(path, 0) == 0)
#define NULL_DEVICE "NUL"
#else
#include <unistd.h>
#include <sys/stat.h>
#define PATH_SEPARATOR ":"
#define ACCESS_CHECK(path) (access(path, X_OK) == 0)
#define NULL_DEVICE "/dev/null"
#endif

// Common module systems in order of preference
static const char* module_systems[] = {
    "module",      // Generic module command (Environment Modules, Lmod)
    NULL
};

// Common module environment variables to check
static const char* module_env_vars[] = {
    "MODULEPATH",
    "LMOD_DIR",
    "LMOD_ROOT",
    "MODULESHOME",
    "MODULE_VERSION",
    NULL
};

int check_module_env_vars() {
    for (int i = 0; module_env_vars[i] != NULL; i++) {
        if (getenv(module_env_vars[i]) != NULL) {
            return 1;
        }
    }
    return 0;
}

char* detect_module_system() {
    for (int i = 0; module_systems[i] != NULL; i++) {
        char* found = find_executable_in_path(module_systems[i]);
        if (found) {
            char command[1024];
            snprintf(command, sizeof(command), "%s --version 2>" NULL_DEVICE " > " NULL_DEVICE, found);
            
            int result = system(command);
            if (result == 0) {
                return found;
            } else {
                free(found);
            }
        }
    }
    
    return NULL;
}

int is_module_available(const char* module_name) {
    char* module_cmd = detect_module_system();
    if (!module_cmd) {
        return 0;
    }
    
    char command[1024];

    const char* check_commands[] = {
        "%s avail %s 2>" NULL_DEVICE " | grep -q %s",
        "%s spider %s 2>" NULL_DEVICE " > " NULL_DEVICE,
        "%s show %s 2>" NULL_DEVICE " > " NULL_DEVICE,
        NULL
    };
    
    for (int i = 0; check_commands[i] != NULL; i++) {
        snprintf(command, sizeof(command), check_commands[i], module_cmd, module_name, module_name);
        
        int result = system(command);
        if (result == 0) {
            free(module_cmd);
            return 1;
        }
    }
    
    free(module_cmd);
    return 0;
}

char* get_module_system() {
    return detect_module_system();
}

int has_module_system() {
    char* module_cmd = detect_module_system();
    if (!module_cmd) {
        return 0;
    }
    char command[1024];
    snprintf(command, sizeof(command), "%s --version 2>" NULL_DEVICE " > " NULL_DEVICE, module_cmd);
    int result = system(command);
    free(module_cmd);
    return (result == 0) ? 1 : 0;
}

char* get_modulepath() {
    return getenv("MODULEPATH");
}

int check_module_available(const char* module_name) {
    return is_module_available(module_name);
}
