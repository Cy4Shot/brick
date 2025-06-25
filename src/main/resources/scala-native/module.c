#define _CRT_SECURE_NO_WARNINGS
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

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

int check_module_env_vars() {
    for (int i = 0; module_env_vars[i] != NULL; i++) {
        if (getenv(module_env_vars[i]) != NULL) {
            return 1; // Found module environment variable
        }
    }
    return 0;
}

char* detect_module_system() {
    // First check environment variables
    if (check_module_env_vars()) {
        // If environment variables are set, check for executables
        for (int i = 0; module_systems[i] != NULL; i++) {
            char* found = find_executable_in_path(module_systems[i]);
            if (found) {
                return found;
            }
        }
        // If env vars exist but no executable found, return generic "module"
        #ifdef _WIN32
        return _strdup("module");
        #else
        return strdup("module");
        #endif
    }
    
    // Check for executables even without environment variables
    for (int i = 0; module_systems[i] != NULL; i++) {
        char* found = find_executable_in_path(module_systems[i]);
        if (found) {
            return found;
        }
    }
    
    return NULL;
}

int is_module_available(const char* module_name) {
    char* module_cmd = detect_module_system();
    if (!module_cmd) {
        return 0; // No module system found
    }
    
    char command[1024];
    
    // Try different module availability check commands
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
            return 1; // Module is available
        }
    }
    
    free(module_cmd);
    return 0; // Module not found or error occurred
}

char* get_module_system() {
    return detect_module_system();
}

int has_module_system() {
    char* module_cmd = detect_module_system();
    if (module_cmd) {
        free(module_cmd);
        return 1;
    }
    return 0;
}

char* get_modulepath() {
    return getenv("MODULEPATH");
}

int check_module_available(const char* module_name) {
    return is_module_available(module_name);
}
