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

; Move a DWORD (b) into a 64 bit register (a)
macro movqd a, b {
	if a in <rax>
		mov eax, b
	else if a in <rdi>
		mov edi, b
	else if a in <rsi>
		mov esi, b
	else if a in <rdx>
		mov edx, b
	else if a in <rcx>
		mov ecx, b
	else 
		display "Invalid register for `movqd`"
	end if
}

macro vlist_getptr o, index {
	mov o, index
	imul o, 16
	add o, 8
	add o, QWORD [vlist]
	mov o, QWORD [o]
}
macro vlist_getsize o, index { 
	mov o, index
	imul o, 16
	add o, QWORD [vlist]
	movqd o, DWORD [o]
}

macro push [i] { push i }
macro pop  [i] { pop  i }

; R/W Data ;
;
segment readable writable

; The actual data of the vlist
vlist_start rb VLIST_SIZE
; The reference frame of the vlist
vlist rb 8
; The end of the vlist
vlist_end rb 8

curpkg rb 8

; Code ;
;
segment readable executable
entry f_main

init:
	lea rax, [vlist_start]
	mov QWORD [vlist_end], rax
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
	push rdx, rsi, rdi
	mov rdx, rsi ; Count
	mov rsi, rdi ; Buffer
	mov rdi, 1   ; stdout
	mov rax, 1   ; sys_write
	syscall
	pop rdi, rsi, rdx
	ret

; args: rdi from, rsi to, rdx size
copy:
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
	ret

; args: rdi ptr0, rsi size0, rdx ptr1, rcx size1
; ret: rdi ptr, rsi size
concat:
	push rdx
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
	pop rdx
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

; args: rdi ptr, rsi size
; ret: rax index
vlist_append:
	push rsi
	mov rax, QWORD [vlist_end]
	mov [rax], rsi
	mov [rax + 8], rdi
	mov rsi, QWORD [vlist]
	sub rax, rsi
	shr rax, 4 ; Divide by 16
	add QWORD [vlist_end], 16
	pop rsi
	ret