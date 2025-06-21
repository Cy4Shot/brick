#define _CRT_SECURE_NO_WARNINGS
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <windows.h>
#include <io.h>

char* find_executable(const char* exe_name) {
    char* path_env = getenv("PATH");
    if (!path_env) {
        return NULL;
    }
    
    char* path_copy = malloc(strlen(path_env) + 1);
    strcpy(path_copy, path_env);
    
    char* token = strtok(path_copy, ";");
    static char full_path[MAX_PATH];
    
    while (token != NULL) {
        // Try with .exe extension
        snprintf(full_path, sizeof(full_path), "%s\\%s.exe", token, exe_name);
        if (_access(full_path, 0) == 0) {
            free(path_copy);
            return full_path;
        }
        
        // Try without extension
        snprintf(full_path, sizeof(full_path), "%s\\%s", token, exe_name);
        if (_access(full_path, 0) == 0) {
            free(path_copy);
            return full_path;
        }
        
        token = strtok(NULL, ";");
    }
    
    free(path_copy);
    return NULL;
}

#else
#include <unistd.h>
#include <sys/stat.h>
#include <limits.h>

char* find_executable(const char* exe_name) {
    char* path_env = getenv("PATH");
    if (!path_env) {
        return NULL;
    }
    
    char* path_copy = malloc(strlen(path_env) + 1);
    strcpy(path_copy, path_env);
    
    char* token = strtok(path_copy, ":");
    static char full_path[PATH_MAX];
    struct stat st;
    
    while (token != NULL) {
        snprintf(full_path, sizeof(full_path), "%s/%s", token, exe_name);
        
        // Check if file exists and is executable
        if (stat(full_path, &st) == 0 && (st.st_mode & S_IXUSR)) {
            free(path_copy);
            return full_path;
        }
        
        token = strtok(NULL, ":");
    }
    
    free(path_copy);
    return NULL;
}

#endif

char* which_executable(const char* exe_name) {
    return find_executable(exe_name);
}