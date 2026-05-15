format elf64 executable 3

SYS_EXIT  = 60
SYS_WRITE = 1
STDOUT    = 1

segment readable executable
entry $
    mov rdi, STDOUT
    lea rsi, [msg]
    mov rdx, msg.len
    mov rax, SYS_WRITE
    syscall

    xor rdi, rdi
    mov rax, SYS_EXIT
    syscall

segment readable writable
    msg db 'Hello, World', 10
    msg.len = $ - msg
