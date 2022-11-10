format ELF64 executable 3
use64

; Help ;
;
; args: rdi, rsi, (rdx, rcx, r8, r9, r10, r11)
; ptr format: SIZE (8) | TYPE (8) | DATA (SIZE)
;
; temp registers (no need for reset):
; rdi, rsi, rax, xmm0, xmm1
;
; Settings ;
;
PACKAGE_SIZE = 131072 ; 1 Mb
VLIST_SIZE = 8 * 1024 ; size * len

; Macros ;
;
macro vlist_get reg, index {
	mov reg, QWORD [vlist]
	mov reg, QWORD [reg + index * 8]
}

; args: rdi ptr
; ret: rax ptr_of_vlist
macro vlist_set index {
	mov rax, QWORD [vlist]
	lea rax, [rax + index * 8]
	mov [rax], rdi
}

macro fstart shift {
	push QWORD [vlist_end]
	push QWORD [vlist]
	mov rax, QWORD [vlist_end]
	mov QWORD [vlist], rax
	add QWORD [vlist_end], shift
}

macro freturn {
	pop rax
	mov QWORD [vlist], rax
	pop rax
	mov QWORD [vlist_end], rax
}

; Macros for pushing/popping multiple things at the same time
macro push [i] { push i }
macro pop  [i] { pop  i }

; R/W Data ;
;
segment readable writable

; The actual data of the vlist
vlist_start rb VLIST_SIZE
; The start of the current reference frame
vlist rb 8
; The end of the current reference frame
vlist_end rb 8

; Floating point temporary memory location
fptmp rb 8

; The pointer to the current package
curpkg rb 8

; Code ;
;
segment readable executable
entry f_main

init:
	lea rax, [vlist_start]
	mov QWORD [vlist], rax
	mov QWORD [vlist_end], rax
	call package_create
	mov QWORD [curpkg], rax
	ret

; args: rdi code
exit:
	mov rax, 60 ; sys_exit
	syscall
	ret

; args: rdi ptr
print:
	push rdx, rsi, rdi
	mov rdx, QWORD [rdi] ; Count
	lea rsi, [rdi + 16]   ; Buffer
	mov rdi, 1           ; stdout
	mov rax, 1           ; sys_write
	syscall
	pop rdi, rsi, rdx
	ret

; args: rdi from, rsi to, rdx amount
copy:
	cmp rdx, 0
	je .e
	push rsi, rdi, rdx
	add rdx, rdi
	inc rdx
	.l:
		mov al, BYTE [rdi]
		mov BYTE [rsi], al
		inc rdi
		inc rsi
		cmp rdi, rdx
		jl .l
	pop rdx, rdi, rsi
	.e:
	ret

; args: rdi ptr0, rsi ptr1
; ret: rdi new_ptr
concat:
	push rax, rsi, rdx
	; Get the new size
	mov rdx, QWORD [rdi]
	add rdx, QWORD [rsi]
	; Put it in the new_ptr
	mov rax, QWORD [curpkg]
	push rax
	mov QWORD [rax], rdx
	add rax, 8
	; Add type info
	lea rdx, [rdi + 8]
	mov rdx, QWORD [rdx]
	mov QWORD [rax], rdx
	add QWORD [curpkg], 16
	; Copy to new_ptr (first)
	mov rdx, QWORD [rdi]
	push rsi
	add rdi, 16
	mov rsi, QWORD [curpkg]
	call copy
	add QWORD [curpkg], rdx
	; Copy to new_ptr (second)
	pop rdi
	mov rdx, QWORD [rdi]
	add rdi, 16
	mov rsi, QWORD [curpkg]
	call copy
	add QWORD [curpkg], rdx
	; End
	pop rdi
	pop rdx, rsi, rax
	ret

; ret: rax ptr
package_create:
	push rdi, rsi, rdx, r10, r8, r9
	xor rdi, rdi
	mov rsi, PACKAGE_SIZE ; Map size
	mov rdx, 0x02 ; R/W
	mov r10, 0x22 ; Private map + Anonymous
	mov r8, -1 ; No file descriptor
	xor r9, r9 ; No offset
	mov rax, 9 ; sys_mmap
	syscall
	pop r9, r8, r10, rdx, rsi, rdi
	ret

; args: rdi ptr
package_delete:
	push rsi
	mov rsi, PACKAGE_SIZE ; Map size
	mov rax, 11 ; sys_munmap
	syscall
	pop rsi
	ret