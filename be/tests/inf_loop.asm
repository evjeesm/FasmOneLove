format elf64 executable 3

SYS_EXIT  = 60
SYS_WRITE = 1
STDOUT    = 1

segment readable executable
entry foo

foo:
    call print
    jmp foo

print:
    mov rdi, STDOUT
    lea rsi, [msg]
    mov rdx, msg.len
    mov rax, SYS_WRITE
    syscall
    ret

segment readable writable
    msg db 'loop', 10
    msg.len = $ - msg

