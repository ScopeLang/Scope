; Generated at 2022/09/10 08:28:08 p.m.

;@FILE,ELF64,c87b3328bbdd10900689af7dbfa552e3,Test.scope
format ELF64 executable 3
use64

PACKAGE_SIZE = 8192
VLIST_SIZE = 8 + 16 * 1024 

struc db [data] {
common
	. db data
	.size = $ - .
}

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

segment readable writable

vlist_start rb VLIST_SIZE
vlist rb 8
vlist_end rb 8

curpkg rb 8

segment readable executable
entry f_main

init:
	lea rax, [vlist_start]
	mov QWORD [vlist_end], rax
	mov QWORD [vlist], rax
	call package_create
	mov QWORD [curpkg], rax
	ret

exit:
	mov rax, 60 
	syscall
	ret

print:
	push rdx, rsi, rdi
	mov rdx, rsi 
	mov rsi, rdi 
	mov rdi, 1   
	mov rax, 1   
	syscall
	pop rdi, rsi, rdx
	ret

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

package_create:
	push rdi, rsi, rdx, r10, r8, r9
	xor rdi, rdi
	mov rsi, PACKAGE_SIZE 
	mov rdx, 0x02 
	mov r10, 0x22 
	mov r8, -1 
	xor r9, r9 
	mov rax, 9 
	syscall
	pop r9, r8, r10, rdx, rsi, rdi
	ret

package_delete:
	push rsi
	mov rsi, PACKAGE_SIZE 
	mov rax, 11 
	syscall
	pop rsi
	ret

vlist_append:
	push rsi
	mov rax, QWORD [vlist_end]
	mov [rax], rsi
	mov [rax + 8], rdi
	mov rsi, QWORD [vlist]
	sub rax, rsi
	shr rax, 4 
	add QWORD [vlist_end], 16
	pop rsi
	ret

;@FUNC,1
f_main:
	call init
	mov rdx, QWORD [vlist_end]
	mov rcx, QWORD [vlist]
	push rdx
	push rcx
	mov rax, QWORD [vlist_end]
	sub rax, 0
	mov QWORD [vlist], rax
	lea rdi, [s_c87b3328bbdd10900689af7dbfa552e3_0]
	mov rsi, s_c87b3328bbdd10900689af7dbfa552e3_0.size
	call vlist_append
	push rax
	call f_print3
	pop rax
	mov QWORD [vlist], rax
	pop rax
	mov QWORD [vlist_end], rax
	mov rdi, 0
	call exit

;@FUNC,5
f_print3:
	mov rdx, QWORD [vlist_end]
	mov rcx, QWORD [vlist]
	pop rax
	int3
	vlist_getptr rdi, rax
	vlist_getsize rsi, rax
	

	call vlist_append
	push rdx
	push rcx
	mov rax, QWORD [vlist_end]
	sub rax, 16
	mov QWORD [vlist], rax
	
	int3
	
	vlist_getptr rdi, 0
	vlist_getsize rsi, 0
	call print
	pop rax
	mov QWORD [vlist], rax
	pop rax
	mov QWORD [vlist_end], rax
	ret

segment readable

;@STR,14
s_c87b3328bbdd10900689af7dbfa552e3_0 db 72, 101, 108, 108, 111, 44, 32, 87, 111, 114, 108, 100, 33, 10

