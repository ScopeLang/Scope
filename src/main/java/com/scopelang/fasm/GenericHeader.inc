format ELF64 executable 3

struc db [data] {
common
	. db data
	.size = $ - .
}

macro movref name, p, s {
	lea p, [name]
	mov s, name#.size
}

segment readable executable
entry f_main

exit:
	mov rdi, rax
	mov rax, 60
	syscall
	ret

print:
	mov rsi, rax
	mov rdi, 1
	mov rax, 1
	syscall
	ret
