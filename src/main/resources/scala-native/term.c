#include <stdio.h>

#ifdef _WIN32
#include <windows.h>
#include <io.h>

int get_terminal_size(int* rows, int* cols) {
    CONSOLE_SCREEN_BUFFER_INFO csbi;
    HANDLE hConsole = GetStdHandle(STD_OUTPUT_HANDLE);
    
    if (hConsole == INVALID_HANDLE_VALUE) {
        return -1;
    }
    
    if (!GetConsoleScreenBufferInfo(hConsole, &csbi)) {
        return -1;
    }
    
    *cols = csbi.srWindow.Right - csbi.srWindow.Left + 1;
    *rows = csbi.srWindow.Bottom - csbi.srWindow.Top + 1;
    return 0;
}

#else
#include <sys/ioctl.h>
#include <unistd.h>

int get_terminal_size(int* rows, int* cols) {
    struct winsize w;
    
    if (ioctl(STDOUT_FILENO, TIOCGWINSZ, &w) == -1) {
        return -1;
    }
    
    *rows = w.ws_row;
    *cols = w.ws_col;
    return 0;
}

#endif

int get_terminal_width() {
    int rows, cols;
    if (get_terminal_size(&rows, &cols) == 0) {
        return cols;
    }
    return 80; // Default fallback
}

int get_terminal_height() {
    int rows, cols;
    if (get_terminal_size(&rows, &cols) == 0) {
        return rows;
    }
    return 24; // Default fallback
}