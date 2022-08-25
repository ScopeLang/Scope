format ELF64 executable 3

; Make all DBs have a size value
struc db [data] {
common
	. db data
	.size = $ - .
}

segment readable executable
entry main

; Exit procedure
exit:
	mov rdi, rax
	mov rax, 60
	syscall
	ret

; Print procedure (stdout)
print:
	mov rsi, rax
	mov rdi, 1
	mov rax, 1
	syscall
	ret

; Entry point
main: