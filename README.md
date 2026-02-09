# jsm
JSM is a made-up assembly-style language that emulates the behavior of a RISC. There are 5 core functions and around a dozen 'microcode' functions built out of those for ease of use. It writes a 'compiled' (no comments) version of the test script into "jsm_no_comments.txt" each time it runs.

Newlines are significant, indentation is not.

How to run:
java JSM.java test.jsm

test.jsm is probably the fastest way to see how the language works.

// Examples, not a runnable test script in full, see test.jsm for that

myVar: 6 // variable declaration

:myLabel // this is a label/jump point

:myFunction // same as a label, put after the "end" command to not be executed unless called

// and # both work for comments

ast a 17 // asserts that a == 17, printing error message if not

// Printing:
print a // prints contents of register a
print myVar // prints value of myVar
print "this will print with quotes"
print this will print with spaces
print memory // prints contents of "RAM", currently a 512 length integer array
print |a // prints the letter a

// Core functions: mov, add, nand, cmp, jmpif
mov a 5 // register a now holds 5
mov b a // b now also holds 5
mov [a] 2 // memory[5] now holds value of 2
mov a 12
mov b [a] // treats contents of a as a memory address, so b = memory[12]
add a b // adds a to b, stores result in a
nand b a // bitwise nands b and a, stores result in b
cmp a b - sets the flags register by comparing a and b, currently the only flags are ==, >, and <
jmpif - short for 'jump if this bit of the flags register is set to 1', so:	
	cmp a b
	jmpif label 1
	// jump if a == b, aka the rightmost bits of the flags register are 001
	
	cmp a b
	jmpif label 2
	// jump if a < b
	
	cmp a b
	jmpif label 4
	// jump if a > b
:label

// Microcode functions
ldr a b // equivalent to mov a b, in case you want to have separate functions for loading to registers and memory for clarity. does not check that a is a register.
store [a] b // alias for mov [a] b, see above
jmp label2 // unconditional jump
jne label2 a b // jumps if a != b
je label2 a b // jumps if a == b

push a // store contents of a to the memory address that the stack pointer points to and decrements the stack pointer (the stack is at the end of the memory)
pop b // store whatever the stack pointer is pointing to in b and increments the stack pointer

call func1 // jumps to the label set with :func1, automatically storing return point
ret // returns to call line from function, must be used with call unless you manually set the rc register beforehand

bloop 5 // code between bloop and eloop will loop 5 times
eloop // ends loop

not a // bitwise not of a, result stored in a
sub a b // subtracts b from a, stores result in a
inc a // increment a by 1
dec a // decrement a by 1

end // ends program, put functions after this to avoid infinite loops

:label2
