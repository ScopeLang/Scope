format ELF64 executable 3
use64

; Help ;
;
; Return    : rax
; Arguments : rdi, rsi, rdx, rcx, r8, r9, r10, r11
;
; VList      : | ENDPTR (8) | ELEM (16)... |
; VList elem : | SIZE (4) | PADDING (4) | PTR (8) |
;
; Settings ;
;
PACKAGE_SIZE = 8192
VLIST_SIZE = 8 + 16 * 1024 ; end + size * len

; Macros ;
;
struc db [data] {
common
	. db data
	.size = $ - .
}

macro vlist_getsize o, index { mov o, DWORD [(vlist + 8) + (16 * index + 0)] }
macro vlist_getptr  o, index { mov o, QWORD [(vlist + 8) + (16 * index + 8)] }

; R/W Data ;
;
segment readable writable

vlist rb VLIST_SIZE
curpkg rb 8

; Code ;
;
segment readable executable
entry f_main

init:
	lea rax, [vlist + 8]
	mov QWORD [vlist], rax
	call package_create
	mov QWORD [curpkg], rax
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

; args: rdi from, rsi to, rdx size
copy:
	push rsi
	push rdi
	push rdx
	add rdx, rdi
	inc rdx
	.l:
		mov al, BYTE [rdi]
		mov BYTE [rsi], al
		inc rdi
		inc rsi
		cmp rdi, rdx
		jl .l
	pop rdx
	pop rdi
	pop rsi
	ret

; args: rdi ptr0, rsi size0, rdx ptr1, rcx size1
; ret: rdi ptr, rsi size
concat:
	push rdx
	mov rdx, rsi
	mov rsi, QWORD [curpkg]
	call copy
	add rsi, rdx
	pop rdi
	mov rdx, rcx
	call copy
	mov rdi, QWORD [curpkg]
	sub rsi, rdi
	add rsi, rdx
	add QWORD [curpkg], rsi
	ret

; ret: rax ptr
package_create:
	xor rdi, rdi
	mov rsi, PACKAGE_SIZE ; Map size
	mov rdx, 0x02 ; R/W
	mov r10, 0x22 ; Private map + Anonymous
	mov r8, -1 ; No file descriptor
	xor r9, r9 ; No offset
	mov rax, 9 ; sys_mmap
	syscall
	ret

; args: rdi ptr
package_delete:
	mov rsi, PACKAGE_SIZE ; Map size
	mov rax, 11 ; sys_munmap
	syscall
	ret

; args: rdi ptr, rsi size
; ret: rax index
vlist_append:
	mov rax, QWORD [vlist] ; endptr
	mov [rax], rsi
	mov [rax + 8], rdi
	lea rsi, [vlist]
	sub rax, rsi
	shr rax, 4 ; Divide by 16
	add QWORD [vlist], 16
	ret