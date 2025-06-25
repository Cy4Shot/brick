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

char* find_executable_in_path(const char* exe_name) {
    char* path_env = getenv("PATH");
    if (!path_env) {
        return NULL;
    }
    
    char* path_copy = (char*)malloc(strlen(path_env) + 1);
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