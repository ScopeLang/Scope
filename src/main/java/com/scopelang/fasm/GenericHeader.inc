format ELF64 executable 3

; Help ;

; Return    : rax
; Arguments : rdi, rsi, rdx, rcx, r8, r9, r10, r11

; VList      : | ENDPTR (8) | ELEM (16)... |
; VList elem : | SIZE (4) | PADDING (4) | PTR (8) |

; Settings ;

PACKAGE_SIZE = 8192
VLIST_SIZE = 8 + 16 * 1024 ; end + size * len

; Macros ;

struc db [data] {
common
	. db data
	.size = $ - .
}

macro vlist_getsize o, index {
	mov o, DWORD [(vlist + 8) + (16 * index)]
}

macro vlist_getptr o, index {
	lea o, [(vlist + 8) + (16 * index + 8)] 
}

; R/W Data ;

segment readable writable

vlist rb VLIST_SIZE

; Code ;

segment readable executable
entry f_main

init:
	lea rax, [vlist + 8]
	mov QWORD [vlist], rax
	ret

; args: rdi code
exit:
	mov rax, 60 ; sys_exit
	syscall
	ret

; args: rdi ptr, rsi size
print:
	mov rdx, rsi ; Count
	mov rsi, rdi ; Buffer
	mov rdi, 1   ; stdout
	mov rax, 1   ; sys_write
	syscall
	ret

; ret: rax ptr
package_create:
	mov rsi, PACKAGE_SIZE ; Map size
	mov rdi, 2 ; R/W
	mov r10, 2 ; Private map
	mov r8, -1 ; No file descriptor
	xor r9, r9  ; No offset
	mov rax, 9 ; sys_mmap
	syscall
	ret

; args: rdi ptr
package_delete:
	mov rsi, PACKAGE_SIZE ; Map size
	mov rax, 11 ; sys_munmap
	syscall
	ret

; args: rdi size, rsi ptr
; ret: rax index
vlist_append:
	mov rax, QWORD [vlist] ; endptr
	mov [rax], rdi
	mov [rax + 8], rsi
	
	; Get index
	lea rdi, [vlist]
	sub rax, rdi
	shr rax, 4 ; Divide by 16
	
	add QWORD [vlist], 16
	ret
