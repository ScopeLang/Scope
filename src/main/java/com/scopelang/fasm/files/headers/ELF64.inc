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
PACKAGE_SIZE = 50 * 1024 * 1024 ; 50 Mb
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
	push rcx, rdx, rsi, rdi
	mov rdx, QWORD [rdi] ; Count
	lea rsi, [rdi + 16]  ; Buffer
	mov rdi, 1           ; stdout
	mov rax, 1           ; sys_write
	syscall
	pop rdi, rsi, rdx, rcx
	ret

; ret: rdi ptr
input:
	push rcx, rdx, rsi
	mov rsi, QWORD [curpkg]
	add rsi, 16
	mov rax, 0
	.l: ; Read until new line
		push rax
		mov rdx, 1 ; Count
		mov rdi, 0 ; stdin
		mov rax, 0 ; sys_read
		syscall
		pop rax
		inc rax ; Increment counter
		inc rsi ; Increment ptr
		cmp BYTE [rsi - 1], 0x0A
		jne .l
	mov BYTE [rsi - 1], 0 ; Get rid of the new line
	dec rax
	mov rdi, QWORD [curpkg]
	mov QWORD [rdi], rax
	add QWORD [curpkg], 16
	add QWORD [curpkg], rax
	pop rsi, rdx, rcx
	ret

; args: rdi seconds, rsi nanoseconds
sleep:
	push rsi, rdi
	; Add data to request buffer
	mov rax, QWORD [curpkg]
	mov QWORD [rax], rdi
	mov QWORD [rax + 8], rsi
	; Run syscall
	mov rsi, 0              ; Remainder buffer (NULL)
	mov rdi, QWORD [curpkg] ; Request buffer
	mov rax, 35             ; sys_nanosleep
	syscall
	; Clear request buffer
	mov rax, QWORD [curpkg]
	mov QWORD [rax], 0
	mov QWORD [rax + 8], 0
	; End
	pop rdi, rsi
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

; args: rdi ptr0, rsi ptr1
; ret: rdi boolean
streq:
	push rcx
	mov rcx, QWORD [rdi]
	cmp rcx, QWORD [rsi]
	jne .e
	mov rax, QWORD [rdi]
	add rdi, 16
	add rsi, 16
	.l:
		mov cl, BYTE [rdi]
		cmp cl, BYTE [rsi]
		jne .e
		inc rdi
		inc rsi
		dec rax
		cmp rax, 1
		jg .l
		mov rdi, 1
		pop rcx
		ret
	.e:
	mov rdi, 0
	pop rcx
	ret

; ret: rax ptr
package_create:
	push rcx, rdi, rsi, rdx, r10, r8, r9
	xor rdi, rdi
	mov rsi, PACKAGE_SIZE ; Map size
	mov rdx, 0x02 ; R/W
	mov r10, 0x22 ; Private map + Anonymous
	mov r8, -1 ; No file descriptor
	xor r9, r9 ; No offset
	mov rax, 9 ; sys_mmap
	syscall
	pop r9, r8, r10, rdx, rsi, rdi, rcx
	ret

; args: rdi ptr
package_delete:
	push rcx, rsi
	mov rsi, PACKAGE_SIZE ; Map size
	mov rax, 11 ; sys_munmap
	syscall
	pop rsi, rcx
	ret